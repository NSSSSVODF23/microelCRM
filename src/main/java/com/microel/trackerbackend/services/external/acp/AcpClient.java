package com.microel.trackerbackend.services.external.acp;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.microel.tdo.dynamictable.TablePaging;
import com.microel.trackerbackend.controllers.configuration.Configuration;
import com.microel.trackerbackend.controllers.configuration.entity.AcpConf;
import com.microel.trackerbackend.modules.exceptions.Unconfigured;
import com.microel.trackerbackend.parsers.commutator.AbstractRemoteAccess;
import com.microel.trackerbackend.parsers.commutator.ra.RAFactory;
import com.microel.trackerbackend.services.api.ResponseException;
import com.microel.trackerbackend.services.api.StompController;
import com.microel.trackerbackend.services.external.RestPage;
import com.microel.trackerbackend.services.external.acp.types.*;
import com.microel.trackerbackend.storage.dispatchers.*;
import com.microel.trackerbackend.storage.entities.acp.AcpHouse;
import com.microel.trackerbackend.storage.entities.acp.NetworkConnectionLocation;
import com.microel.trackerbackend.storage.entities.acp.commutator.*;
import com.microel.trackerbackend.storage.entities.address.Address;
import com.microel.trackerbackend.storage.entities.address.House;
import com.microel.trackerbackend.storage.exceptions.IllegalFields;
import lombok.*;
import org.hibernate.Hibernate;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Lazy;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.http.RequestEntity;
import org.springframework.lang.Nullable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import javax.validation.constraints.NotNull;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

@Service
public class AcpClient {
    private final RestTemplate restTemplate = new RestTemplateBuilder().build();
    private final HouseDispatcher houseDispatcher;
    private final AcpCommutatorDispatcher acpCommutatorDispatcher;
    private final PortInfoDispatcher portInfoDispatcher;
    private final FdbItemDispatcher fdbItemDispatcher;
    private final NetworkConnectionLocationDispatcher networkConnectionLocationDispatcher;
    private final Set<RemoteUpdatingCommutatorItem> commutatorPoolInTheProcessOfUpdating = ConcurrentHashMap.newKeySet();
    private final RAFactory remoteAccessFactory;
    private final CommutatorsAvailabilityCheckService availabilityCheckService;
    private final StompController stompController;
    private final Configuration configurationService;
    private AcpConf configuration;

    public AcpClient(Configuration configurationService, HouseDispatcher houseDispatcher,
                     AcpCommutatorDispatcher acpCommutatorDispatcher, PortInfoDispatcher portInfoDispatcher,
                     FdbItemDispatcher fdbItemDispatcher, NetworkConnectionLocationDispatcher networkConnectionLocationDispatcher,
                     RAFactory remoteAccessFactory, @Lazy CommutatorsAvailabilityCheckService availabilityCheckService,
                     StompController stompController) {
        this.configurationService = configurationService;
        this.configuration = configurationService.loadOrDefault(AcpConf.class, new AcpConf());
        this.houseDispatcher = houseDispatcher;
        this.acpCommutatorDispatcher = acpCommutatorDispatcher;
        this.portInfoDispatcher = portInfoDispatcher;
        this.fdbItemDispatcher = fdbItemDispatcher;
        this.networkConnectionLocationDispatcher = networkConnectionLocationDispatcher;
        this.remoteAccessFactory = remoteAccessFactory;
        this.availabilityCheckService = availabilityCheckService;
        this.stompController = stompController;
    }

    public LogsRequest getLogsByLogin(String login, Integer page) {
        return get(LogsRequest.class, Map.of(), "dhcp", "binding", login, "logs", page.toString());
    }

    @Transactional(readOnly = true)
    public List<DhcpBinding> getBindingsByLogin(String login) {
        DhcpBinding[] dhcpBindings = get(DhcpBinding[].class, Map.of("login", login), "dhcp", "bindings");
        if (dhcpBindings == null) return Collections.emptyList();
        List<DhcpBinding> bindings = Arrays.stream(dhcpBindings).sorted(Comparator.comparing(DhcpBinding::getSessionTime).reversed()).collect(Collectors.toList());
        bindings.forEach(this::prepareBinding);
        return bindings;
    }

    public Page<DhcpBinding> getBindingsByLogin(String login, Integer page, @Nullable BindingFilter filter) {
        RequestEntity.BodyBuilder request = RequestEntity.post(url(Map.of(), "user", login, "bindings", page.toString()));
        RestPage<DhcpBinding> responseBody;
        if(filter != null) {
            responseBody = restTemplate.exchange(request.body(filter), new ParameterizedTypeReference<RestPage<DhcpBinding>>() {
            }).getBody();
        }else{
            responseBody = restTemplate.exchange(request.build(), new ParameterizedTypeReference<RestPage<DhcpBinding>>() {
            }).getBody();
        }
        if(responseBody == null) return Page.empty();
        responseBody.forEach(this::prepareBinding);
        return responseBody;
    }

    /**
     * Добавляет информацию к сессии абонента о последнем месте включения в коммутатор
     * @param binding
     * @return
     */
    public DhcpBinding prepareBinding(DhcpBinding binding){
        NetworkConnectionLocation location = networkConnectionLocationDispatcher.getByBindingId(binding.getId());
        if (location != null && location.getPortId() != null) {
            AcpCommutator foundCommutator = acpCommutatorDispatcher.getById(location.getCommutatorId());
            if(foundCommutator != null && foundCommutator.getSystemInfo() != null && foundCommutator.getPorts() != null)
                location.setCommutatorInfo(foundCommutator.getSystemInfo().getLastUpdate(), foundCommutator.getPorts(), binding.getMacaddr());
        }
        binding.setLastConnectionLocation(location);
        return binding;
    }

    @Transactional(readOnly = true)
    public RestPage<DhcpBinding> getLastBindings(Integer page, @Nullable Short state, @Nullable String macaddr,
                                                 @Nullable String login, @Nullable String ip, @Nullable Integer vlan,
                                                 @Nullable Integer buildingId, @Nullable List<Integer> targetIds) {
        Map<String, String> query = new HashMap<>();
        if (state != null) query.put("state", state.toString());
        if (macaddr != null) query.put("macaddr", macaddr);
        if (login != null) query.put("login", login);
        if (ip != null) query.put("ip", ip);
        if (vlan != null) query.put("vlan", vlan.toString());
        if (buildingId != null) query.put("buildingId", buildingId.toString());
        try {
            if (targetIds != null) query.put("targetIds", new ObjectMapper().writeValueAsString(targetIds));
        }catch (JsonProcessingException e){
            throw new ResponseException("Ошибка при попытке конвертирования списка идентификаторов целевых сессий");
        }
        RequestEntity<Void> request = RequestEntity.get(url(query, "dhcp", "bindings", page.toString(), "last")).build();
        RestPage<DhcpBinding> pageResponse = restTemplate.exchange(request, new ParameterizedTypeReference<RestPage<DhcpBinding>>() {
        }).getBody();
        if (pageResponse == null) return null;
        pageResponse.forEach(this::prepareBinding);
        return pageResponse;
    }

    @Transactional(readOnly = true)
    public RestPage<DhcpBinding> getLastBindings(Integer page, @Nullable Short state, @Nullable String macaddr,
                                                 @Nullable String login, @Nullable String ip, @Nullable Integer vlan,
                                                 @Nullable Integer buildingId, Integer commutator, @Nullable Integer port) {

        List<NetworkConnectionLocation> lastByCommutator = networkConnectionLocationDispatcher.getLastByCommutator(commutator, port);

        return getLastBindings(
                page,
                state,
                macaddr,
                login,
                ip,
                vlan,
                buildingId,
                lastByCommutator.stream().map(NetworkConnectionLocation::getDhcpBindingId).collect(Collectors.toList())
        );
    }

    public RestPage<DhcpBinding> getDhcpBindingsByVlan(Integer page, Integer id, String excludeLogin) {
        RequestEntity<Void> request = RequestEntity.get(url(Map.of("excludeLogin", excludeLogin), "vlan", id.toString(), "dhcp", "bindings", page.toString())).build();
        RestPage<DhcpBinding> pageResponse = restTemplate.exchange(request, new ParameterizedTypeReference<RestPage<DhcpBinding>>() {
        }).getBody();
        if (pageResponse == null) return pageResponse;
        pageResponse.forEach(binding -> {
            NetworkConnectionLocation location = networkConnectionLocationDispatcher.getByBindingId(binding.getId());
            if (location != null && location.getPortId() != null) {
                AcpCommutator foundCommutator = acpCommutatorDispatcher.getById(location.getCommutatorId());
                if (foundCommutator != null) {
                    location.setLastPortCheck(foundCommutator.getSystemInfo().getLastUpdate());
                    if (location.getPortId() != null) {
                        foundCommutator.getPorts().stream().filter(port -> port.getPortInfoId().equals(location.getPortId())).findFirst().ifPresent(portInfo -> {
                            location.setIsHasLink(Objects.equals(portInfo.getStatus(), PortInfo.Status.UP));
                            location.setPortSpeed(portInfo.getSpeed());
                        });
                    }
                }
            }
            binding.setLastConnectionLocation(location);
        });
        return pageResponse;
    }

    @Nullable
    public DhcpBinding getDhcpBindingByMac(String macaddr) {
        return get(DhcpBinding.class, Map.of("macaddr", macaddr), "dhcp", "binding");
    }

    @Nullable
    public DhcpBinding getDhcpBindingByMacAndVlan(String macaddr, Integer vid) {
        return get(DhcpBinding.class, Map.of("macaddr", macaddr, "vid", vid.toString()), "dhcp", "binding", "mac-and-vlan");
    }

    public List<AcpHouse> getHouses(@Nullable String query) {
        AcpHouse[] acpHouses = get(AcpHouse[].class, Map.of("query", query == null ? "" : query), "buildings");
        if (acpHouses == null) return new ArrayList<>();
        return Arrays.stream(acpHouses).collect(Collectors.toList());
    }

    public List<SwitchModel> getCommutatorModels(@Nullable String query) {
        SwitchModel[] switchModels = get(SwitchModel[].class, Map.of("query", query == null ? "" : query), "commutator", "models");
        if (switchModels == null) return new ArrayList<>();
        return Arrays.stream(switchModels).collect(Collectors.toList());
    }

    @Nullable
    public SwitchModel getCommutatorModel(Integer id) {
        return get(SwitchModel.class, Map.of(), "commutator", "model", id.toString());
    }

    @Transactional(readOnly = true)
    public Page<SwitchBaseInfo> getCommutators(Integer page, @Nullable String name, @Nullable String ip, @Nullable Integer buildingId, @Nullable Integer pageSize) {
        Map<String, String> query = new HashMap<>();
        if (name != null) query.put("name", name);
        if (ip != null) query.put("ip", ip);
        if (buildingId != null) query.put("buildingId", buildingId.toString());
        if (pageSize != null) query.put("pageSize", pageSize.toString());
        RequestEntity<Void> request = RequestEntity.get(url(query, "commutators", page.toString())).build();
        RestPage<Switch> responseBody = restTemplate.exchange(request, new ParameterizedTypeReference<RestPage<Switch>>() {
        }).getBody();
        if(responseBody == null) throw new ResponseException("Ошибка при обращении к ACP");
        Map<Integer, String> commutatorModelsNames = getCommutatorModels(null).stream().collect(Collectors.toMap(SwitchModel::getId, SwitchModel::getName));
        return responseBody.map(commutator -> {
            AcpCommutator additionalInfo = acpCommutatorDispatcher.getById(commutator.getId());
            commutator.setAdditionalInfo(additionalInfo);
            return SwitchBaseInfo.from(commutator, commutatorModelsNames.get(commutator.getSwmodelId().intValue()));

//            if (commutator.getBuildId() == null) return commutator;
//            House house = houseDispatcher.getByAcpBindId(commutator.getBuildId());
//            if (additionalInfo != null) commutator.setAdditionalInfo(additionalInfo);
//            if (house != null) commutator.setAddress(house.getAddress());
        });
    }

    public Page<SwitchBaseInfo> getCommutatorsTable(TablePaging paging) {
        RequestEntity.BodyBuilder request = RequestEntity.post(url(Map.of(), "commutators", "table"));
        RestPage<Switch> responseBody = restTemplate.exchange(request.body(paging), new ParameterizedTypeReference<RestPage<Switch>>() {
        }).getBody();
        if(responseBody == null) throw new ResponseException("Ошибка при обращении к ACP");
        Map<Integer, String> commutatorModelsNames = getCommutatorModels(null).stream().collect(Collectors.toMap(SwitchModel::getId, SwitchModel::getName));
        Set<Integer> externalHouseIds = new HashSet<>();
        responseBody.forEach(sw->{
            externalHouseIds.add(sw.getBuildId());
        });
        Map<Integer, House> houses = houseDispatcher.getByExternalHouseIds(externalHouseIds);
        return responseBody.map(commutator -> {
            AcpCommutator additionalInfo = acpCommutatorDispatcher.getById(commutator.getId());
            commutator.setAdditionalInfo(additionalInfo);
            SwitchBaseInfo switchBaseInfo = SwitchBaseInfo.from(commutator, commutatorModelsNames.get(commutator.getSwmodelId().intValue()));
            if(commutator.getBuildId() != null) {
                House house = houses.get(commutator.getBuildId());
                switchBaseInfo.setAddress(house != null ? house.getAddressName() : "");
            }
            return switchBaseInfo;
        });
    }

    public Page<Switch> getCommutatorsWithAdditionalInfo(Integer page, Integer pageSize) {
        Map<String, String> query = new HashMap<>();
        if (pageSize != null) query.put("pageSize", pageSize.toString());
        RequestEntity<Void> request = RequestEntity.get(url(query, "commutators", page.toString())).build();
        RestPage<Switch> responseBody = restTemplate.exchange(request, new ParameterizedTypeReference<RestPage<Switch>>() {}).getBody();
        if(responseBody == null) throw new ResponseException("Ошибка при обращении к ACP");
        Map<Integer, String> commutatorModelsNames = getCommutatorModels(null).stream().collect(Collectors.toMap(SwitchModel::getId, SwitchModel::getName));
        return responseBody.map(commutator -> {
            AcpCommutator additionalInfo = acpCommutatorDispatcher.getById(commutator.getId());
            commutator.setAdditionalInfo(additionalInfo);
            return commutator;
        });
    }

    public List<Switch> getCommutatorsByVlan(Integer vlan) {
        Map<String, String> query = new HashMap<>();
        query.put("vlan", vlan.toString());
        RequestEntity<Void> request = RequestEntity.get(url(query, "commutators", "by-vlan")).build();
        return Objects.requireNonNull(restTemplate.exchange(request, new ParameterizedTypeReference<List<Switch>>() {
        }).getBody()).stream().map(commutator -> {
            if (commutator.getBuildId() == null) return commutator;
            House house = houseDispatcher.getByAcpBindId(commutator.getBuildId());
            AcpCommutator additionalInfo = acpCommutatorDispatcher.getById(commutator.getId());
            if (additionalInfo != null) commutator.setAdditionalInfo(additionalInfo);
            if (house != null) commutator.setAddress(house.getAddress());
            return commutator;
        }).collect(Collectors.toList());
    }

    public List<Switch> getAllCommutators() {
        Switch[] switches = get(Switch[].class, Map.of(), "commutators", "all");
        if (switches == null) return new ArrayList<>();
        return Arrays.stream(switches).peek(sw->{
            AcpCommutator additionalInfo = acpCommutatorDispatcher.getById(sw.getId());
            if (additionalInfo != null) {
                sw.setAdditionalInfo(additionalInfo);
            }
        }).collect(Collectors.toList());
    }

    public List<SwitchWithAddress> searchCommutators(@Nullable String stringQuery) {
        Map<String, String> query = new HashMap<>();
        if (stringQuery != null) query.put("query", stringQuery);
        SwitchWithAddress[] switchWithAddresses = get(SwitchWithAddress[].class, query, "commutators", "search");
        if (switchWithAddresses == null) return new ArrayList<>();
        return Arrays.stream(switchWithAddresses).collect(Collectors.toList());
    }

    @Nullable
    private <T> T get(Class<T> clazz, Map<String, String> query, String... params) {
        try {
            T object = this.restTemplate.getForObject(url(query, params), clazz);
            if (object == null) return null;
            return object;
        } catch (RestClientException e) {
            throw new ResponseException("Ошибка при обращении к ACP");
        }
    }

    @Nullable
    private <T> T post(Class<T> clazz, Map<String, String> query, Object request, String... params) {
        try {
            T object = this.restTemplate.postForObject(url(query, params), request, clazz);
            if (object == null) return null;
            return object;
        } catch (RestClientException e) {
            throw new ResponseException("Ошибка при обращении к ACP");
        }
    }

    private String url(Map<String, String> query, String... params) {
        checkConfiguration();
        UriComponentsBuilder uriBuilder = UriComponentsBuilder.fromHttpUrl(configuration.getAcpFlexConnectorEndpoint() + String.join("/", params));
        query.forEach(uriBuilder::queryParam);
        return uriBuilder.build().toUriString();
    }

    private void checkConfiguration() {
        if (!configuration.isFilled()) {
            throw new Unconfigured("Отсутствует конфигурация интеграции с ACP");
        }
    }

    public AcpConf getConfiguration() {
        return configuration;
    }

    public void setConfiguration(AcpConf conf) {
        if (!conf.isFilled()) throw new IllegalFields("Конфигурация не заполнена");
        configuration = conf;
        configurationService.save(configuration);
        stompController.changeAcpConfig(configuration);
    }

    public void authDhcpBinding(DhcpBinding.AuthForm form) {
        try {
            this.restTemplate.postForObject(url(Map.of(), "dhcp", "binding", "auth"), form, Void.class);
        } catch (Throwable e) {
            throw new ResponseException(e.getMessage());
        }
    }

    public Boolean checkCommutatorNameExist(String name) {
        return get(Boolean.class, Map.of("name", name), "commutator", "check-exist", "name");
    }

    public Boolean checkCommutatorIpExist(String ip) {
        return get(Boolean.class, Map.of("ip", ip), "commutator", "check-exist", "ip");
    }

    @Nullable
    public SwitchWithAddress getCommutator(Integer id) {
        SwitchWithAddress switchWithAddress = get(SwitchWithAddress.class, Map.of(), "commutator", id.toString());
        if (switchWithAddress == null) return null;
        House house = null;
        if (switchWithAddress.getCommutator().getBuildId() != null)
            house = houseDispatcher.getByAcpBindId(switchWithAddress.getCommutator().getBuildId());

        AcpCommutator additionalInfo = acpCommutatorDispatcher.getById(switchWithAddress.getCommutator().getId());
        if (additionalInfo != null) switchWithAddress.getCommutator().setAdditionalInfo(additionalInfo);
        if (house != null) switchWithAddress.getCommutator().setAddress(house.getAddress());
        return switchWithAddress;
    }

    public SwitchEditingPreset getCommutatorEditingPreset(Integer id) {
        SwitchWithAddress target = getCommutator(id);
        if (target == null) throw new ResponseException("Не найден коммутатор");
        Map<Integer, SwitchModel> commutatorModelsNames = getCommutatorModels(null).stream().collect(Collectors.toMap(SwitchModel::getId, s->s));
        House house = houseDispatcher.getByAcpBindId(target.getCommutator().getBuildId());
        Address address = null;
        if(house != null) address = house.getAddress();
        return SwitchEditingPreset.from(target.getCommutator(), address, commutatorModelsNames.get(target.getCommutator().getSwmodelId().intValue()), getCommutator(target.getCommutator().getPhyUplinkId()));
    }

    public SwitchBaseInfo getCommutatorBaseInfo(Integer id) {
        return get(SwitchBaseInfo.class, Map.of(), "commutator", id.toString(), "base-info");
    }

    public List<SwitchBaseInfo> getCommutatorsBaseInfo() {
        return Arrays.stream(Objects.requireNonNull(get(SwitchBaseInfo[].class, Map.of(), "commutators", "base-info"))).collect(Collectors.toList());
    }

    public Switch createCommutator(Switch.Form form) {
        try {
            Switch commutator = this.restTemplate.postForObject(url(Map.of(), "commutator"), form, Switch.class);
            availabilityCheckService.synchronizeBetweenBases();
            if (commutator == null) return null;
            House house = houseDispatcher.getByAcpBindId(commutator.getBuildId());
            AcpCommutator additionalInfo = acpCommutatorDispatcher.getById(commutator.getId());
            if (additionalInfo != null) commutator.setAdditionalInfo(additionalInfo);
            if (house != null) commutator.setAddress(house.getAddress());
            stompController.createCommutator(commutator);
            SwitchModel commutatorModel = getCommutatorModel(commutator.getSwmodelId().intValue());
            String modelName = commutatorModel != null ? commutatorModel.getName() : "";
            stompController.createBaseCommutator(SwitchBaseInfo.from(commutator, modelName));
            return commutator;
        } catch (Throwable e) {
            throw new ResponseException(e.getMessage());
        }
    }

    @Nullable
    public Switch updateCommutator(Integer id, Switch.Form form) {
        try {
            Switch commutator = this.restTemplate.patchForObject(url(Map.of(), "commutator", id.toString()), form, Switch.class);
            availabilityCheckService.synchronizeBetweenBases();
            if (commutator == null) return null;
            House house = houseDispatcher.getByAcpBindId(commutator.getBuildId());
            AcpCommutator additionalInfo = acpCommutatorDispatcher.getById(commutator.getId());
            if (additionalInfo != null) commutator.setAdditionalInfo(additionalInfo);
            if (house != null) commutator.setAddress(house.getAddress());
            stompController.updateCommutator(commutator);
            SwitchModel commutatorModel = getCommutatorModel(commutator.getSwmodelId().intValue());
            String modelName = commutatorModel != null ? commutatorModel.getName() : "";
            stompController.createBaseCommutator(SwitchBaseInfo.from(commutator, modelName));
            return commutator;
        } catch (Throwable e) {
            throw new ResponseException(e.getMessage());
        }
    }

    public void deleteCommutator(Integer id) {
        try {
            this.restTemplate.delete(url(Map.of(), "commutator", id.toString()));
            availabilityCheckService.synchronizeBetweenBases();
            stompController.deleteCommutator(id);
            stompController.deleteBaseCommutator(id);
        } catch (Throwable e) {
            throw new ResponseException(e.getMessage());
        }
    }

    public void getCommutatorRemoteUpdate(Integer id) {
        SwitchWithAddress commutator = getCommutator(id);
        if (commutator == null) throw new ResponseException("Не найден коммутатор в ACP с id: " + id);
        Map<Integer, String> commutatorModels = getCommutatorModels(null).stream().collect(Collectors.toMap(SwitchModel::getId, SwitchModel::getName));
        Hibernate.initialize(commutator.getCommutator().getAdditionalInfo().getPorts());
        Hibernate.initialize(commutator.getCommutator().getAdditionalInfo().getRemoteUpdateLogs());
        appendCommutatorInUpdatePool(commutator.getCommutator());
        try {
            connectToCommutatorAndUpdate(commutator.getCommutator(), commutatorModels);
        }finally {
            removeCommutatorFromUpdatePool(commutator.getCommutator());
        }
    }

    public void getCommutatorsByVlanRemoteUpdate(Integer vlan) {
        List<Switch> commutatorsByVlan = getCommutatorsByVlan(vlan);
        remoteUpdateCommutators(commutatorsByVlan, 60L);
    }

    @Transactional(readOnly = true)
    @Async
    @Scheduled(cron = "0 0 */3 * * *")
    public void getAllCommutatorsRemoteUpdate() {
        List<Switch> commutators = getAllCommutators();
        commutators.forEach(commutator -> {
            if (commutator.getAdditionalInfo() == null) return;
            Hibernate.initialize(commutator.getAdditionalInfo().getPorts());
            Hibernate.initialize(commutator.getAdditionalInfo().getRemoteUpdateLogs());
        });
        remoteUpdateCommutators(commutators, 7200L);
    }

    public void remoteUpdateCommutators(List<Switch> commutators, Long timeout){
        Map<Integer, String> commutatorModels = getCommutatorModels(null).stream().collect(Collectors.toMap(SwitchModel::getId, SwitchModel::getName));
        ExecutorService executorService = Executors.newFixedThreadPool(3);
        for (Switch commutator : commutators) {
            if(commutator.getAdditionalInfo() == null) continue;
            executorService.execute(() -> {
                try {
                    Thread.currentThread().setName("REMOTE UPDATE");
                    appendCommutatorInUpdatePool(commutator);
                    connectToCommutatorAndUpdate(commutator, commutatorModels);
                }catch (Throwable e){
//                    System.out.println("Ошибка в потоке: " + e.getMessage());
                }
                removeCommutatorFromUpdatePool(commutator);
            });
        }
        executorService.shutdown();
        try {
            executorService.awaitTermination(timeout, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            throw new ResponseException("Таймаут обновления коммутаторов");
        }
    }

    private synchronized boolean appendCommutatorInUpdatePool(@NotNull Switch commutator) {
        boolean isNotAppend = !commutatorPoolInTheProcessOfUpdating.add(RemoteUpdatingCommutatorItem.of(commutator));
        if (isNotAppend) throw new ResponseException("Коммутатор уже обновляется");
        stompController.updateCommutatorUpdatePool(commutatorPoolInTheProcessOfUpdating);
        return isNotAppend;
    }

    private synchronized void removeCommutatorFromUpdatePool(@NotNull Switch commutator) {
        boolean isRemoved = commutatorPoolInTheProcessOfUpdating.removeIf(sw -> Objects.equals(sw.id, commutator.getId()));
        if (isRemoved) stompController.updateCommutatorUpdatePool(commutatorPoolInTheProcessOfUpdating);
    }

    public void connectToCommutatorAndUpdate(Switch commutator, Map<Integer, String> models) {

        String modelName = models.get(commutator.getSwmodelId().intValue());
        if (modelName == null)
            throw new ResponseException("Не найдена модель для коммутатора: " + commutator.getName());

        AcpCommutator additionalInfo = commutator.getAdditionalInfo();
        SystemInfo systemInfo = additionalInfo == null ? null : additionalInfo.getSystemInfo();
        List<PortInfo> ports = additionalInfo == null ? null : additionalInfo.getPorts();

        AbstractRemoteAccess remoteAccess = null;

        try {
            if (additionalInfo != null) {
                remoteAccess = remoteAccessFactory.getRemoteAccess(modelName, commutator.getIpaddr());
                remoteAccess.auth();
                SystemInfo receivedSystemInfo = remoteAccess.getSystemInfo();
                List<PortInfo> receivedPorts = remoteAccess.getPorts();
                if (systemInfo == null) {
                    additionalInfo.setSystemInfo(receivedSystemInfo);
                    if (additionalInfo.getPorts() == null || additionalInfo.getPorts().isEmpty()) {
                        additionalInfo.appendPorts(receivedPorts);
                    } else {
                        additionalInfo.clearPorts();
                        additionalInfo.appendPorts(receivedPorts);
                    }
                } else {
                    if (!systemInfo.equals(receivedSystemInfo) || ports.isEmpty() || ports.size() != receivedPorts.size()) {
                        systemInfo.setDevice(receivedSystemInfo.getDevice());
                        systemInfo.setMac(receivedSystemInfo.getMac());
                        systemInfo.setHwVersion(receivedSystemInfo.getHwVersion());
                    }
                    additionalInfo.clearPorts();
                    additionalInfo.appendPorts(receivedPorts);
                    systemInfo.setFwVersion(receivedSystemInfo.getFwVersion());
                    systemInfo.setUptime(receivedSystemInfo.getUptime());
                }
                additionalInfo.getSystemInfo().setLastUpdate(Timestamp.from(Instant.now()));
                additionalInfo.removeOldRemoteUpdateLogs();
                additionalInfo.appendRemoteUpdateLog(RemoteUpdateLog.success(receivedPorts.size(), additionalInfo.getMacTableSize()));
                additionalInfo = acpCommutatorDispatcher.save(additionalInfo);
                for (PortInfo port : additionalInfo.getPorts()) {
                    if (port.isDownlink()) continue;
                    for (FdbItem fdbItem : port.getMacTable()) {
                        DhcpBinding existingSession = getDhcpBindingByMac(fdbItem.getMac());
                        if (existingSession != null) {
                            NetworkConnectionLocation location = networkConnectionLocationDispatcher.checkAndWrite(existingSession, commutator, port, fdbItem);
                            if(additionalInfo.getSystemInfo() != null && additionalInfo.getPorts() != null) {
                                location.setCommutatorInfo(additionalInfo.getSystemInfo().getLastUpdate(), additionalInfo.getPorts(), existingSession.getMacaddr());
                                existingSession.setLastConnectionLocation(location);
                                stompController.updateDhcpBinding(existingSession);
                            }
                        }
                    }
                }
                commutator.setAdditionalInfo(additionalInfo);
                stompController.updateCommutator(commutator);
                stompController.updateBaseCommutator(SwitchBaseInfo.from(commutator,modelName));
            }
            try {
                if(remoteAccess != null) remoteAccess.close();
            }catch (Throwable ignore){}
        } catch (Throwable e) {
            if (additionalInfo != null) {
                additionalInfo.removeOldRemoteUpdateLogs();
                additionalInfo.appendRemoteUpdateLog(RemoteUpdateLog.error(e));
                acpCommutatorDispatcher.save(additionalInfo);
                commutator.setAdditionalInfo(additionalInfo);
                stompController.updateCommutator(commutator);
                stompController.updateBaseCommutator(SwitchBaseInfo.from(commutator,modelName));
            }
            try {
                if(remoteAccess != null) remoteAccess.close();
            }catch (Throwable ignore){}
            throw new ResponseException(e.getMessage());
        }
    }

    public List<FdbItem> getFdbByPort(Long id) {
        List<FdbItem> fdbByPort = fdbItemDispatcher.getFdbByPort(id);
        fdbByPort.forEach(fdbItem -> {
            fdbItem.setDhcpBinding(getDhcpBindingByMac(fdbItem.getMac()));
        });
        return fdbByPort;
    }

    public void multicastUpdateCommutatorRemoteUpdatePool() {
        stompController.updateCommutatorUpdatePool(commutatorPoolInTheProcessOfUpdating);
    }

    public NCLHistoryWrapper getNetworkConnectionLocationHistory(Integer id) {
        List<NetworkConnectionLocation> nclByBinding = networkConnectionLocationDispatcher.getAllByBindingId(id);



        Long start = nclByBinding.stream().map(ncl->ncl.getCreatedAt().getTime()).min(Long::compareTo).orElse(0L);
        Long end = nclByBinding.stream().map(ncl->ncl.getCheckedAt().getTime()).max(Long::compareTo).orElse(0L);

        if(start > end) end = start;

        Long duration = end - start;
        Long step = duration / 100;
        if(step == 0L) step = 1L;
        AtomicLong prevPercent = new AtomicLong(-1L);

        Long finalStep = step;
        Map<String, List<NCLHistoryItem>> items = nclByBinding.stream().map(ncl->{
            Long nclStart = ncl.getCreatedAt().getTime();
            Long nclEnd = ncl.getCheckedAt().getTime();
            Long nclStartPercent = (nclStart - start) / finalStep;
            if(nclStartPercent.equals(prevPercent.get())) nclStartPercent++;
            Long nclEndPercent = ((nclEnd - nclStart) / finalStep)+nclStartPercent;
            if(nclEndPercent <= nclStartPercent) nclEndPercent = nclStartPercent+1;
            if(nclEndPercent > 99L) nclEndPercent = 99L;
            prevPercent.set(nclEndPercent);
            return NCLHistoryItem.of(ncl, nclStartPercent.intValue(), nclEndPercent.intValue());
        }).collect(Collectors.groupingBy(NCLHistoryItem::getConnectionName));

        return new NCLHistoryWrapper(new Timestamp(start), new Timestamp(end), items);
    }

    public List<TopologyStreet> getTopology() {
        TopologyStreet[] topology = get(TopologyStreet[].class, Map.of(), "topology");
        if(topology == null) return new ArrayList<>();
        return Arrays.asList(topology);
    }

    public AcpHouse getBuilding(Integer id) {
        return get(AcpHouse.class, Map.of(), "building", id.toString());
    }

    public List<CommutatorListItem> getCommutatorsByBuildingId(Integer id) {
        CommutatorListItem[] commutatorsByBuildingId = get(CommutatorListItem[].class, Map.of(), "building", id.toString(), "commutators");
        if(commutatorsByBuildingId == null) return new ArrayList<>();
        return Arrays.stream(commutatorsByBuildingId).collect(Collectors.toList());
    }

    public Page<DhcpBinding> getBindingsByBuildingId(Integer id, Integer page, @Nullable BindingFilter filter) {
        RequestEntity.BodyBuilder request = RequestEntity.post(url(Map.of(), "building", id.toString(), "bindings", page.toString()));
        RestPage<DhcpBinding> responseBody;
        if(filter != null) {
            responseBody = restTemplate.exchange(request.body(filter), new ParameterizedTypeReference<RestPage<DhcpBinding>>() {
            }).getBody();
        }else{
            responseBody = restTemplate.exchange(request.build(), new ParameterizedTypeReference<RestPage<DhcpBinding>>() {
            }).getBody();
        }
        if(responseBody == null) return Page.empty();
        responseBody.forEach(this::prepareBinding);
        return responseBody;
    }

    public Page<DhcpBinding> getBindingsByVlan(Integer vlan, Integer page, @Nullable BindingFilter filter) {
        RequestEntity.BodyBuilder request = RequestEntity.post(url(Map.of(), "vlan", vlan.toString(), "bindings", page.toString()));
        RestPage<DhcpBinding> responseBody;
        if(filter != null) {
            responseBody = restTemplate.exchange(request.body(filter), new ParameterizedTypeReference<RestPage<DhcpBinding>>() {
            }).getBody();
        }else{
            responseBody = restTemplate.exchange(request.build(), new ParameterizedTypeReference<RestPage<DhcpBinding>>() {
            }).getBody();
        }
        if(responseBody == null) return Page.empty();
        responseBody.forEach(this::prepareBinding);
        return responseBody;
    }

    public Page<DhcpBinding> getBindingsFromBuildingByLogin(String login, Integer page, @Nullable BindingFilter filter) {
        RequestEntity.BodyBuilder request = RequestEntity.post(url(Map.of(), "user", login, "bindings-from-building", page.toString()));
        RestPage<DhcpBinding> responseBody;
        if(filter != null) {
            responseBody = restTemplate.exchange(request.body(filter), new ParameterizedTypeReference<RestPage<DhcpBinding>>() {
            }).getBody();
        }else{
            responseBody = restTemplate.exchange(request.build(), new ParameterizedTypeReference<RestPage<DhcpBinding>>() {
            }).getBody();
        }
        if(responseBody == null) return Page.empty();
        responseBody.forEach(this::prepareBinding);
        return responseBody;
    }

    public Page<DhcpBinding> getBindingsByCommutator(Long commutatorId, Integer page, @Nullable BindingFilter filter) {
        List<FdbItem.PortWithMac> fdbByCommutator = fdbItemDispatcher.getFdbByCommutator(commutatorId).stream().map(FdbItem.PortWithMac::of).toList();
        if(fdbByCommutator.isEmpty())
            return Page.empty();

        RequestEntity.BodyBuilder request = RequestEntity.post(url(Map.of(), "fdb", "bindings", page.toString()));
        RestPage<DhcpBinding> responseBody;
        if(filter != null) {
            filter.setMacs(fdbByCommutator);
            responseBody = restTemplate.exchange(request.body(filter), new ParameterizedTypeReference<RestPage<DhcpBinding>>() {
            }).getBody();
        }else{
            responseBody = restTemplate.exchange(request.body(BindingFilter.from(fdbByCommutator)), new ParameterizedTypeReference<RestPage<DhcpBinding>>() {
            }).getBody();
        }
        if(responseBody == null) return Page.empty();

        responseBody.forEach(this::prepareBinding);

        BindingFilter.SortItem portsSorting = filter != null && filter.getSort() != null ?
                filter.getSort().stream().filter(sortItem -> sortItem.field.equals("ports")).findFirst().orElse(null) : null;

        List<DhcpBinding> dhcpBindings = responseBody.stream().peek(binding -> {
            String macaddr = binding.getMacaddr();
            String portList = fdbByCommutator.stream()
                    .filter(fdb -> fdb.getMac().equals(macaddr))
                    .map(FdbItem.PortWithMac::getPort)
                    .collect(Collectors.joining(", "));
            binding.setPortList(portList);
        }).toList();

        if(portsSorting != null){
            Comparator<DhcpBinding> bindingComparator = Comparator.nullsLast(Comparator.comparing(DhcpBinding::getPortList));
            if(portsSorting.order < 0) bindingComparator =  bindingComparator.reversed();
            return new PageImpl<>(dhcpBindings.stream().sorted(bindingComparator).toList(), responseBody.getPageable(), responseBody.getTotalElements());
        }

        return new PageImpl<>(dhcpBindings, responseBody.getPageable(), responseBody.getTotalElements());
    }

    public Page<DhcpBinding> getBindingsTable(TablePaging paging) {
        RequestEntity.BodyBuilder request = RequestEntity.post(url(Map.of(), "bindings", "table"));
        RestPage<DhcpBinding> responseBody = restTemplate.exchange(request.body(paging), new ParameterizedTypeReference<RestPage<DhcpBinding>>() {}).getBody();
        if(responseBody == null) return Page.empty();
        responseBody.forEach(this::prepareBinding);
        return responseBody;
    }

    @Nullable
    public DhcpBinding getActiveBinding(String login) {
        return get(DhcpBinding.class, Map.of(), "user", login, "active-binding");
    }

    @Nullable
    public Integer getBuildingIdByVlan(Integer vlan) {
        return get(Integer.class, Map.of(), "vlan", vlan.toString(), "building-id");
    }

    @Getter
    @Setter
    @AllArgsConstructor
    @NoArgsConstructor
    public static class NCLHistoryItem {
        private Long nclId;
        private String color;
        private String borderColor;
        private String connectionName;
        private Integer commutatorId;
        private Integer bindingId;
        private Timestamp timeStart;
        private Timestamp timeEnd;
        private Integer percentStart;
        private Integer percentEnd;

        public static NCLHistoryItem of(NetworkConnectionLocation ncl, Integer percentStart, Integer percentEnd) {
            int nameHash = ncl.getConnectionName().hashCode();
            return new NCLHistoryItem(ncl.getNetworkConnectionLocationId(), color(nameHash, 0.8f, 0.9f), color(nameHash, 0.8f, 0.75f), ncl.getConnectionName(), ncl.getCommutatorId(),
                    ncl.getDhcpBindingId(), ncl.getCreatedAt(), ncl.getCheckedAt(), percentStart, percentEnd);
        }

        private static String color(int hash, float saturation, float lightness) {
            int b = Math.round((360f/255f)*(hash & 0x0000FF));
            return "hsl("+b+","+saturation*100+"%,"+lightness*100+"%)";
        }
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class NCLHistoryWrapper{
        private Timestamp from;
        private Timestamp to;
        private Map<String, List<NCLHistoryItem>> nclItems;
    }

    @Getter
    @Setter
    @AllArgsConstructor
    public static class RemoteUpdatingCommutatorItem {
        private Integer id;
        private String name;
        private String ip;

        public static RemoteUpdatingCommutatorItem of(Switch sw) {
            return new RemoteUpdatingCommutatorItem(sw.getId(), sw.getName(), sw.getIpaddr());
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof RemoteUpdatingCommutatorItem that)) return false;
            return getId().equals(that.getId()) && getName().equals(that.getName());
        }

        @Override
        public int hashCode() {
            return Objects.hash(getId(), getName());
        }
    }

    @Data
    public static class TopologyStreet {
        private String streetName;
        private List<TopologyHouse> houses;

        public static TopologyStreet of(Street street, List<TopologyHouse> topologyHouses) {
            TopologyStreet topologyStreet = new TopologyStreet();
            topologyStreet.setStreetName(street.getName());
            topologyStreet.setHouses(topologyHouses);
            return topologyStreet;
        }
    }

    @Data
    public static class TopologyHouse {
        private Integer buildingId;
        private String houseNum;

        public static TopologyHouse of(Building building) {
            TopologyHouse house = new TopologyHouse();
            house.setBuildingId(building.getId());
            house.setHouseNum(building.getHouseNum());
            return house;
        }
    }

    @Data
    public static class CommutatorListItem {
        private Integer id;
        private String ip;
        private String name;
        private String model;
        private String type;
        @Nullable
        private CommutatorListItem uplink;
    }

    @Data
    public static class BindingFilter {
        private Integer size;
        @Nullable
        private String status;
        @Nullable
        private List<SortItem> sort;
        @Nullable
        private List<FdbItem.PortWithMac> macs;

        @Data
        public static class SortItem{
            private String field;
            private Integer order;
        }

        public static BindingFilter from(List<FdbItem.PortWithMac> macs){
            BindingFilter filter = new BindingFilter();
            filter.setSize(25);
            filter.setMacs(macs);
            return filter;
        }
    }
}

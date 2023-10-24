package com.microel.trackerbackend.services.external.acp;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.microel.trackerbackend.controllers.configuration.ConfigurationStorage;
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
import com.microel.trackerbackend.storage.entities.address.House;
import com.microel.trackerbackend.storage.exceptions.IllegalFields;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Lazy;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.data.domain.Page;
import org.springframework.http.RequestEntity;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import javax.validation.constraints.NotNull;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

@Service
public class AcpClient {
    private final RestTemplate restTemplate = new RestTemplateBuilder().build();
    private final ConfigurationStorage configurationStorage;
    private final HouseDispatcher houseDispatcher;
    private final AcpCommutatorDispatcher acpCommutatorDispatcher;
    private final PortInfoDispatcher portInfoDispatcher;
    private final FdbItemDispatcher fdbItemDispatcher;
    private final NetworkConnectionLocationDispatcher networkConnectionLocationDispatcher;
    private final Set<RemoteUpdatingCommutatorItem> commutatorPoolInTheProcessOfUpdating = ConcurrentHashMap.newKeySet();
    private final RAFactory remoteAccessFactory;
    private final CommutatorsAvailabilityCheckService availabilityCheckService;
    private final StompController stompController;
    private AcpConf configuration;

    public AcpClient(ConfigurationStorage configurationStorage, HouseDispatcher houseDispatcher, AcpCommutatorDispatcher acpCommutatorDispatcher, PortInfoDispatcher portInfoDispatcher, FdbItemDispatcher fdbItemDispatcher, NetworkConnectionLocationDispatcher networkConnectionLocationDispatcher, RAFactory remoteAccessFactory, @Lazy CommutatorsAvailabilityCheckService availabilityCheckService, StompController stompController) {
        this.configurationStorage = configurationStorage;
        configuration = configurationStorage.loadOrDefault(AcpConf.class, new AcpConf());
        this.houseDispatcher = houseDispatcher;
        this.acpCommutatorDispatcher = acpCommutatorDispatcher;
        this.portInfoDispatcher = portInfoDispatcher;
        this.fdbItemDispatcher = fdbItemDispatcher;
        this.networkConnectionLocationDispatcher = networkConnectionLocationDispatcher;
        this.remoteAccessFactory = remoteAccessFactory;
        this.availabilityCheckService = availabilityCheckService;
        this.stompController = stompController;
    }

    public List<DhcpBinding> getBindingsByLogin(String login) {
        DhcpBinding[] dhcpBindings = get(DhcpBinding[].class, Map.of("login", login), "dhcp", "bindings");
        if (dhcpBindings == null) return Collections.emptyList();
        List<DhcpBinding> bindings = Arrays.stream(dhcpBindings).sorted(Comparator.comparing(DhcpBinding::getSessionTime).reversed()).collect(Collectors.toList());
        bindings.forEach(this::prepareBinding);
        return bindings;
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
                location.setCommutatorInfo(foundCommutator.getSystemInfo().getLastUpdate(), foundCommutator.getPorts());
        }
        binding.setLastConnectionLocation(location);
        return binding;
    }

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

    public Page<Switch> getCommutators(Integer page, @Nullable String name, @Nullable String ip, @Nullable Integer buildingId, @Nullable Integer pageSize) {
        Map<String, String> query = new HashMap<>();
        if (name != null) query.put("name", name);
        if (ip != null) query.put("ip", ip);
        if (buildingId != null) query.put("buildingId", buildingId.toString());
        if (pageSize != null) query.put("pageSize", pageSize.toString());
        RequestEntity<Void> request = RequestEntity.get(url(query, "commutators", page.toString())).build();
        return Objects.requireNonNull(restTemplate.exchange(request, new ParameterizedTypeReference<RestPage<Switch>>() {
        }).getBody()).map(commutator -> {
            if (commutator.getBuildId() == null) return commutator;
            House house = houseDispatcher.getByAcpBindId(commutator.getBuildId());
            AcpCommutator additionalInfo = acpCommutatorDispatcher.getById(commutator.getId());
            if (additionalInfo != null) commutator.setAdditionalInfo(additionalInfo);
            if (house != null) commutator.setAddress(house.getAddress());
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
        return Arrays.stream(switches).collect(Collectors.toList());
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
        configurationStorage.save(configuration);
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
        } catch (Throwable e) {
            throw new ResponseException(e.getMessage());
        }
    }

    public void getCommutatorRemoteUpdate(Integer id) {
        SwitchWithAddress commutator = getCommutator(id);
        if (commutator == null) throw new ResponseException("Не найден коммутатор в ACP с id: " + id);

        Map<Integer, String> commutatorModels = getCommutatorModels(null).stream().collect(Collectors.toMap(SwitchModel::getId, SwitchModel::getName));
        AcpCommutator additionalInfo = commutator.getCommutator().getAdditionalInfo();
        SystemInfo systemInfo = additionalInfo == null ? null : commutator.getCommutator().getAdditionalInfo().getSystemInfo();
        List<PortInfo> ports = additionalInfo == null ? null : commutator.getCommutator().getAdditionalInfo().getPorts();

        connectToCommutatorAndUpdate(commutator.getCommutator(), additionalInfo, systemInfo, ports, commutatorModels);
    }

    public void getCommutatorsByVlanRemoteUpdate(Integer vlan) {
        List<Switch> commutatorsByVlan = getCommutatorsByVlan(vlan);
        Map<Integer, String> commutatorModels = getCommutatorModels(null).stream().collect(Collectors.toMap(SwitchModel::getId, SwitchModel::getName));
        CountDownLatch latch = new CountDownLatch(commutatorsByVlan.size());
        for (Switch commutator : commutatorsByVlan) {
            Executors.newSingleThreadExecutor().execute(() -> {
                try {
                    AcpCommutator additionalInfo = commutator.getAdditionalInfo();
                    SystemInfo systemInfo = additionalInfo == null ? null : commutator.getAdditionalInfo().getSystemInfo();
                    List<PortInfo> ports = additionalInfo == null ? null : commutator.getAdditionalInfo().getPorts();
                    connectToCommutatorAndUpdate(commutator, additionalInfo, systemInfo, ports, commutatorModels);
                } catch (Throwable ignore) {
                } finally {
                    latch.countDown();
                }
            });
        }
        try {
            latch.await(60, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            throw new ResponseException("Таймаут обновления коммутаторов");
        }
    }

    public void getAllCommutatorsRemoteUpdate() {

        Map<Integer, String> commutatorModels = getCommutatorModels(null).stream().collect(Collectors.toMap(SwitchModel::getId, SwitchModel::getName));

        int page = 0;
        Page<Switch> commutatorsPage = getCommutators(page, null, null, null, 25);
        while (!commutatorsPage.getContent().isEmpty()) {
            CountDownLatch latch = new CountDownLatch(commutatorsPage.getContent().size());
            for (Switch commutator : commutatorsPage.getContent()) {
                AcpCommutator additionalInfo = commutator.getAdditionalInfo();
                SystemInfo systemInfo = additionalInfo == null ? null : commutator.getAdditionalInfo().getSystemInfo();
                List<PortInfo> ports = additionalInfo == null ? null : commutator.getAdditionalInfo().getPorts();
                Thread thread = new Thread(() -> {
                    try {
//                        System.out.println("Begin update commutator " + commutator.getName());
                        connectToCommutatorAndUpdate(commutator, additionalInfo, systemInfo, ports, commutatorModels);
//                        System.out.println("Commutator " + commutator.getName() + " updated");
                    } catch (Throwable e) {
                        System.out.println(e.getMessage());
                    }
                    latch.countDown();
                });
                thread.start();
            }
            try {
                latch.await(30, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                throw new RuntimeException(e.getMessage());
            }
            commutatorsPage = getCommutators(++page, null, null, null, 25);
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

    private void connectToCommutatorAndUpdate(Switch commutator, AcpCommutator additionalInfo, SystemInfo systemInfo, List<PortInfo> ports, Map<Integer, String> models) {

        String modelName = models.get(commutator.getSwmodelId().intValue());
        if (modelName == null)
            throw new ResponseException("Не найдена модель для коммутатора: " + commutator.getName());

        appendCommutatorInUpdatePool(commutator);

        try {
            if (additionalInfo != null) {
                AbstractRemoteAccess remoteAccess = remoteAccessFactory.getRemoteAccess(modelName, commutator.getIpaddr());
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
                remoteAccess.close();

                for (PortInfo port : additionalInfo.getPorts()) {
                    if (port.isDownlink()) continue;
                    for (FdbItem fdbItem : port.getMacTable()) {
                        DhcpBinding existingSession = getDhcpBindingByMac(fdbItem.getMac());
                        if (existingSession != null) {
                            NetworkConnectionLocation location = networkConnectionLocationDispatcher.checkAndWrite(existingSession, commutator, port, fdbItem);
                            if(additionalInfo.getSystemInfo() != null && additionalInfo.getPorts() != null) {
                                location.setCommutatorInfo(additionalInfo.getSystemInfo().getLastUpdate(), additionalInfo.getPorts());
                                existingSession.setLastConnectionLocation(location);
                                stompController.updateDhcpBinding(existingSession);
                            }
                        }
                    }
                }
                commutator.setAdditionalInfo(additionalInfo);
                stompController.updateCommutator(commutator);
            }
        } catch (Throwable e) {
            if (additionalInfo != null) {
                additionalInfo.removeOldRemoteUpdateLogs();
                additionalInfo.appendRemoteUpdateLog(RemoteUpdateLog.error(e));
                acpCommutatorDispatcher.save(additionalInfo);
                commutator.setAdditionalInfo(additionalInfo);
                stompController.updateCommutator(commutator);
            }
            throw new ResponseException(e.getMessage());
        } finally {
            removeCommutatorFromUpdatePool(commutator);
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
}

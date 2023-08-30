package com.microel.trackerbackend.services.external.acp;

import com.microel.trackerbackend.controllers.configuration.ConfigurationStorage;
import com.microel.trackerbackend.controllers.configuration.entity.AcpConf;
import com.microel.trackerbackend.modules.exceptions.Unconfigured;
import com.microel.trackerbackend.services.api.ResponseException;
import com.microel.trackerbackend.services.api.StompController;
import com.microel.trackerbackend.services.external.RestPage;
import com.microel.trackerbackend.services.external.acp.types.DhcpBinding;
import com.microel.trackerbackend.services.external.acp.types.Switch;
import com.microel.trackerbackend.services.external.acp.types.SwitchModel;
import com.microel.trackerbackend.services.external.acp.types.SwitchWithAddress;
import com.microel.trackerbackend.storage.dispatchers.AcpCommutatorDispatcher;
import com.microel.trackerbackend.storage.dispatchers.HouseDispatcher;
import com.microel.trackerbackend.storage.entities.acp.AcpCommutator;
import com.microel.trackerbackend.storage.entities.acp.AcpHouse;
import com.microel.trackerbackend.storage.entities.address.House;
import com.microel.trackerbackend.storage.exceptions.IllegalFields;
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

import java.util.*;
import java.util.stream.Collectors;

@Service
public class AcpClient {
    private final RestTemplate restTemplate = new RestTemplateBuilder().build();
    private final ConfigurationStorage configurationStorage;
    private final HouseDispatcher houseDispatcher;
    private final AcpCommutatorDispatcher acpCommutatorDispatcher;
    private final CommutatorsAvailabilityCheckService availabilityCheckService;
    private final StompController stompController;
    private AcpConf configuration;

    public AcpClient(ConfigurationStorage configurationStorage, HouseDispatcher houseDispatcher, AcpCommutatorDispatcher acpCommutatorDispatcher, @Lazy CommutatorsAvailabilityCheckService availabilityCheckService, StompController stompController) {
        this.configurationStorage = configurationStorage;
        configuration = configurationStorage.loadOrDefault(AcpConf.class, new AcpConf());
        this.houseDispatcher = houseDispatcher;
        this.acpCommutatorDispatcher = acpCommutatorDispatcher;
        this.availabilityCheckService = availabilityCheckService;
        this.stompController = stompController;
    }

    public List<DhcpBinding> getBindingsByLogin(String login) {
        DhcpBinding[] dhcpBindings = get(DhcpBinding[].class, Map.of("login", login), "dhcp", "bindings");
        if (dhcpBindings == null) return Collections.emptyList();
        return Arrays.stream(dhcpBindings).sorted(Comparator.comparing(DhcpBinding::getSessionTime).reversed()).collect(Collectors.toList());
    }

    public RestPage<DhcpBinding> getLastBindings(Integer page, @Nullable Short state, @Nullable String macaddr, @Nullable String login, @Nullable String ip, @Nullable Integer vlan, @Nullable Integer buildingId) {
        Map<String, String> query = new HashMap<>();
        if (state != null) query.put("state", state.toString());
        if (macaddr != null) query.put("macaddr", macaddr);
        if (login != null) query.put("login", login);
        if (ip != null) query.put("ip", ip);
        if (vlan != null) query.put("vlan", vlan.toString());
        if (buildingId != null) query.put("buildingId", buildingId.toString());
        RequestEntity<Void> request = RequestEntity.get(url(query, "dhcp", "bindings", page.toString(), "last")).build();
        return restTemplate.exchange(request, new ParameterizedTypeReference<RestPage<DhcpBinding>>() {
        }).getBody();
    }

    public RestPage<DhcpBinding> getDhcpBindingsByVlan(Integer page, Integer id, String excludeLogin) {
        RequestEntity<Void> request = RequestEntity.get(url(Map.of("excludeLogin", excludeLogin), "vlan", id.toString(), "dhcp", "bindings", page.toString())).build();
        return restTemplate.exchange(request, new ParameterizedTypeReference<RestPage<DhcpBinding>>() {
        }).getBody();
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

    public Page<Switch> getCommutators(Integer page, @Nullable String name, @Nullable String ip, @Nullable Integer buildingId) {
        Map<String, String> query = new HashMap<>();
        if (name != null) query.put("name", name);
        if (ip != null) query.put("ip", ip);
        if (buildingId != null) query.put("buildingId", buildingId.toString());
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

    public SwitchWithAddress getCommutator(Integer id) {
        return get(SwitchWithAddress.class, Map.of(), "commutator", id.toString());
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
}

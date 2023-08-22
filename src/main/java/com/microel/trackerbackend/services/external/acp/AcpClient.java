package com.microel.trackerbackend.services.external.acp;

import com.fasterxml.jackson.core.type.TypeReference;
import com.microel.trackerbackend.controllers.configuration.ConfigurationStorage;
import com.microel.trackerbackend.controllers.configuration.entity.AcpConf;
import com.microel.trackerbackend.modules.exceptions.Unconfigured;
import com.microel.trackerbackend.services.api.ResponseException;
import com.microel.trackerbackend.services.api.StompController;
import com.microel.trackerbackend.services.external.RestPage;
import com.microel.trackerbackend.services.external.acp.types.DhcpBinding;
import com.microel.trackerbackend.services.external.acp.types.Switch;
import com.microel.trackerbackend.storage.entities.acp.AcpHouse;
import com.microel.trackerbackend.storage.exceptions.IllegalFields;
import io.netty.handler.codec.http.HttpMethod;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpEntity;
import org.springframework.http.RequestEntity;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class AcpClient {
    private final RestTemplate restTemplate = new RestTemplateBuilder().build();
    private final ConfigurationStorage configurationStorage;
    private final StompController stompController;
    private AcpConf configuration;

    public AcpClient(ConfigurationStorage configurationStorage, StompController stompController) {
        this.configurationStorage = configurationStorage;
        configuration = configurationStorage.loadOrDefault(AcpConf.class, new AcpConf());
        this.stompController = stompController;
    }

    public List<DhcpBinding> getBindingsByLogin(String login) {
        return Arrays.stream(
                get(DhcpBinding[].class, Map.of("login", login), "dhcp", "bindings")
        ).sorted(Comparator.comparing(DhcpBinding::getSessionTime).reversed()).collect(Collectors.toList());
    }

    public RestPage<DhcpBinding> getDhcpBindingsByVlan(Integer page, Integer id, String excludeLogin) {
        RequestEntity<Void> request = RequestEntity.get(url(Map.of("excludeLogin", excludeLogin), "vlan", id.toString(), "dhcp", "bindings", page.toString())).build();
        return restTemplate.exchange(request, new ParameterizedTypeReference<RestPage<DhcpBinding>>(){}).getBody();
    }

    public List<AcpHouse> getHouses(@Nullable String query) {
        return Arrays.stream(
                get(AcpHouse[].class, Map.of("query", query == null ? "" : query), "buildings")
        ).collect(Collectors.toList());
    }

    public List<Switch> getSwitchesByBuildingId(Integer buildingId) {
        return Arrays.stream(
                get(Switch[].class, Map.of(), "building", buildingId.toString(), "switches")
        ).collect(Collectors.toList());
    }

    public List<Switch> getSwitchesByVlanId(Integer vlanId) {
        return Arrays.stream(
                get(Switch[].class, Map.of(), "vlan", vlanId.toString(), "switches")
        ).collect(Collectors.toList());
    }

    private <T> T get(Class<T> clazz, Map<String, String> query, String... params) {
        try {
            T object = this.restTemplate.getForObject(url(query, params), clazz);
            if (object == null) throw new ResponseException("Пустой ответ от ACP");
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
        }catch (Throwable e) {
            throw new ResponseException(e.getMessage());
        }
    }
}

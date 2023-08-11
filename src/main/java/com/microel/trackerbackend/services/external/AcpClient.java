package com.microel.trackerbackend.services.external;

import com.microel.trackerbackend.modules.StaticConfigurationModule;
import com.microel.trackerbackend.modules.exceptions.Unconfigured;
import com.microel.trackerbackend.services.external.types.acp.DhcpBinding;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class AcpClient {
    private final RestTemplate restTemplate = new RestTemplateBuilder().build();
    private final StaticConfigurationModule staticConfigurationModule;
    private StaticConfigurationModule.Configuration configuration;

    public AcpClient(StaticConfigurationModule staticConfigurationModule) {
        this.staticConfigurationModule = staticConfigurationModule;
        configuration = staticConfigurationModule.getConfiguration();
    }

    public List<DhcpBinding> getBindingsByLogin(String login) {
        return Arrays.stream(
                Objects.requireNonNull(
                        this.restTemplate.getForObject(
                                url(Map.of("login", login), "dhcp", "bindings"),
                                DhcpBinding[].class
                        )
                )
        ).sorted(Comparator.comparing(DhcpBinding::getSessionTime).reversed()).collect(Collectors.toList());
    }

    private String url(Map<String, String> query, String... params) {
        checkConfiguration();
        UriComponentsBuilder uriBuilder = UriComponentsBuilder.fromHttpUrl(configuration.getAcpFlexConnectorUrl() + "api/acp/" + String.join("/", params));
        query.forEach(uriBuilder::queryParam);
        return uriBuilder.build().toUriString();
    }

    private void checkConfiguration() {
        if (configuration == null
                || configuration.getAcpFlexConnectorUrl() == null
                || configuration.getAcpFlexConnectorUrl().isBlank()) {
            throw new Unconfigured("Отсутствует конфигурация интеграции с ACP");
        }
    }
}

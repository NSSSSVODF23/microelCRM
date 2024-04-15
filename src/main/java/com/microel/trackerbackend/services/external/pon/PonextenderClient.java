package com.microel.trackerbackend.services.external.pon;

import com.microel.tdo.chart.TimeDataset;
import com.microel.tdo.dynamictable.TablePaging;
import com.microel.tdo.pon.MacTableEntry;
import com.microel.tdo.pon.OpticalLineTerminal;
import com.microel.tdo.pon.Worker;
import com.microel.tdo.pon.events.OntStatusChangeEvent;
import com.microel.tdo.pon.schema.PonNode;
import com.microel.tdo.pon.schema.PonScheme;
import com.microel.tdo.pon.schema.forms.PonSchemeForm;
import com.microel.tdo.pon.terminal.OpticalNetworkTerminal;
import com.microel.trackerbackend.controllers.configuration.Configuration;
import com.microel.trackerbackend.controllers.configuration.entity.PonextenderConf;
import com.microel.trackerbackend.modules.exceptions.Unconfigured;
import com.microel.trackerbackend.modules.transport.DateRange;
import com.microel.trackerbackend.services.api.ResponseException;
import com.microel.trackerbackend.services.api.StompController;
import com.microel.trackerbackend.services.external.RestPage;
import com.microel.trackerbackend.storage.exceptions.IllegalFields;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.data.domain.Page;
import org.springframework.http.RequestEntity;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.*;

@Component
public class PonextenderClient {
    private final RestTemplate restTemplate = new RestTemplateBuilder().build();
    private final Configuration configurationService;
    private final StompController stompController;
    private PonextenderConf ponextenderConf;

    public PonextenderClient(Configuration configurationService, StompController stompController) {
        this.configurationService = configurationService;
        this.ponextenderConf = configurationService.loadOrDefault(PonextenderConf.class, new PonextenderConf());
        this.stompController = stompController;
    }

    public Page<OpticalNetworkTerminal> getOntTable(TablePaging paging) {
        try {
            RequestEntity.BodyBuilder request = RequestEntity.post(url(Map.of(), "ont", "table"));
            RestPage<OpticalNetworkTerminal> responseBody = restTemplate.exchange(request.body(paging), new ParameterizedTypeReference<RestPage<OpticalNetworkTerminal>>() {
            }).getBody();
            if (responseBody == null) throw new ResponseException("Ошибка при обращении к Ponextender API");
            return responseBody;
        } catch (RestClientException e) {
            throw new ResponseException("Не удалось подключиться к модулю PON");
        }
    }

    public List<OpticalLineTerminal> getOltList() {
        try {
            RequestEntity<Void> request = RequestEntity.get(url(Map.of(), "olt", "list")).build();
            List<OpticalLineTerminal> responseBody = restTemplate.exchange(request, new ParameterizedTypeReference<List<OpticalLineTerminal>>() {
            }).getBody();
            if (responseBody == null) throw new ResponseException("Ошибка при обращении к Ponextender API");
            return responseBody;
        } catch (RestClientException e) {
            throw new ResponseException("Не удалось подключиться к модулю PON");
        }
    }

    public Page<OntStatusChangeEvent> getOntStatusChangeEvents(Integer offset, @Nullable Long oltId, @Nullable Integer port) {
        try {
            Map<String, String> query = new HashMap<>();
            if (oltId != null) query.put("oltId", oltId.toString());
            if (port != null) query.put("port", port.toString());
            RequestEntity<Void> request = RequestEntity.get(url(query, "event", "ont", "status-change", offset.toString())).build();
            Page<OntStatusChangeEvent> responseBody = restTemplate.exchange(request, new ParameterizedTypeReference<RestPage<OntStatusChangeEvent>>() {
            }).getBody();
            if (responseBody == null) throw new ResponseException("Ошибка при обращении к Ponextender API");
            return responseBody;
        } catch (RestClientException e) {
            throw new ResponseException("Не удалось подключиться к модулю PON");
        }
    }

    public Queue<Worker> getWorkerQueue() {
        try {
            RequestEntity<Void> request = RequestEntity.get(url(Map.of(), "worker-queue")).build();
            Queue<Worker> responseBody = restTemplate.exchange(request, new ParameterizedTypeReference<Queue<Worker>>() {
            }).getBody();
            if (responseBody == null) throw new ResponseException("Ошибка при обращении к Ponextender API");
            return responseBody;
        } catch (RestClientException e) {
            throw new ResponseException("Не удалось подключиться к модулю PON");
        }
    }

    public List<TimeDataset> getOntSignalChart(Long id, DateRange timeRange) {
        try {
            if (!timeRange.validate()) throw new ResponseException("Некорректный период данных");
            RequestEntity<Void> request = RequestEntity.get(url(Map.of("from", timeRange.start().toString(), "to", timeRange.end().toString()), "ont", id.toString(), "signal-chart")).build();
            List<TimeDataset> responseBody = restTemplate.exchange(request, new ParameterizedTypeReference<List<TimeDataset>>() {
            }).getBody();
            if (responseBody == null) throw new ResponseException("Ошибка при обращении к Ponextender API");
            return responseBody;
        } catch (RestClientException e) {
            throw new ResponseException("Не удалось подключиться к модулю PON");
        }
    }

    public OpticalNetworkTerminal getOnt(Long id) {
        try {
            RequestEntity<Void> request = RequestEntity.get(url(Map.of(), "ont", id.toString())).build();
            OpticalNetworkTerminal responseBody = restTemplate.exchange(request, new ParameterizedTypeReference<OpticalNetworkTerminal>() {
            }).getBody();
            if (responseBody == null) throw new ResponseException("Ошибка при обращении к Ponextender API");
            return responseBody;
        } catch (RestClientException e) {
            throw new ResponseException("Не удалось подключиться к модулю PON");
        }
    }

    public void renameOnt(Long id, String name) {
        RequestEntity<Void> request = RequestEntity.patch(url(Map.of("name", name), "ont", id.toString(), "rename")).build();
        try {
            restTemplate.exchange(request, new ParameterizedTypeReference<Void>() {
            });
        } catch (Exception e) {
            throw new ResponseException("Не удалось переименовать терминал");
        }
    }

    public void rebootOnt(Long id) {
        RequestEntity<Void> request = RequestEntity.post(url(Map.of(), "ont", id.toString(), "reboot")).build();
        try {
            restTemplate.exchange(request, new ParameterizedTypeReference<Void>() {
            });
        } catch (Exception e) {
            throw new ResponseException("Не удалось перезагрузить терминал");
        }
    }

    public UUID updateOnt(Long id) {
        RequestEntity<Void> request = RequestEntity.post(url(Map.of(), "ont", id.toString(), "update")).build();
        try {
            return restTemplate.exchange(request, new ParameterizedTypeReference<UUID>() {}).getBody();
        } catch (Exception e) {
            throw new ResponseException("Не удалось запросить новые данные по оптическому терминалу");
        }
    }

    public void assignLoginToOnt(Long id, String login) {
        try {
            RequestEntity<Void> request = RequestEntity.patch(url(Map.of("login", login), "ont", id.toString(), "assign-login")).build();
            restTemplate.exchange(request, new ParameterizedTypeReference<Void>() {
            });
        } catch (RestClientException e) {
            throw new ResponseException("Не удалось подключиться к модулю PON");
        }
    }

    public List<OpticalNetworkTerminal> getOntSuggestions(String query) {
        try {
            RequestEntity<Void> request = RequestEntity.get(url(Map.of("query", query), "suggestions", "ont")).build();
            List<OpticalNetworkTerminal> responseBody = restTemplate.exchange(request, new ParameterizedTypeReference<List<OpticalNetworkTerminal>>() {
            }).getBody();
            if (responseBody == null) throw new ResponseException("Ошибка при обращении к Ponextender API");
            return responseBody;
        } catch (RestClientException e) {
            throw new ResponseException("Не удалось подключиться к модулю PON");
        }
    }

    @Nullable
    public OpticalNetworkTerminal getOntByLogin(String login) {
        try {
            RequestEntity<Void> request = RequestEntity.get(url(Map.of(), "ont", "login", login)).build();
            return restTemplate.exchange(request, new ParameterizedTypeReference<OpticalNetworkTerminal>() {
            }).getBody();
        } catch (RestClientException e) {
            throw new ResponseException("Не удалось подключиться к модулю PON");
        }
    }

    public void signLogins(List<MacTableEntry> macTable) {
        try {
            RequestEntity.BodyBuilder request = RequestEntity.post(url(Map.of(), "ont", "sign-logins"));
            restTemplate.exchange(request.body(macTable), new ParameterizedTypeReference<OpticalNetworkTerminal>() {
            });
        } catch (RestClientException e) {
            throw new ResponseException("Не удалось подключиться к модулю PON");
        }
    }

    public PonScheme createScheme(PonSchemeForm form) {
        try {
            RequestEntity.BodyBuilder request = RequestEntity.post(url(Map.of(), "scheme", "create"));
            return restTemplate.exchange(request.body(form), new ParameterizedTypeReference<PonScheme>() {
            }).getBody();
        } catch (RestClientException e) {
            throw new ResponseException("Не удалось подключиться к модулю PON");
        }
    }

    public PonScheme updateScheme(Long id, PonSchemeForm form) {
        try {
            RequestEntity.BodyBuilder request = RequestEntity.patch(url(Map.of(), "scheme", id.toString(), "update"));
            return restTemplate.exchange(request.body(form), new ParameterizedTypeReference<PonScheme>() {
            }).getBody();
        } catch (RestClientException e) {
            throw new ResponseException("Не удалось подключиться к модулю PON");
        }
    }

    public void deleteScheme(Long id) {
        try {
            RequestEntity.HeadersBuilder<?> request = RequestEntity.delete(url(Map.of(), "scheme", id.toString(), "delete"));
            restTemplate.exchange(request.build(),new ParameterizedTypeReference<Void>() {
            });
        } catch (RestClientException e) {
            throw new ResponseException("Не удалось подключиться к модулю PON");
        }
    }

    public List<PonScheme> getSchemes() {
        try {
            RequestEntity<Void> request = RequestEntity.get(url(Map.of(), "scheme", "list")).build();
            return restTemplate.exchange(request, new ParameterizedTypeReference<List<PonScheme>>() {
            }).getBody();
        } catch (RestClientException e) {
            throw new ResponseException("Не удалось подключиться к модулю PON");
        }
    }

    public PonScheme getSchemeById(Long id) {
        try {
            RequestEntity<Void> request = RequestEntity.get(url(Map.of(), "scheme", id.toString())).build();
            return restTemplate.exchange(request, new ParameterizedTypeReference<PonScheme>() {
            }).getBody();
        } catch (RestClientException e) {
            throw new ResponseException("Не удалось подключиться к модулю PON");
        }
    }

    private String url(Map<String, String> query, String... params) {
        checkConfiguration();
        UriComponentsBuilder uriBuilder = UriComponentsBuilder.fromHttpUrl(ponextenderConf.getPonextenderUrl() + String.join("/", params));
        query.forEach(uriBuilder::queryParam);
        return uriBuilder.build().toUriString();
    }

    private void checkConfiguration() {
        if (!ponextenderConf.isFilled()) {
            throw new Unconfigured("Отсутствует конфигурация интеграции с Ponextender API");
        }
    }

    public PonextenderConf getConfiguration() {
        return ponextenderConf;
    }

    public void setConfiguration(PonextenderConf conf) {
        if (!conf.isFilled()) throw new IllegalFields("Конфигурация не заполнена");
        ponextenderConf = conf;
        configurationService.save(ponextenderConf);
//        stompController.changeAcpConfig(ponextenderConf);
    }

    public void editScheme(Long id, List<? extends PonNode> data, String login) {
        try {
            RequestEntity.BodyBuilder request = RequestEntity.patch(url(Map.of("login", login), "scheme", id.toString(), "edit"));
            restTemplate.exchange(request.body(data), new ParameterizedTypeReference<PonScheme>() {
            });
        } catch (RestClientException e) {
            throw new ResponseException("Не удалось подключиться к модулю PON");
        }
    }

    public List<? extends PonNode> getSchemeElements(Long id) {
        try {
            RequestEntity<Void> request = RequestEntity.get(url(Map.of(), "scheme", id.toString(), "elements")).build();
            return restTemplate.exchange(request, new ParameterizedTypeReference<List<? extends PonNode>>() {
            }).getBody();
        } catch (RestClientException e) {
            throw new ResponseException("Не удалось подключиться к модулю PON");
        }
    }
}

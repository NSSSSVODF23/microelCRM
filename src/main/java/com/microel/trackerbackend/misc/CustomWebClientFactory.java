package com.microel.trackerbackend.misc;

import lombok.NoArgsConstructor;
import org.springframework.web.reactive.function.client.ExchangeFilterFunction;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.time.Duration;

@NoArgsConstructor
public class CustomWebClientFactory {

    public Mono<Short> createGetResponse(String url, Short port) {
        return WebClient.create().get().uri(url + ":" + port)
                .exchangeToMono(response -> Mono.just(port))
                .timeout(Duration.ofSeconds(5)).onErrorReturn((short) 0);
    }

    private ExchangeFilterFunction errorHandler() {
        return ExchangeFilterFunction.ofResponseProcessor(clientResponse -> {
            if (clientResponse.statusCode().is5xxServerError()) {
                return clientResponse.bodyToMono(String.class)
                        .flatMap(errorBody -> Mono.error(new Exception(errorBody)));
            } else if (clientResponse.statusCode().is4xxClientError()) {
                return clientResponse.bodyToMono(String.class)
                        .flatMap(errorBody -> Mono.error(new Exception(errorBody)));
            } else {
                return Mono.just(clientResponse);
            }
        });
    }
}

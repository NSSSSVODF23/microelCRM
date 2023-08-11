package com.microel.trackerbackend.misc;

import io.netty.channel.ChannelOption;
import io.netty.handler.timeout.ReadTimeoutHandler;
import io.netty.handler.timeout.WriteTimeoutHandler;
import lombok.NoArgsConstructor;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.ExchangeFilterFunction;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import reactor.netty.http.client.HttpClient;
import reactor.netty.http.client.HttpClientRequest;

import java.time.Duration;


@NoArgsConstructor
public class CustomWebClientFactory {

    public Mono<Short> createGetResponse(String url, Short port) {
        return WebClient.create().get().uri(url+":"+port).exchangeToMono(response -> {
            if (response.statusCode().is2xxSuccessful()) {
                return Mono.just(port);
            } else {
                return Mono.error(new Exception("Error"));
            }
        }).timeout(Duration.ofSeconds(1)).onErrorReturn(Integer.valueOf(0).shortValue());
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

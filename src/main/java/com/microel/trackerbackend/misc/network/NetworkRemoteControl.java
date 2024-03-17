package com.microel.trackerbackend.misc.network;

import com.microel.trackerbackend.controllers.telegram.Utils;
import com.microel.trackerbackend.misc.CustomWebClientFactory;
import lombok.Getter;
import lombok.Setter;
import org.springframework.http.ResponseEntity;
import org.springframework.lang.Nullable;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;

@Getter
@Setter
public class NetworkRemoteControl {

    private String ip;
    @Nullable
    private Short webPort = null;
    @Nullable
    private Short telnetPort = null;
    @Nullable
    private Short sshPort = null;
    private Boolean hasAccess = false;

    @Nullable
    public static Mono<NetworkRemoteControl> of(@Nullable String ip){
        if(!Utils.validIP(ip)) return null;
        NetworkRemoteControl networkRemoteControl = new NetworkRemoteControl();
        networkRemoteControl.ip = ip;
        return networkRemoteControl.checkWebAccess();
    }

    private Mono<NetworkRemoteControl> checkWebAccess(){
        Short[] potentialHttpPorts = {80, 8080, 81, 8081, 8888, 280, 591, 777, 5080, 8090};
        Short[] potentialHttpsPorts = {443, 5083, 5443, 8083, 8443};
        CustomWebClientFactory webClientFactory = new CustomWebClientFactory();
        List<Mono<Short>> httpEndpoint = Arrays.stream(potentialHttpPorts).map(port -> webClientFactory.createGetResponse("http://" + ip, port)).toList();
        List<Mono<Short>> httpsEndpoint = Arrays.stream(potentialHttpsPorts).map(port -> webClientFactory.createGetResponse("https://" + ip, port)).toList();
        return Flux.merge(
                Stream.concat(httpEndpoint.stream(), httpsEndpoint.stream()).toList()
        ).filter(val->!val.equals(Integer.valueOf(0).shortValue())).next().map(response -> {
            webPort = response;
            hasAccess = true;
            return this;
        }).defaultIfEmpty(new NetworkRemoteControl());
    }
}

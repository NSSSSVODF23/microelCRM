package com.microel.trackerbackend.misc.network;

import com.microel.trackerbackend.controllers.telegram.Utils;
import com.microel.trackerbackend.misc.CustomWebClientFactory;
import lombok.Getter;
import lombok.Setter;
import org.springframework.http.ResponseEntity;
import org.springframework.lang.Nullable;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Arrays;

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
        Short[] potentialPorts = {80, 8080, 8081, 8888, 443};
        CustomWebClientFactory webClientFactory = new CustomWebClientFactory();
        return Flux.merge(
            Arrays.stream(potentialPorts).map(port->webClientFactory.createGetResponse("http://" + ip , port)).toList()
        ).filter(val->!val.equals(Integer.valueOf(0).shortValue())).next().map(response -> {
            webPort = response;
            hasAccess = true;
            return this;
        }).defaultIfEmpty(new NetworkRemoteControl());
    }
}

package com.microel.trackerbackend.controllers.configuration.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.microel.confstore.AbstractConfiguration;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class PonextenderConf implements AbstractConfiguration {

    private String ponextenderUrl;

    @Override
    public String fileName() {
        return "ponextender.conf";
    }

    @Override
    @JsonIgnore
    public Boolean isFilled() {
        if (ponextenderUrl == null || ponextenderUrl.isBlank()) return false;
        return true;
    }

    @Override
    public String toString() {
        return "AcpConf{" +
                "ponextenderUrl='" + ponextenderUrl + '\'' +
                '}';
    }
}

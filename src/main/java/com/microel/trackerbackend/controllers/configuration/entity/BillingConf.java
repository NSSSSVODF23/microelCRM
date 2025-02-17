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
public class BillingConf implements AbstractConfiguration {
    private String host;
    private Integer port;
    private String login;
    private String password;
    private String daemonName;
    private String selfIp;

    @Override
    public String fileName() {
        return "billing.conf";
    }

    @Override
    @JsonIgnore
    public Boolean isFilled() {
        if (host == null || host.isBlank()) return false;
        if (port == null) return false;
        if (login == null || login.isBlank()) return false;
        if (password == null || password.isBlank()) return false;
        if (daemonName == null || daemonName.isBlank()) return false;
        return selfIp != null && !selfIp.isBlank();
    }

    @Override
    public String toString() {
        return "BillingConf{" +
                "host='" + host + '\'' +
                ", port=" + port +
                ", login='" + login + '\'' +
                ", password='" + password + '\'' +
                ", daemonName='" + daemonName + '\'' +
                '}';
    }
}

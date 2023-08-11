package com.microel.trackerbackend.controllers.configuration.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.microel.trackerbackend.controllers.configuration.AbstractConfiguration;
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
    private String selfIp;

    @Override
    @JsonIgnore
    public Boolean isFilled() {
        if (host == null || host.isBlank()) return false;
        if (port == null) return false;
        if (login == null || login.isBlank()) return false;
        if (password == null || password.isBlank()) return false;
        if (selfIp == null || selfIp.isBlank()) return false;
        return true;
    }

    @Override
    public String toString() {
        String sb = "BillingConf{" +
                "host='" + host + '\'' +
                ", port=" + port +
                ", login='" + login + '\'' +
                ", password='" + password + '\'' +
                '}';
        return sb;
    }
}

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
public class UserTelegramConf implements AbstractConfiguration {
    private String botToken;
    private String botName;
    private String notificationChannelId;
    private String microelHubIpPort;


    @Override
    public String fileName() {
        return "user_telegram.conf";
    }

    @Override
    @JsonIgnore
    public Boolean isFilled() {
        if(botToken == null || botToken.isBlank()) return false;
        return botName != null && !botName.isBlank();
    }

    @Override
    public String toString() {
        return "TelegramConf{" +
                "botToken='" + botToken + '\'' +
                ", botName='" + botName + '\'' +
                '}';
    }
}

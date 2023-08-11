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
public class TelegramConf implements AbstractConfiguration {
    private String botToken;
    private String botName;
    private String dhcpNotificationChatId;


    @Override
    @JsonIgnore
    public Boolean isFilled() {
        if(botToken == null || botToken.isBlank()) return false;
        if(botName == null || botName.isBlank()) return false;
        if(dhcpNotificationChatId == null || dhcpNotificationChatId.isBlank()) return false;
        return true;
    }

    @Override
    public String toString() {
        return "TelegramConf{" +
                "botToken='" + botToken + '\'' +
                ", botName='" + botName + '\'' +
                ", dhcpNotificationChatId='" + dhcpNotificationChatId + '\'' +
                '}';
    }
}

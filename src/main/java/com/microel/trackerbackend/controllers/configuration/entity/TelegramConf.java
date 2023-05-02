package com.microel.trackerbackend.controllers.configuration.entity;

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

    @Override
    public String toString() {
        return "TelegramConf{" +
                "botToken='" + botToken + '\'' +
                ", botName='" + botName + '\'' +
                '}';
    }
}

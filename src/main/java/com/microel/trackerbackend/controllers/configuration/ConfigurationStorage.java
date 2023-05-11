package com.microel.trackerbackend.controllers.configuration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.microel.trackerbackend.controllers.configuration.entity.DefaultCitiesConf;
import com.microel.trackerbackend.controllers.configuration.entity.TelegramConf;
import com.microel.trackerbackend.parsers.oldtracker.AddressCorrectingPool;
import com.microel.trackerbackend.parsers.oldtracker.OldTrackerParserSettings;
import com.microel.trackerbackend.parsers.oldtracker.UncreatedTasksPool;
import org.springframework.stereotype.Component;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

@Component
public class ConfigurationStorage {
    private final String PATH = "./configurations/";
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final Map<Class, String> fileNames = Map.of(
            TelegramConf.class, "telegram.conf",
            OldTrackerParserSettings.class, "oldTracker.conf",
            DefaultCitiesConf.class, "defaultCities.conf",
            AddressCorrectingPool.class, "addressCorrectingPool.json",
            UncreatedTasksPool.class, "uncreatedTasksPool.json"
    );

    public ConfigurationStorage() {
        try {
            Files.createDirectories(Path.of(PATH));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void save(AbstractConfiguration configuration) throws FailedToWriteConfigurationException {
        try {
            Files.writeString(Path.of(PATH + fileNames.get(configuration.getClass())),objectMapper.writeValueAsString(configuration));
        } catch (IOException e) {
            throw new FailedToWriteConfigurationException(e.getMessage());
        }
    }

    public <T extends AbstractConfiguration> T load(Class<T> configurationClass) throws FailedToReadConfigurationException {
        try {
            String rawConfig = Files.readString(Path.of(PATH + fileNames.get(configurationClass)));
            return objectMapper.readValue(rawConfig, configurationClass);
        } catch (IOException e) {
            throw new FailedToReadConfigurationException(e.getMessage());
        }
    }

    public <T extends AbstractConfiguration> T loadOrDefault(Class<T> configurationClass, T defaultConfiguration) {
        try {
            return load(configurationClass);
        } catch (FailedToReadConfigurationException e) {
            try {
                save(defaultConfiguration);
            } catch (FailedToWriteConfigurationException ignore) {
            }
            return defaultConfiguration;
        }
    }
}

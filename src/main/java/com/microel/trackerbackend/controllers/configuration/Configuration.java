package com.microel.trackerbackend.controllers.configuration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.microel.confstore.AbstractConfiguration;
import com.microel.confstore.ConfigurationStorage;
import com.microel.trackerbackend.controllers.configuration.entity.AcpConf;
import com.microel.trackerbackend.controllers.configuration.entity.BillingConf;
import com.microel.trackerbackend.controllers.configuration.entity.DefaultCitiesConf;
import com.microel.trackerbackend.controllers.configuration.entity.TelegramConf;
import com.microel.trackerbackend.parsers.oldtracker.AddressCorrectingPool;
import com.microel.trackerbackend.parsers.oldtracker.OldTrackerParserSettings;
import com.microel.trackerbackend.parsers.oldtracker.UncreatedTasksPool;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

@Service
public class Configuration {
    private final ConfigurationStorage configurationStorage;

    public Configuration() {
        this.configurationStorage = new ConfigurationStorage(null);
    }

    public void save(AbstractConfiguration configuration) throws FailedToWriteConfigurationException {
        try {
            configurationStorage.save(configuration);
        } catch (Exception e) {
            throw new FailedToWriteConfigurationException(e.getMessage());
        }
    }

    public <T extends AbstractConfiguration> T load(Class<T> configurationClass) throws FailedToReadConfigurationException {
        try {
            return configurationStorage.load(configurationClass);
        } catch (Exception e) {
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

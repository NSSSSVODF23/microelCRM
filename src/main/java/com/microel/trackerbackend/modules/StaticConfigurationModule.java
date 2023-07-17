package com.microel.trackerbackend.modules;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Path;

@Slf4j
@Service
public class StaticConfigurationModule {

    private final Path CONFIGURATION_FILE_PATH = Path.of("./configuration.json");
    private final ObjectMapper mapper = new ObjectMapper();

    public StaticConfigurationModule() {

    }

    synchronized public void setConfiguration(Configuration configuration){
        try {
            mapper.writeValue(CONFIGURATION_FILE_PATH.toFile(), configuration);
        } catch (IOException e) {
            log.error("Не удалось записать конфигурационный файл");
        }
    }

    @Nullable
    public Configuration getConfiguration(){
        try {
            return mapper.readValue(CONFIGURATION_FILE_PATH.toFile(), Configuration.class);
        } catch (IOException e) {
            try {
                Configuration newConf = new Configuration();
                mapper.writeValue(CONFIGURATION_FILE_PATH.toFile(), newConf);
                return newConf;
            } catch (IOException ex) {
                log.error("Не удалось записать конфигурационный файл");
            }
        }
        return null;
    }

    public Configuration changeDhcpNotificationChatId(String chatId){
        Configuration configuration = getConfiguration();
        if(configuration == null) return null;
        configuration.setDhcpNotificationChatId(chatId);
        setConfiguration(configuration);
        return configuration;
    }

    @Getter
    @Setter
    public static class Configuration{
        @Nullable
        private String dhcpNotificationChatId;
    }
}

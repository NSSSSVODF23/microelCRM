package com.microel.trackerbackend;

import io.metaloom.video4j.Video4j;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.transaction.annotation.EnableTransactionManagement;

@SpringBootApplication
@EnableTransactionManagement
@EnableScheduling
@Slf4j
public class BackendApplication {
    public BackendApplication() {
        try{
            Video4j.init();
            log.info("Библиотека video4j инициализирована");
        }catch (RuntimeException e){
            log.warn("Библиотека video4j не инициализирована: {}", e.getMessage());
        }
    }

    public static void main(String[] args) {
        SpringApplication.run(BackendApplication.class, args);
    }

}

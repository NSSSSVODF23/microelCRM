package com.microel.trackerbackend;

import com.microel.trackerbackend.misc.Async;
import com.microel.trackerbackend.services.external.acp.AcpClient;
import com.microel.trackerbackend.services.external.acp.CommutatorsAvailabilityCheckService;
import com.microel.trackerbackend.storage.dispatchers.TaskDispatcher;
import io.metaloom.video4j.Video4j;
import lombok.extern.slf4j.Slf4j;
import net.time4j.tz.repo.TZDATA;
import org.apache.xmlrpc.XmlRpcException;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.transaction.annotation.EnableTransactionManagement;

import java.net.MalformedURLException;

@SpringBootApplication
@EnableTransactionManagement
@EnableScheduling
@Slf4j
public class BackendApplication {
    public BackendApplication(AcpClient acpClient, CommutatorsAvailabilityCheckService commutatorsAvailabilityCheckService, TaskDispatcher taskDispatcher) throws MalformedURLException, XmlRpcException {
        Async.of(()->{
            try {
                Video4j.init();
                log.info("Библиотека video4j инициализирована");
            } catch (RuntimeException e) {
                log.warn("Библиотека video4j не инициализирована: {}", e.getMessage());
            }
            TZDATA.init();
        });
    }

    public static void main(String[] args) {
        SpringApplication.run(BackendApplication.class, args);
    }

}

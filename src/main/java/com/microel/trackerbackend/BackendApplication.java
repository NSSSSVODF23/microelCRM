package com.microel.trackerbackend;

import com.microel.trackerbackend.services.external.acp.AcpClient;
import com.microel.trackerbackend.services.external.acp.CommutatorsAvailabilityCheckService;
import com.microel.trackerbackend.storage.dispatchers.CommentDispatcher;
import com.microel.trackerbackend.storage.dispatchers.TaskDispatcher;
import com.microel.trackerbackend.storage.entities.task.Task;
import com.microel.trackerbackend.storage.entities.task.TaskStatus;
import com.microel.trackerbackend.storage.repositories.TaskRepository;
import io.metaloom.video4j.Video4j;
import lombok.extern.slf4j.Slf4j;
import net.time4j.tz.repo.TZDATA;
import org.apache.xmlrpc.XmlRpcException;
import org.hibernate.Hibernate;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Lazy;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.transaction.annotation.EnableTransactionManagement;

import javax.persistence.criteria.Predicate;
import java.io.IOException;
import java.net.MalformedURLException;
import java.security.GeneralSecurityException;
import java.util.ArrayList;
import java.util.List;

@SpringBootApplication
@EnableTransactionManagement
@EnableScheduling
@Slf4j
public class BackendApplication {
    public BackendApplication(AcpClient acpClient, CommutatorsAvailabilityCheckService commutatorsAvailabilityCheckService, TaskDispatcher taskDispatcher) throws MalformedURLException, XmlRpcException {
        try {
            Video4j.init();
            log.info("Библиотека video4j инициализирована");
        } catch (RuntimeException e) {
            log.warn("Библиотека video4j не инициализирована: {}", e.getMessage());
        }
        TZDATA.init();
        taskDispatcher.initializeLastComments();
//        System.out.println(acpClient.getBindingsByLogin("16111630"));
    }

    public static void main(String[] args) {
        SpringApplication.run(BackendApplication.class, args);
    }

}

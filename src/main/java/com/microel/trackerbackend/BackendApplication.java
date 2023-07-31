package com.microel.trackerbackend;

import com.microel.trackerbackend.controllers.billing.BillingRequestController;
import io.metaloom.video4j.Video4j;
import lombok.extern.slf4j.Slf4j;
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
    public BackendApplication() throws MalformedURLException, XmlRpcException {
        try{
            Video4j.init();
            log.info("Библиотека video4j инициализирована");
        }catch (RuntimeException e){
            log.warn("Библиотека video4j не инициализирована: {}", e.getMessage());
        }
//        new BillingRequestController().getUsersByLogin();
//        new BillingRequestController().getUserInfo();
//        new BillingRequestController().getHelp();
    }

    public static void main(String[] args) {
        SpringApplication.run(BackendApplication.class, args);
    }

}

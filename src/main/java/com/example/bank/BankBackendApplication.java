package com.example.bank;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 银行后端应用启动类。
 */
@SpringBootApplication
@EnableFeignClients
@EnableDiscoveryClient
@EnableScheduling
public class BankBackendApplication {

    private static final Logger log = LoggerFactory.getLogger(BankBackendApplication.class);

    public static void main(String[] args) {
        SpringApplication.run(BankBackendApplication.class, args);
    }

    @EventListener(ApplicationReadyEvent.class)
    public void onApplicationReady() {
        log.info("Customer service started successfully. Swagger UI: http://localhost:8082/swagger-ui/index.html");
    }
}

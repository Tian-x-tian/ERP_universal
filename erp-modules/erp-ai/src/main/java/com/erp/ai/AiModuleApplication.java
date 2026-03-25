package com.erp.ai;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;

@SpringBootApplication(scanBasePackages = {
        "com.erp.common",
        "com.erp.ai"
}, exclude = {
        DataSourceAutoConfiguration.class,
        SecurityAutoConfiguration.class
})
@EnableDiscoveryClient
public class AiModuleApplication {
    public static void main(String[] args) {
        SpringApplication.run(AiModuleApplication.class, args);
    }
}

package com.erp.saas.control;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

@EnableDiscoveryClient
@SpringBootApplication(scanBasePackages = {
        "com.erp.common",
        "com.erp.saas.control"
})
public class SaasControlApplication {

    public static void main(String[] args) {
        SpringApplication.run(SaasControlApplication.class, args);
    }
}

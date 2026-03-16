package com.erp.business;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

/**
 * ERP 业务服务启动入口。
 */
@EnableDiscoveryClient
@SpringBootApplication
public class BusinessApplication {

    /**
     * 启动业务服务。
     *
     * @param args 启动参数
     */
    public static void main(String[] args) {
        SpringApplication.run(BusinessApplication.class, args);
    }
}

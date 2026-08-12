package com.erp.system;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

/**
 * ERP 系统模块独立启动入口。
 */
@EnableDiscoveryClient
@SpringBootApplication(scanBasePackages = {
        "com.erp.common",
        "com.erp.system"
})
public class SystemModuleApplication {

    /**
     * 启动系统模块服务。
     *
     * @param args 启动参数
     */
    public static void main(String[] args) {
        SpringApplication.run(SystemModuleApplication.class, args);
    }
}

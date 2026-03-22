package com.erp.business;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

import java.util.Arrays;

/**
 * ERP 业务服务启动入口。
 */
@EnableDiscoveryClient
@SpringBootApplication(scanBasePackages = {
        "com.erp.business",
        "com.erp.common"
})
public class BusinessApplication {
    private static final String DEFAULT_BUSINESS_PORT = "9093";

    /**
     * 启动业务服务。
     *
     * @param args 启动参数
     */
    public static void main(String[] args) {
        applyStandalonePort(args);
        SpringApplication.run(BusinessApplication.class, args);
    }

    /**
     * 为独立启动模式补齐默认端口，避免被共享配置中的 system 端口覆盖。
     *
     * @param args 启动参数
     */
    private static void applyStandalonePort(String[] args) {
        if (System.getProperty("server.port") != null || containsServerPortArgument(args)) {
            return;
        }
        System.setProperty("server.port", DEFAULT_BUSINESS_PORT);
    }

    /**
     * 判断启动参数中是否已显式指定端口。
     *
     * @param args 启动参数
     * @return true 表示已指定 server.port
     */
    private static boolean containsServerPortArgument(String[] args) {
        return Arrays.stream(args == null ? new String[0] : args)
                .anyMatch(arg -> arg != null && arg.startsWith("--server.port="));
    }
}

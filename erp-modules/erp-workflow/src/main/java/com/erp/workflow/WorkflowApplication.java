package com.erp.workflow;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

import java.util.Arrays;

/**
 * 工作流模块独立启动入口。
 */
@EnableDiscoveryClient
@MapperScan("com.erp.workflow.mapper")
@SpringBootApplication(scanBasePackages = {
        "com.erp.common",
        "com.erp.workflow.config",
        "com.erp.workflow.exception",
        "com.erp.workflow.filter",
        "com.erp.workflow.security",
        "com.erp.workflow.service",
        "com.erp.workflow.support",
        "com.erp.workflow"
})
public class WorkflowApplication {
    private static final String DEFAULT_WORKFLOW_PORT = "9094";

    /**
     * 启动工作流模块。
     *
     * @param args 启动参数
     */
    public static void main(String[] args) {
        applyStandalonePort(args);
        SpringApplication.run(WorkflowApplication.class, args);
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
        System.setProperty("server.port", DEFAULT_WORKFLOW_PORT);
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



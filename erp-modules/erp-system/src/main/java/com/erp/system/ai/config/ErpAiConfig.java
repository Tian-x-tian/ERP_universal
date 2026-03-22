package com.erp.system.ai.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

/**
 * ERP AI 基础配置。
 */
@Configuration
public class ErpAiConfig {

    /**
     * 注册 ERP AI 配置属性。
     *
     * @return 配置属性对象
     */
    @Bean
    @ConfigurationProperties(prefix = "erp.ai")
    public ErpAiProperties erpAiProperties() {
        return new ErpAiProperties();
    }

    /**
     * 注册 AI 流式任务执行线程池。
     *
     * @return AI 流式执行器
     */
    @Bean(name = "aiStreamingExecutor")
    public Executor aiStreamingExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(2);
        executor.setMaxPoolSize(4);
        executor.setQueueCapacity(64);
        executor.setThreadNamePrefix("erp-ai-stream-");
        executor.initialize();
        return executor;
    }
}

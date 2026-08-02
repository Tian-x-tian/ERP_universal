package com.erp.ai.config;

import com.erp.common.core.context.TenantContextHolder;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.TaskDecorator;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.concurrent.Executor;
import java.time.Clock;

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

    @Bean
    @ConditionalOnMissingBean(Clock.class)
    public Clock aiQuotaClock() {
        return Clock.systemUTC();
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
        executor.setTaskDecorator(aiStreamingTaskDecorator());
        executor.initialize();
        return executor;
    }

    /**
     * 为 AI 流式线程透传登录态与租户上下文，避免异步线程丢失认证信息。
     *
     * @return 任务装饰器
     */
    @Bean
    public TaskDecorator aiStreamingTaskDecorator() {
        return runnable -> {
            SecurityContext capturedContext = captureSecurityContext();
            String capturedTenantId = TenantContextHolder.getTenantId();
            return () -> {
                SecurityContext previousContext = SecurityContextHolder.getContext();
                String previousTenantId = TenantContextHolder.getTenantId();
                try {
                    SecurityContextHolder.setContext(capturedContext);
                    if (capturedTenantId != null) {
                        TenantContextHolder.setTenantId(capturedTenantId);
                    } else {
                        TenantContextHolder.clear();
                    }
                    runnable.run();
                } finally {
                    if (previousContext != null && previousContext.getAuthentication() != null) {
                        SecurityContextHolder.setContext(previousContext);
                    } else {
                        SecurityContextHolder.clearContext();
                    }
                    if (previousTenantId != null) {
                        TenantContextHolder.setTenantId(previousTenantId);
                    } else {
                        TenantContextHolder.clear();
                    }
                }
            };
        };
    }

    /**
     * 拷贝当前线程的安全上下文，避免请求线程结束后认证对象被清空。
     *
     * @return 安全上下文副本
     */
    private SecurityContext captureSecurityContext() {
        SecurityContext sourceContext = SecurityContextHolder.getContext();
        SecurityContext copiedContext = SecurityContextHolder.createEmptyContext();
        if (sourceContext == null) {
            return copiedContext;
        }
        Authentication authentication = sourceContext.getAuthentication();
        if (authentication != null) {
            copiedContext.setAuthentication(authentication);
        }
        return copiedContext;
    }
}

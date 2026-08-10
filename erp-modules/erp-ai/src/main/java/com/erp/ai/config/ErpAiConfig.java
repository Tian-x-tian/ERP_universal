package com.erp.ai.config;

import com.erp.ai.context.AiInvocationScope;
import com.erp.common.core.context.TenantContextHolder;
import com.erp.common.security.AuthHeaders;
import com.erp.common.security.InternalAuthContextHolder;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.TaskDecorator;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.util.StringUtils;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.time.Clock;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.Executor;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadPoolExecutor;

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
     * <p>一次 SSE 对话会独占一个线程，线程数即并发对话上限，因此改为按配置项设定；
     * 队列打满时直接抛 {@link RejectedExecutionException}，由控制层转成明确的“繁忙”提示，
     * 而不是让请求悄悄挂住。</p>
     *
     * @param erpAiProperties AI 配置属性
     * @return AI 流式执行器
     */
    @Bean(name = "aiStreamingExecutor")
    public Executor aiStreamingExecutor(ErpAiProperties erpAiProperties) {
        int corePoolSize = Math.max(1, erpAiProperties.getStreamCorePoolSize());
        int maxPoolSize = Math.max(corePoolSize, erpAiProperties.getStreamMaxPoolSize());
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(corePoolSize);
        executor.setMaxPoolSize(maxPoolSize);
        executor.setQueueCapacity(Math.max(0, erpAiProperties.getStreamQueueCapacity()));
        executor.setThreadNamePrefix("erp-ai-stream-");
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.AbortPolicy());
        executor.setTaskDecorator(aiStreamingTaskDecorator());
        executor.initialize();
        return executor;
    }

    /**
     * 为 AI 流式线程透传登录态、租户与内部认证头，避免异步线程丢失身份信息。
     *
     * <p>内部客户端默认从当前 Servlet 请求透传 {@code X-Auth-*}；异步线程上没有请求绑定，
     * 如果不做快照，下游会把调用方当成合成服务账号，权限判定随之错位。</p>
     *
     * @return 任务装饰器
     */
    @Bean
    public TaskDecorator aiStreamingTaskDecorator() {
        return runnable -> {
            SecurityContext capturedContext = captureSecurityContext();
            String capturedTenantId = TenantContextHolder.getTenantId();
            Map<String, String> capturedAuthHeaders = captureAuthHeaders();
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
                    InternalAuthContextHolder.set(capturedAuthHeaders);
                    AiInvocationScope.open();
                    runnable.run();
                } finally {
                    AiInvocationScope.close();
                    InternalAuthContextHolder.clear();
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

    /**
     * 从当前请求中抓取可透传的内部认证头快照。
     *
     * @return 认证头快照，无请求绑定时返回空集合
     */
    private Map<String, String> captureAuthHeaders() {
        HttpServletRequest request = currentRequest();
        if (request == null) {
            return Map.of();
        }
        Map<String, String> snapshot = new LinkedHashMap<>();
        for (String headerName : AuthHeaders.INTERNAL_HEADERS) {
            String headerValue = request.getHeader(headerName);
            if (StringUtils.hasText(headerValue)) {
                snapshot.put(headerName, headerValue.trim());
            }
        }
        return snapshot;
    }

    /**
     * 获取当前线程绑定的 Servlet 请求。
     *
     * @return 当前请求，未绑定时返回 null
     */
    private HttpServletRequest currentRequest() {
        RequestAttributes requestAttributes = RequestContextHolder.getRequestAttributes();
        if (requestAttributes instanceof ServletRequestAttributes servletRequestAttributes) {
            return servletRequestAttributes.getRequest();
        }
        return null;
    }
}

package com.erp.gateway.config;

import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

/**
 * Gateway 鉴权相关配置。
 */
@Configuration
public class GatewayAuthConfig {

    /**
     * 注册支持服务发现的 WebClient.Builder。
     *
     * @return WebClient.Builder
     */
    @Bean
    @LoadBalanced
    public WebClient.Builder loadBalancedWebClientBuilder() {
        return WebClient.builder();
    }
}

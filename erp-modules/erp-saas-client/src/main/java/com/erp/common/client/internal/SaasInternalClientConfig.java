package com.erp.common.client.internal;

import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;

/**
 * Dedicated transport configuration for calls to the SaaS control plane.
 */
@Configuration
public class SaasInternalClientConfig {
    @Bean(name = "saasInternalRestTemplate")
    @LoadBalanced
    public RestTemplate saasInternalRestTemplate(RestTemplateBuilder builder,
            InternalSystemClientProperties properties) {
        return builder
                .setConnectTimeout(Duration.ofMillis(properties.resolveSaasConnectTimeoutMs()))
                .setReadTimeout(Duration.ofMillis(properties.resolveSaasReadTimeoutMs()))
                .build();
    }
}

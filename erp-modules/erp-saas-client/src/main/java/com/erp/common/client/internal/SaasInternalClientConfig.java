package com.erp.common.client.internal;

import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.DefaultResponseErrorHandler;
import org.springframework.web.client.RestTemplate;

import java.net.http.HttpClient;
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
        HttpClient httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofMillis(properties.resolveSaasConnectTimeoutMs()))
                .followRedirects(HttpClient.Redirect.NEVER)
                .build();
        JdkClientHttpRequestFactory requestFactory = new JdkClientHttpRequestFactory(httpClient);
        requestFactory.setReadTimeout(Duration.ofMillis(properties.resolveSaasReadTimeoutMs()));
        return builder
                .requestFactory(() -> requestFactory)
                .errorHandler(new DefaultResponseErrorHandler() {
                    @Override
                    protected boolean hasError(HttpStatusCode statusCode) {
                        return !statusCode.is2xxSuccessful();
                    }
                })
                .build();
    }
}

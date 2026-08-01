package com.erp.common.client.internal;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.web.client.RestTemplate;

/**
 * 平台内部调用配置。
 */
@Configuration
@EnableConfigurationProperties(InternalSystemClientProperties.class)
public class InternalSystemClientConfig {

    /**
     * 注册内部调用使用的 RestTemplate。
     *
     * @return RestTemplate
     */
    @Bean
    @LoadBalanced
    @Primary
    public RestTemplate internalSystemRestTemplate() {
        return new RestTemplate();
    }
}

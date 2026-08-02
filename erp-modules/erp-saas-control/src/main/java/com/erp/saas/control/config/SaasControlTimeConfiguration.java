package com.erp.saas.control.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

@Configuration
public class SaasControlTimeConfiguration {
    @Bean
    @ConditionalOnMissingBean(Clock.class)
    Clock saasControlClock() {
        return Clock.systemUTC();
    }
}

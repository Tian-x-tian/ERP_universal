package com.erp.system.saas;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

@Configuration
public class SaasSnapshotTimeConfiguration {
    @Bean
    @ConditionalOnMissingBean(Clock.class)
    Clock saasSnapshotClock() {
        return Clock.systemUTC();
    }
}

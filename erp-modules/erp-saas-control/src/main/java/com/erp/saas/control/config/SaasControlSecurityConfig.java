package com.erp.saas.control.config;

import com.erp.common.security.servlet.InternalApiSecurityConfigurer;
import com.erp.saas.control.security.SaasAuthenticationFilter;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import org.springframework.security.web.util.matcher.OrRequestMatcher;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SaasControlSecurityConfig {
    private final SaasAuthenticationFilter authenticationFilter;

    public SaasControlSecurityConfig(SaasAuthenticationFilter authenticationFilter) {
        this.authenticationFilter = authenticationFilter;
    }

    @Bean
    @Order(2)
    public SecurityFilterChain saasControlFilterChain(HttpSecurity http, ObjectMapper objectMapper) throws Exception {
        return InternalApiSecurityConfigurer.buildFilterChain(
                http,
                new OrRequestMatcher(
                        new AntPathRequestMatcher("/saas/**"),
                        new AntPathRequestMatcher("/internal/saas/**")),
                authenticationFilter,
                objectMapper);
    }
}

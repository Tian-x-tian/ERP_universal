package com.erp.business.config;

import com.erp.business.security.filter.JwtAuthenticationFilter;
import com.erp.common.security.servlet.InternalApiSecurityConfigurer;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Spring Security 配置。
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    public SecurityConfig(JwtAuthenticationFilter jwtAuthenticationFilter) {
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
    }

    /**
     * 配置业务模块的安全过滤链。
     *
     * @param http HttpSecurity
     * @return 过滤链
     * @throws Exception 配置异常
     */
    @Bean
    @Order(2)
    public SecurityFilterChain filterChain(HttpSecurity http, ObjectMapper objectMapper) throws Exception {
        return InternalApiSecurityConfigurer.buildFilterChain(
                http,
                "/business/**",
                jwtAuthenticationFilter,
                objectMapper);
    }

    /**
     * 注册认证管理器。
     *
     * @param authenticationConfiguration 认证配置
     * @return 认证管理器
     * @throws Exception 配置异常
     */
    @Bean
    public org.springframework.security.authentication.AuthenticationManager authenticationManager(
            org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration authenticationConfiguration)
            throws Exception {
        return authenticationConfiguration.getAuthenticationManager();
    }

}

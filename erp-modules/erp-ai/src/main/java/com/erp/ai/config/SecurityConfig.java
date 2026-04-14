package com.erp.ai.config;

import com.erp.ai.security.filter.JwtAuthenticationFilter;
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
 * AI 模块 Spring Security 配置。
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
     * 配置 AI 模块的内部接口认证过滤链。
     *
     * @param http HttpSecurity 配置入口
     * @param objectMapper JSON 工具
     * @return AI 模块安全过滤链
     * @throws Exception 配置异常
     */
    @Bean
    @Order(2)
    public SecurityFilterChain filterChain(HttpSecurity http, ObjectMapper objectMapper) throws Exception {
        return InternalApiSecurityConfigurer.buildFilterChain(
                http,
                "/system/ai/**",
                jwtAuthenticationFilter,
                objectMapper);
    }

    /**
     * 注册认证管理器，供方法级权限与扩展鉴权逻辑复用。
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

package com.erp.system.config;

import com.erp.common.security.servlet.InternalApiSecurityConfigurer;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;
import com.erp.system.security.filter.JwtAuthenticationFilter;

/**
 * Spring Security 配置
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

        private final JwtAuthenticationFilter jwtAuthenticationFilter;

        public SecurityConfig(JwtAuthenticationFilter jwtAuthenticationFilter) {
                this.jwtAuthenticationFilter = jwtAuthenticationFilter;
        }

        @Bean
        public SecurityFilterChain filterChain(HttpSecurity http, ObjectMapper objectMapper) throws Exception {
                return InternalApiSecurityConfigurer.buildFilterChain(
                                http,
                                "/system/**",
                                jwtAuthenticationFilter,
                                objectMapper,
                                "/system/public/tenants/active");
        }

        /**
         * 身份验证管理器
         */
        @Bean
        public org.springframework.security.authentication.AuthenticationManager authenticationManager(
                        org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration authenticationConfiguration)
                        throws Exception {
                return authenticationConfiguration.getAuthenticationManager();
        }

}

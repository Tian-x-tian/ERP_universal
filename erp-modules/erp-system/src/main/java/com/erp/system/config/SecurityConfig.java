package com.erp.system.config;

import com.erp.common.core.domain.R;
import com.erp.common.core.domain.ResultCode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import com.erp.system.security.filter.JwtAuthenticationFilter;

import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;

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
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        return http
                // CSRF 禁用，因为不使用 Session
                .csrf(AbstractHttpConfigurer::disable)
                // 禁用 HTTP 基本认证
                .httpBasic(AbstractHttpConfigurer::disable)
                // 禁用表单登录
                .formLogin(AbstractHttpConfigurer::disable)
                // 禁用默认退出登录，使用自定义 /logout 接口返回统一 JSON
                .logout(AbstractHttpConfigurer::disable)
                // 基于 Token，不需要 Session
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                // 过滤请求
                .authorizeHttpRequests(auth -> auth
                        // 对于登录接口 允许匿名访问
                        .requestMatchers("/login", "/logout", "/doc.html", "/webjars/**", "/v3/api-docs/**",
                                "/swagger-ui/**")
                        .permitAll()
                        // 除上面外的所有请求全部需要鉴权认证
                        .anyRequest().authenticated())
                // 统一异常返回 JSON，便于前端按业务码处理 40101/40301
                .exceptionHandling(exception -> exception
                        .authenticationEntryPoint((request, response, authException) ->
                                writeJson(response, R.failed(ResultCode.UNAUTHORIZED), ResultCode.UNAUTHORIZED.getCode()))
                        .accessDeniedHandler((request, response, accessDeniedException) ->
                                writeJson(response, R.failed(ResultCode.FORBIDDEN), ResultCode.FORBIDDEN.getCode())))
                // 添加 JWT 过滤器
                .addFilterBefore(jwtAuthenticationFilter,
                        org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter.class)
                .build();
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

    /**
     * 强散列哈希加密实现
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /**
     * 统一写出 JSON 响应。
     *
     * @param response 响应对象
     * @param body     响应体
     * @param code     业务码
     * @throws IOException IO 异常
     */
    private void writeJson(HttpServletResponse response, R<?> body, long code) throws IOException {
        response.setCharacterEncoding("UTF-8");
        response.setContentType("application/json; charset=UTF-8");
        response.setStatus(ApiHttpStatusResolver.resolve(code).value());
        ObjectMapper objectMapper = new ObjectMapper();
        response.getWriter().write(objectMapper.writeValueAsString(body));
    }
}

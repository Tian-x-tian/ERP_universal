package com.erp.common.security.servlet;

import com.erp.common.core.domain.ResultCode;
import com.erp.common.web.error.ApiErrorResponseWriter;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.DispatcherType;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.util.matcher.RequestMatcher;
import org.springframework.web.filter.OncePerRequestFilter;

import java.util.Arrays;
import java.util.List;

/**
 * 内部 API 安全过滤链配置工具。
 */
public final class InternalApiSecurityConfigurer {
    private static final List<String> DEFAULT_PUBLIC_PATTERNS = List.of(
            "/doc.html",
            "/webjars/**",
            "/v3/api-docs/**",
            "/swagger-ui/**");

    private InternalApiSecurityConfigurer() {
    }

    /**
     * 构建统一的内部 API 安全过滤链。
     *
     * @param http         HttpSecurity
     * @param matcher      安全匹配路径
     * @param authFilter   鉴权过滤器
     * @param objectMapper JSON 工具
     * @param publicPaths  额外放行路径
     * @return 过滤链
     * @throws Exception 配置异常
     */
    public static SecurityFilterChain buildFilterChain(HttpSecurity http,
            String matcher,
            OncePerRequestFilter authFilter,
            ObjectMapper objectMapper,
            String... publicPaths) throws Exception {
        http.securityMatcher(matcher);
        return configureFilterChain(http, authFilter, objectMapper, publicPaths);
    }

    /**
     * Builds a stateless security chain for a composite request matcher.
     *
     * @param http        security builder
     * @param matcher     protected request matcher
     * @param authFilter  signed-principal authentication filter
     * @param objectMapper JSON response writer
     * @param publicPaths additional public paths inside the protected matcher
     * @return configured security chain
     * @throws Exception configuration failure
     */
    public static SecurityFilterChain buildFilterChain(HttpSecurity http,
            RequestMatcher matcher,
            OncePerRequestFilter authFilter,
            ObjectMapper objectMapper,
            String... publicPaths) throws Exception {
        http.securityMatcher(matcher);
        return configureFilterChain(http, authFilter, objectMapper, publicPaths);
    }

    private static SecurityFilterChain configureFilterChain(HttpSecurity http,
            OncePerRequestFilter authFilter,
            ObjectMapper objectMapper,
            String... publicPaths) throws Exception {
        List<String> publicMatchers = Arrays.stream(publicPaths == null ? new String[0] : publicPaths)
                .filter(path -> path != null && !path.isBlank())
                .toList();
        return http
                .csrf(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable)
                .formLogin(AbstractHttpConfigurer::disable)
                .logout(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> {
                    auth.dispatcherTypeMatchers(DispatcherType.ASYNC, DispatcherType.ERROR).permitAll();
                    auth.requestMatchers(DEFAULT_PUBLIC_PATTERNS.toArray(new String[0])).permitAll();
                    if (!publicMatchers.isEmpty()) {
                        auth.requestMatchers(publicMatchers.toArray(new String[0])).permitAll();
                    }
                    auth.anyRequest().authenticated();
                })
                .exceptionHandling(exception -> exception
                        .authenticationEntryPoint((request, response, authException) -> ApiErrorResponseWriter.writeServlet(
                                request,
                                response,
                                objectMapper,
                                ResultCode.UNAUTHORIZED))
                        .accessDeniedHandler((request, response, accessDeniedException) -> ApiErrorResponseWriter.writeServlet(
                                request,
                                response,
                                objectMapper,
                                ResultCode.FORBIDDEN)))
                .addFilterBefore(authFilter, UsernamePasswordAuthenticationFilter.class)
                .build();
    }
}

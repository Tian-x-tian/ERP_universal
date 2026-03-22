package com.erp.common.security.servlet;

import com.erp.common.core.domain.R;
import com.erp.common.core.domain.ResultCode;
import com.erp.common.web.ApiHttpStatusResolver;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
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
        List<String> publicMatchers = Arrays.stream(publicPaths == null ? new String[0] : publicPaths)
                .filter(path -> path != null && !path.isBlank())
                .toList();
        return http
                .securityMatcher(matcher)
                .csrf(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable)
                .formLogin(AbstractHttpConfigurer::disable)
                .logout(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> {
                    auth.requestMatchers(DEFAULT_PUBLIC_PATTERNS.toArray(new String[0])).permitAll();
                    if (!publicMatchers.isEmpty()) {
                        auth.requestMatchers(publicMatchers.toArray(new String[0])).permitAll();
                    }
                    auth.anyRequest().authenticated();
                })
                .exceptionHandling(exception -> exception
                        .authenticationEntryPoint((request, response, authException) -> writeJson(response,
                                objectMapper,
                                R.failed(ResultCode.UNAUTHORIZED),
                                ResultCode.UNAUTHORIZED.getCode()))
                        .accessDeniedHandler((request, response, accessDeniedException) -> writeJson(response,
                                objectMapper,
                                R.failed(ResultCode.FORBIDDEN),
                                ResultCode.FORBIDDEN.getCode())))
                .addFilterBefore(authFilter, UsernamePasswordAuthenticationFilter.class)
                .build();
    }

    /**
     * 统一写出 JSON 响应。
     *
     * @param response     响应对象
     * @param objectMapper JSON 工具
     * @param body         响应体
     * @param code         业务码
     * @throws IOException IO 异常
     */
    private static void writeJson(HttpServletResponse response,
            ObjectMapper objectMapper,
            R<?> body,
            long code) throws IOException {
        response.setCharacterEncoding("UTF-8");
        response.setContentType("application/json; charset=UTF-8");
        response.setStatus(ApiHttpStatusResolver.resolve(code).value());
        response.getWriter().write(objectMapper.writeValueAsString(body));
    }
}

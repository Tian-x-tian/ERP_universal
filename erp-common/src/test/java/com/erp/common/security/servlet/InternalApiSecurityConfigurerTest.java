package com.erp.common.security.servlet;

import com.erp.common.core.domain.R;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.Test;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.core.Ordered;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.ArrayList;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * {@link InternalApiSecurityConfigurer} 单元测试。
 */
@SpringBootTest(classes = InternalApiSecurityConfigurerTest.TestApplication.class)
@AutoConfigureMockMvc
class InternalApiSecurityConfigurerTest {

    @jakarta.annotation.Resource
    private MockMvc mockMvc;

    @jakarta.annotation.Resource
    private ObjectMapper objectMapper;

    /**
     * 验证匿名访问受保护接口时会返回统一 401 错误体。
     */
    @Test
    void shouldWriteUnifiedUnauthorizedResponse() throws Exception {
        String content = mockMvc.perform(get("/internal/secure")
                        .header("X-Trace-Id", "trace-security-401"))
                .andExpect(status().isUnauthorized())
                .andReturn()
                .getResponse()
                .getContentAsString();

        JsonNode body = objectMapper.readTree(content);
        assertThat(body.path("code").asLong()).isEqualTo(40101L);
        assertThat(body.path("traceId").asText()).isEqualTo("trace-security-401");
        assertThat(body.path("path").asText()).isEqualTo("/internal/secure");
        assertThat(body.path("timestamp").asText()).isNotBlank();
    }

    /**
     * 验证已登录但缺少权限时会返回统一 403 错误体。
     */
    @Test
    void shouldWriteUnifiedForbiddenResponse() throws Exception {
        String content = mockMvc.perform(get("/internal/admin")
                        .header("X-Trace-Id", "trace-security-403")
                        .header("X-Test-Auth", "true"))
                .andExpect(status().isForbidden())
                .andReturn()
                .getResponse()
                .getContentAsString();

        JsonNode body = objectMapper.readTree(content);
        assertThat(body.path("code").asLong()).isEqualTo(40301L);
        assertThat(body.path("traceId").asText()).isEqualTo("trace-security-403");
        assertThat(body.path("path").asText()).isEqualTo("/internal/admin");
        assertThat(body.path("timestamp").asText()).isNotBlank();
    }

    /**
     * 测试应用配置。
     */
    @SpringBootConfiguration
    @EnableAutoConfiguration(exclude = DataSourceAutoConfiguration.class)
    @EnableWebSecurity
    @EnableMethodSecurity
    static class TestApplication {

        @Bean
        FilterRegistrationBean<TestTraceIdFilter> traceIdFilterRegistration() {
            FilterRegistrationBean<TestTraceIdFilter> registration = new FilterRegistrationBean<>();
            registration.setFilter(new TestTraceIdFilter());
            registration.setOrder(Ordered.HIGHEST_PRECEDENCE);
            return registration;
        }

        @Bean
        TestAuthenticationFilter testAuthenticationFilter() {
            return new TestAuthenticationFilter();
        }

        @Bean
        SecurityFilterChain filterChain(HttpSecurity http,
                ObjectMapper objectMapper,
                TestAuthenticationFilter testAuthenticationFilter) throws Exception {
            return InternalApiSecurityConfigurer.buildFilterChain(
                    http,
                    "/internal/**",
                    testAuthenticationFilter,
                    objectMapper);
        }

        @RestController
        static class ProtectedController {

            @GetMapping(value = "/internal/secure", produces = MediaType.APPLICATION_JSON_VALUE)
            public R<String> secure() {
                return R.success("ok");
            }

            @org.springframework.security.access.prepost.PreAuthorize("hasAuthority('ADMIN')")
            @GetMapping(value = "/internal/admin", produces = MediaType.APPLICATION_JSON_VALUE)
            public R<String> admin() {
                return R.success("ok");
            }
        }
    }

    /**
     * 测试用 traceId 过滤器。
     */
    static class TestTraceIdFilter extends com.erp.common.web.filter.TraceIdFilterSupport {
    }

    /**
     * 测试用认证过滤器。
     */
    static class TestAuthenticationFilter extends OncePerRequestFilter {

        @Override
        protected void doFilterInternal(HttpServletRequest request,
                HttpServletResponse response,
                FilterChain filterChain) throws ServletException, IOException {
            if (StringUtils.hasText(request.getHeader("X-Test-Auth"))) {
                UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                        "tester",
                        null,
                        new ArrayList<>());
                SecurityContextHolder.getContext().setAuthentication(authentication);
            }
            try {
                filterChain.doFilter(request, response);
            } finally {
                SecurityContextHolder.clearContext();
            }
        }
    }
}

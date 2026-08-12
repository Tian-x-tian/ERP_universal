package com.erp.common.security.servlet;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * {@link InternalAuthenticationFilterSupport} 单元测试。
 */
class InternalAuthenticationFilterSupportTest {
    private final ObjectMapper objectMapper = new ObjectMapper();

    @AfterEach
    void tearDown() {
        com.erp.common.core.context.RequestTraceContextHolder.clear();
    }

    /**
     * 验证内部鉴权失败时会输出统一的 JSON 错误体。
     */
    @Test
    void shouldWriteUnifiedUnauthorizedBody() throws Exception {
        TestInternalAuthenticationFilter filter = new TestInternalAuthenticationFilter(objectMapper);
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/system/internal/demo");
        request.addHeader("X-Trace-Id", "trace-filter-001");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.writeUnauthorizedDirect(request, response, "Token无效或已过期");

        JsonNode body = objectMapper.readTree(response.getContentAsByteArray());
        assertThat(response.getStatus()).isEqualTo(401);
        assertThat(body.path("code").asLong()).isEqualTo(40101L);
        assertThat(body.path("message").asText()).isEqualTo("Token无效或已过期");
        assertThat(body.path("traceId").asText()).isEqualTo("trace-filter-001");
        assertThat(body.path("path").asText()).isEqualTo("/system/internal/demo");
        assertThat(body.path("timestamp").asText()).isNotBlank();
    }

    /**
     * 内部测试过滤器。
     */
    private static final class TestInternalAuthenticationFilter extends InternalAuthenticationFilterSupport {

        private TestInternalAuthenticationFilter(ObjectMapper objectMapper) {
            super(objectMapper, "internal-test-secret", "/system/internal/", "测试模块");
        }

        private void writeUnauthorizedDirect(HttpServletRequest request,
                MockHttpServletResponse response,
                String message) throws Exception {
            writeUnauthorized(request, response, message);
        }
    }
}

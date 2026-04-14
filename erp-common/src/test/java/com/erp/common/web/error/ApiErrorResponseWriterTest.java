package com.erp.common.web.error;

import com.erp.common.core.context.RequestTraceContextHolder;
import com.erp.common.core.domain.ResultCode;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.server.MockServerWebExchange;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * {@link ApiErrorResponseWriter} 单元测试。
 */
class ApiErrorResponseWriterTest {
    private final ObjectMapper objectMapper = new ObjectMapper();

    @AfterEach
    void tearDown() {
        RequestTraceContextHolder.clear();
    }

    /**
     * 验证 Servlet 写出逻辑会输出完整统一错误体。
     */
    @Test
    void shouldWriteServletJsonErrorBody() throws Exception {
        RequestTraceContextHolder.setContext("trace-servlet-writer-001", "/system/test");
        MockHttpServletResponse response = new MockHttpServletResponse();

        ApiErrorResponseWriter.writeServlet(null, response, objectMapper, ResultCode.UNAUTHORIZED, "未授权");

        JsonNode body = objectMapper.readTree(response.getContentAsByteArray());
        assertThat(response.getStatus()).isEqualTo(401);
        assertThat(body.path("code").asLong()).isEqualTo(ResultCode.UNAUTHORIZED.getCode());
        assertThat(body.path("message").asText()).isEqualTo("未授权");
        assertThat(body.path("traceId").asText()).isEqualTo("trace-servlet-writer-001");
        assertThat(body.path("path").asText()).isEqualTo("/system/test");
        assertThat(body.path("timestamp").asText()).isNotBlank();
    }

    /**
     * 验证 Reactive 写出逻辑会输出完整统一错误体。
     */
    @Test
    void shouldWriteReactiveJsonErrorBody() throws Exception {
        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/workflow/test")
                        .header("X-Trace-Id", "trace-reactive-writer-001")
                        .build());

        ApiErrorResponseWriter.writeReactive(exchange, objectMapper, ResultCode.FORBIDDEN, "无权限").block();

        JsonNode body = objectMapper.readTree(exchange.getResponse().getBodyAsString().block());
        assertThat(exchange.getResponse().getStatusCode().value()).isEqualTo(403);
        assertThat(body.path("code").asLong()).isEqualTo(ResultCode.FORBIDDEN.getCode());
        assertThat(body.path("message").asText()).isEqualTo("无权限");
        assertThat(body.path("traceId").asText()).isEqualTo("trace-reactive-writer-001");
        assertThat(body.path("path").asText()).isEqualTo("/workflow/test");
        assertThat(body.path("timestamp").asText()).isNotBlank();
    }
}

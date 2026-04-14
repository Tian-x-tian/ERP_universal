package com.erp.gateway.config;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.web.server.ResponseStatusException;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 网关全局异常处理器测试。
 */
class GatewayErrorWebExceptionHandlerTest {
    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * 验证已知状态异常会被映射为统一错误体。
     */
    @Test
    void shouldHandleResponseStatusException() throws Exception {
        GatewayErrorWebExceptionHandler handler = new GatewayErrorWebExceptionHandler(objectMapper);
        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/workflow/error")
                        .header("X-Trace-Id", "trace-gateway-status-001")
                        .build());

        handler.handle(exchange, new ResponseStatusException(HttpStatus.NOT_FOUND, "未找到资源")).block();

        JsonNode body = objectMapper.readTree(exchange.getResponse().getBodyAsString().block());
        assertThat(exchange.getResponse().getStatusCode().value()).isEqualTo(404);
        assertThat(body.path("code").asLong()).isEqualTo(40401L);
        assertThat(body.path("traceId").asText()).isEqualTo("trace-gateway-status-001");
        assertThat(body.path("path").asText()).isEqualTo("/workflow/error");
        assertThat(body.path("timestamp").asText()).isNotBlank();
    }

    /**
     * 验证未知异常会被统一降级为系统异常。
     */
    @Test
    void shouldHandleUnknownException() throws Exception {
        GatewayErrorWebExceptionHandler handler = new GatewayErrorWebExceptionHandler(objectMapper);
        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/workflow/error")
                        .build());

        handler.handle(exchange, new IllegalStateException("gateway crashed")).block();

        JsonNode body = objectMapper.readTree(exchange.getResponse().getBodyAsString().block());
        assertThat(exchange.getResponse().getStatusCode().value()).isEqualTo(500);
        assertThat(body.path("code").asLong()).isEqualTo(50001L);
        assertThat(body.path("traceId").asText()).matches("[0-9a-f]{16}");
        assertThat(body.path("path").asText()).isEqualTo("/workflow/error");
        assertThat(body.path("timestamp").asText()).isNotBlank();
    }
}

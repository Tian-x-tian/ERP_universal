package com.erp.gateway.filter;

import com.erp.common.security.AuthHeaders;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.ExchangeFunction;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

class GatewayAuthFilterTest {
    private static final String INTERNAL_SECRET = "gateway-test-secret";

    /**
     * 校验 workflow 路径会先经认证中心校验，再转成内部身份头透传给下游服务。
     */
    @Test
    void shouldRelayAuthenticatedWorkflowRequestAsInternalHeaders() {
        GatewayAuthFilter filter = new GatewayAuthFilter(buildVerifiedWebClientBuilder(), new ObjectMapper(), INTERNAL_SECRET);
        MockServerHttpRequest request = MockServerHttpRequest.get("/workflow/todos/list")
                .header(HttpHeaders.AUTHORIZATION, "Bearer mock-token")
                .header("tenantId", "DEFAULT")
                .header(AuthHeaders.USER_ID, "999")
                .build();
        MockServerWebExchange exchange = MockServerWebExchange.from(request);
        AtomicReference<ServerWebExchange> forwardedExchange = new AtomicReference<>();
        GatewayFilterChain chain = forwarded -> {
            forwardedExchange.set(forwarded);
            return Mono.empty();
        };

        filter.filter(exchange, chain).block();

        assertThat(forwardedExchange.get()).as("workflow 请求应继续透传给下游").isNotNull();
        HttpHeaders headers = forwardedExchange.get().getRequest().getHeaders();
        assertThat(headers.getFirst(AuthHeaders.USER_ID)).isEqualTo("1");
        assertThat(headers.getFirst(AuthHeaders.USER_NAME)).isEqualTo("workflow-user");
        assertThat(headers.getFirst(AuthHeaders.TENANT_ID)).isEqualTo("DEFAULT");
        assertThat(headers.getFirst(AuthHeaders.TOKEN_VERSION)).isEqualTo("2");
        assertThat(headers.getFirst(AuthHeaders.EXPIRES_AT)).isEqualTo("1893456000000");
        assertThat(headers.getFirst(AuthHeaders.SIGNATURE)).isNotBlank();
    }

    /**
     * 校验认证中心校验失败时会返回统一未授权错误体，而不是手写精简 JSON。
     */
    @Test
    void shouldWriteUnifiedUnauthorizedBodyWhenTokenVerificationFails() throws Exception {
        GatewayAuthFilter filter = new GatewayAuthFilter(buildRejectedWebClientBuilder(), new ObjectMapper(), INTERNAL_SECRET);
        MockServerHttpRequest request = MockServerHttpRequest.get("/workflow/todos/list")
                .header(HttpHeaders.AUTHORIZATION, "Bearer bad-token")
                .header("tenantId", "DEFAULT")
                .header("X-Trace-Id", "trace-gateway-auth-001")
                .build();
        MockServerWebExchange exchange = MockServerWebExchange.from(request);
        GatewayFilterChain chain = forwarded -> Mono.empty();

        filter.filter(exchange, chain).block();

        String content = exchange.getResponse().getBodyAsString().block();
        com.fasterxml.jackson.databind.JsonNode body = new ObjectMapper().readTree(content);
        assertThat(exchange.getResponse().getStatusCode().value()).isEqualTo(401);
        assertThat(body.path("code").asLong()).isEqualTo(40101L);
        assertThat(body.path("message").asText()).isEqualTo("Token无效或已过期");
        assertThat(body.path("traceId").asText()).isEqualTo("trace-gateway-auth-001");
        assertThat(body.path("path").asText()).isEqualTo("/workflow/todos/list");
        assertThat(body.path("timestamp").asText()).isNotBlank();
    }

    /**
     * 构造一个返回固定认证成功结果的 WebClient.Builder，模拟认证中心验签成功。
     *
     * @return 可用于 GatewayAuthFilter 的 WebClient.Builder
     */
    private WebClient.Builder buildVerifiedWebClientBuilder() {
        ExchangeFunction exchangeFunction = request -> Mono.just(ClientResponse.create(HttpStatus.OK)
                .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .body("""
                        {
                          "code": 0,
                          "message": "success",
                          "data": {
                            "userId": 1,
                            "userName": "workflow-user",
                            "tenantId": "DEFAULT",
                            "tokenVersion": 2,
                            "expiresAt": 1893456000000
                          }
                        }
                        """)
                .build());
        return WebClient.builder().exchangeFunction(exchangeFunction);
    }

    /**
     * 构造一个返回认证失败结果的 WebClient.Builder，模拟认证中心验签失败。
     *
     * @return 可用于 GatewayAuthFilter 的 WebClient.Builder
     */
    private WebClient.Builder buildRejectedWebClientBuilder() {
        ExchangeFunction exchangeFunction = request -> Mono.just(ClientResponse.create(HttpStatus.OK)
                .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .body("""
                        {
                          "code": 40101,
                          "message": "Token无效或已过期",
                          "data": null
                        }
                        """)
                .build());
        return WebClient.builder().exchangeFunction(exchangeFunction);
    }
}

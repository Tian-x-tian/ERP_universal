package com.erp.gateway.filter;

import com.erp.common.security.AuthHeaders;
import com.erp.common.security.ResolvedTenantAssertion;
import com.erp.common.security.ResolvedTenantAssertionSignatureUtils;
import com.erp.common.security.TenantAssertionHeaders;
import com.erp.saas.contract.model.DeploymentMode;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.web.reactive.function.client.ClientRequest;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.ExchangeFunction;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

class GatewayAuthFilterTest {
    private static final String INTERNAL_SECRET = "gateway-test-secret";
    private static final String ASSERTION_SECRET = "gateway-tenant-assertion-secret-32-bytes";
    private static final Instant NOW = Instant.parse("2026-08-02T03:00:00Z");
    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * 校验内部契约路径不允许从公网入口进入。
     *
     * <p>这些端点接受显式 userId 入参且不做归属校验，其安全前提是「只有服务能调到」。
     * 而网关对受保护路径签发的是调用者本人的身份头，下游只校验签名有效性、不校验主体是不是服务账号，
     * 因此一旦放行，任何已登录用户都能传别人的 userId 读写他人数据。</p>
     */
    @Test
    void shouldRejectInternalContractPathsFromEdge() {
        String[] internalPaths = {
                "/system/internal/ai/brief",
                "/system/internal/ai/sessions",
                "/system/internal/platform/oper-log",
                "/workflow/internal/tasks/todo/pending",
                "/business/internal/inventory/warehouses/1/reference-usage",
                "/saas/internal/tenants",
        };

        for (String internalPath : internalPaths) {
            GatewayAuthFilter filter = filter(builder(new ArrayList<>(), TenantResponse.ACTIVE, "tenant-a"));
            MockServerHttpRequest request = MockServerHttpRequest.get(internalPath)
                    .header(HttpHeaders.HOST, "Acme.Example:443")
                    .header(HttpHeaders.AUTHORIZATION, "Bearer mock-token")
                    .build();
            MockServerWebExchange exchange = MockServerWebExchange.from(request);
            AtomicReference<org.springframework.web.server.ServerWebExchange> forwarded = new AtomicReference<>();
            GatewayFilterChain chain = forwardedExchange -> {
                forwarded.set(forwardedExchange);
                return Mono.empty();
            };

            filter.filter(exchange, chain).block();

            assertThat(forwarded.get())
                    .as("内部路径 %s 不应被转发给下游", internalPath)
                    .isNull();
            assertThat(exchange.getResponse().getStatusCode())
                    .as("内部路径 %s 应被拒绝", internalPath)
                    .isEqualTo(HttpStatus.FORBIDDEN);
        }
    }

    @Test
    void shouldResolveHostRemoveClientTenantHeadersAndRelayMatchingToken() {
        List<ClientRequest> calls = new ArrayList<>();
        GatewayAuthFilter filter = filter(builder(calls, TenantResponse.ACTIVE, "tenant-a"));
        MockServerHttpRequest request = MockServerHttpRequest.get("/workflow/todos/list")
                .header(HttpHeaders.HOST, "Acme.Example:443")
                .header(HttpHeaders.AUTHORIZATION, "Bearer mock-token")
                .header("tenantId", "forged")
                .header(TenantAssertionHeaders.TENANT_ID, "forged")
                .build();
        AtomicReference<org.springframework.web.server.ServerWebExchange> forwarded = new AtomicReference<>();
        GatewayFilterChain chain = exchange -> {
            forwarded.set(exchange);
            return Mono.empty();
        };

        filter.filter(MockServerWebExchange.from(request), chain).block();

        assertThat(forwarded.get()).isNotNull();
        HttpHeaders headers = forwarded.get().getRequest().getHeaders();
        assertThat(headers.getFirst("tenantId")).isNull();
        assertThat(headers.getFirst(AuthHeaders.TENANT_ID)).isEqualTo("tenant-a");
        ResolvedTenantAssertion assertion = assertion(headers);
        assertThat(assertion.getHost()).isEqualTo("acme.example");
        assertThat(assertion.getTenantId()).isEqualTo("tenant-a");
        assertThat(ResolvedTenantAssertionSignatureUtils.verify(ASSERTION_SECRET, assertion,
                headers.getFirst(TenantAssertionHeaders.SIGNATURE), NOW.toEpochMilli())).isTrue();
        assertThat(calls).hasSize(2);
        assertThat(calls.get(0).url().getHost()).isEqualTo("erp-saas-control");
        assertThat(calls.get(0).headers().getFirst(AuthHeaders.SIGNATURE)).isNotBlank();
        assertThat(calls.get(1).headers().getFirst("tenantId")).isEqualTo("tenant-a");
    }

    @Test
    void shouldAttachSignedTenantAssertionToLoginWithoutTokenVerification() {
        List<ClientRequest> calls = new ArrayList<>();
        GatewayAuthFilter filter = filter(builder(calls, TenantResponse.ACTIVE, "tenant-a"));
        MockServerHttpRequest request = MockServerHttpRequest.post("/login")
                .header(HttpHeaders.HOST, "acme.example")
                .header("Tenantid", "forged")
                .build();
        AtomicReference<org.springframework.web.server.ServerWebExchange> forwarded = new AtomicReference<>();

        filter.filter(MockServerWebExchange.from(request), exchange -> {
            forwarded.set(exchange);
            return Mono.empty();
        }).block();

        assertThat(forwarded.get()).isNotNull();
        assertThat(calls).hasSize(1);
        HttpHeaders headers = forwarded.get().getRequest().getHeaders();
        assertThat(headers.getFirst("tenantId")).isNull();
        assertThat(assertion(headers).getPath()).isEqualTo("/login");
    }

    @Test
    void shouldAttachSignedTenantAssertionToActivationWithoutTokenVerification() {
        List<ClientRequest> calls = new ArrayList<>();
        GatewayAuthFilter filter = filter(builder(calls, TenantResponse.ACTIVE, "tenant-a"));
        MockServerHttpRequest request = MockServerHttpRequest.post("/system/public/saas/activation")
                .header(HttpHeaders.HOST, "acme.example")
                .build();
        AtomicReference<org.springframework.web.server.ServerWebExchange> forwarded = new AtomicReference<>();

        filter.filter(MockServerWebExchange.from(request), exchange -> {
            forwarded.set(exchange);
            return Mono.empty();
        }).block();

        assertThat(forwarded.get()).isNotNull();
        assertThat(calls).hasSize(1);
        assertThat(assertion(forwarded.get().getRequest().getHeaders()).getPath())
                .isEqualTo("/system/public/saas/activation");
    }

    @Test
    void shouldRejectTokenTenantDifferentFromResolvedDomain() throws Exception {
        GatewayAuthFilter filter = filter(builder(new ArrayList<>(), TenantResponse.ACTIVE, "tenant-b"));
        MockServerWebExchange exchange = MockServerWebExchange.from(MockServerHttpRequest
                .get("/business/orders")
                .header(HttpHeaders.HOST, "acme.example")
                .header(HttpHeaders.AUTHORIZATION, "Bearer wrong-tenant-token")
                .build());

        filter.filter(exchange, ignored -> Mono.empty()).block();

        JsonNode body = objectMapper.readTree(exchange.getResponse().getBodyAsString().block());
        assertThat(exchange.getResponse().getStatusCode().value()).isEqualTo(401);
        assertThat(body.path("message").asText()).isEqualTo("租户与访问域名不匹配");
    }

    @Test
    void shouldProtectPlatformSaasRoutesWithResolvedManagementDomainAndToken() {
        List<ClientRequest> calls = new ArrayList<>();
        GatewayAuthFilter filter = filter(builder(calls, TenantResponse.PLATFORM, "000000"));
        MockServerHttpRequest request = MockServerHttpRequest.post("/saas/tenants")
                .header(HttpHeaders.HOST, "platform.example")
                .header(HttpHeaders.AUTHORIZATION, "Bearer platform-token")
                .build();
        AtomicReference<org.springframework.web.server.ServerWebExchange> forwarded = new AtomicReference<>();

        filter.filter(MockServerWebExchange.from(request), exchange -> {
            forwarded.set(exchange);
            return Mono.empty();
        }).block();

        assertThat(forwarded.get()).isNotNull();
        assertThat(forwarded.get().getRequest().getHeaders().getFirst(AuthHeaders.TENANT_ID))
                .isEqualTo("000000");
        assertThat(calls).hasSize(2);
    }

    @Test
    void shouldRejectUnknownOrUnverifiedDomain() throws Exception {
        GatewayAuthFilter filter = filter(builder(new ArrayList<>(), TenantResponse.NOT_FOUND, null));
        MockServerWebExchange exchange = MockServerWebExchange.from(MockServerHttpRequest
                .post("/login").header(HttpHeaders.HOST, "unknown.example").build());

        filter.filter(exchange, ignored -> Mono.empty()).block();

        JsonNode body = objectMapper.readTree(exchange.getResponse().getBodyAsString().block());
        assertThat(exchange.getResponse().getStatusCode().value()).isEqualTo(401);
        assertThat(body.path("message").asText()).isEqualTo("租户域名无效或未验证");
    }

    @Test
    void shouldRejectSuspendedTenantLogin() throws Exception {
        GatewayAuthFilter filter = filter(builder(new ArrayList<>(), TenantResponse.SUSPENDED, null));
        MockServerWebExchange exchange = MockServerWebExchange.from(MockServerHttpRequest
                .post("/login").header(HttpHeaders.HOST, "acme.example").build());

        filter.filter(exchange, ignored -> Mono.empty()).block();

        JsonNode body = objectMapper.readTree(exchange.getResponse().getBodyAsString().block());
        assertThat(exchange.getResponse().getStatusCode().value()).isEqualTo(401);
        assertThat(body.path("message").asText()).isEqualTo("租户当前状态不允许访问");
    }

    @Test
    void shouldRejectBusinessWritesWhenTenantIsReadOnly() throws Exception {
        List<ClientRequest> calls = new ArrayList<>();
        GatewayAuthFilter filter = filter(builder(calls, TenantResponse.READ_ONLY, "tenant-a"));
        MockServerWebExchange exchange = MockServerWebExchange.from(MockServerHttpRequest
                .post("/business/orders")
                .header(HttpHeaders.HOST, "acme.example")
                .header(HttpHeaders.AUTHORIZATION, "Bearer readonly-token")
                .build());
        AtomicReference<org.springframework.web.server.ServerWebExchange> forwarded = new AtomicReference<>();

        filter.filter(exchange, request -> {
            forwarded.set(request);
            return Mono.empty();
        }).block();

        JsonNode body = objectMapper.readTree(exchange.getResponse().getBodyAsString().block());
        assertThat(exchange.getResponse().getStatusCode().value()).isEqualTo(403);
        assertThat(body.path("message").asText()).isEqualTo("租户当前处于只读状态，禁止业务写入");
        assertThat(forwarded.get()).isNull();
        assertThat(calls).hasSize(1);
    }

    @Test
    void shouldAllowQueriesAndExportsWhenTenantIsReadOnly() {
        List<ClientRequest> calls = new ArrayList<>();
        GatewayAuthFilter filter = filter(builder(calls, TenantResponse.READ_ONLY, "tenant-a"));
        AtomicInteger forwarded = new AtomicInteger();
        GatewayFilterChain chain = request -> {
            forwarded.incrementAndGet();
            return Mono.empty();
        };

        filter.filter(MockServerWebExchange.from(MockServerHttpRequest
                .get("/business/orders")
                .header(HttpHeaders.HOST, "acme.example")
                .header(HttpHeaders.AUTHORIZATION, "Bearer readonly-token")
                .build()), chain).block();
        filter.filter(MockServerWebExchange.from(MockServerHttpRequest
                .post("/business/inventory/report/summary/export")
                .header(HttpHeaders.HOST, "acme.example")
                .header(HttpHeaders.AUTHORIZATION, "Bearer readonly-token")
                .build()), chain).block();

        assertThat(forwarded).hasValue(2);
        assertThat(calls).hasSize(4);
    }

    @Test
    void shouldUseLastVerifiedHostResolutionWhenControlPlaneIsTemporarilyUnavailable() {
        List<ClientRequest> calls = new ArrayList<>();
        AtomicInteger resolutionAttempts = new AtomicInteger();
        ExchangeFunction exchangeFunction = request -> {
            calls.add(request);
            if ("erp-saas-control".equals(request.url().getHost())) {
                if (resolutionAttempts.incrementAndGet() == 1) {
                    return Mono.just(TenantResponse.ACTIVE.response());
                }
                return Mono.error(new IllegalStateException("control plane unavailable"));
            }
            return Mono.just(tokenResponse("tenant-a"));
        };
        GatewayAuthFilter filter = filter(WebClient.builder().exchangeFunction(exchangeFunction));
        AtomicInteger forwarded = new AtomicInteger();
        GatewayFilterChain chain = request -> {
            forwarded.incrementAndGet();
            return Mono.empty();
        };

        for (int index = 0; index < 2; index++) {
            filter.filter(MockServerWebExchange.from(MockServerHttpRequest
                    .get("/business/orders")
                    .header(HttpHeaders.HOST, "acme.example")
                    .header(HttpHeaders.AUTHORIZATION, "Bearer active-token")
                    .build()), chain).block();
        }

        assertThat(forwarded).hasValue(2);
        assertThat(resolutionAttempts).hasValue(2);
        assertThat(calls).hasSize(4);
    }

    @Test
    void shouldNotUseCachedResolutionWhenControlPlaneExplicitlyRejectsDomain() throws Exception {
        AtomicInteger resolutionAttempts = new AtomicInteger();
        ExchangeFunction exchangeFunction = request -> {
            if ("erp-saas-control".equals(request.url().getHost())) {
                return Mono.just(resolutionAttempts.incrementAndGet() == 1
                        ? TenantResponse.ACTIVE.response() : TenantResponse.NOT_FOUND.response());
            }
            return Mono.just(tokenResponse("tenant-a"));
        };
        GatewayAuthFilter filter = filter(WebClient.builder().exchangeFunction(exchangeFunction));
        AtomicInteger forwarded = new AtomicInteger();
        GatewayFilterChain chain = request -> {
            forwarded.incrementAndGet();
            return Mono.empty();
        };
        MockServerHttpRequest request = MockServerHttpRequest.get("/business/orders")
                .header(HttpHeaders.HOST, "acme.example")
                .header(HttpHeaders.AUTHORIZATION, "Bearer active-token")
                .build();

        filter.filter(MockServerWebExchange.from(request), chain).block();
        MockServerWebExchange rejectedExchange = MockServerWebExchange.from(request);
        filter.filter(rejectedExchange, chain).block();

        JsonNode body = objectMapper.readTree(rejectedExchange.getResponse().getBodyAsString().block());
        assertThat(forwarded).hasValue(1);
        assertThat(rejectedExchange.getResponse().getStatusCode().value()).isEqualTo(401);
        assertThat(body.path("message").asText()).isEqualTo("租户域名无效或未验证");
    }

    @Test
    void shouldRejectResolutionOutsideDedicatedInstanceBinding() throws Exception {
        GatewayAuthFilter filter = new GatewayAuthFilter(
                builder(new ArrayList<>(), TenantResponse.DEDICATED_OTHER, "tenant-b"),
                objectMapper, INTERNAL_SECRET, ASSERTION_SECRET, "http://erp-saas-control",
                2000L, Duration.ofHours(24).toMillis(), Clock.fixed(NOW, ZoneOffset.UTC),
                DeploymentMode.DEDICATED, "tenant-a", "acme.example");
        MockServerWebExchange exchange = MockServerWebExchange.from(MockServerHttpRequest
                .get("/business/orders")
                .header(HttpHeaders.HOST, "other.example")
                .header(HttpHeaders.AUTHORIZATION, "Bearer other-token")
                .build());

        filter.filter(exchange, ignored -> Mono.empty()).block();

        JsonNode body = objectMapper.readTree(exchange.getResponse().getBodyAsString().block());
        assertThat(exchange.getResponse().getStatusCode().value()).isEqualTo(401);
        assertThat(body.path("message").asText()).isEqualTo("租户域名不属于当前独立部署实例");
    }

    private GatewayAuthFilter filter(WebClient.Builder builder) {
        return new GatewayAuthFilter(builder, objectMapper, INTERNAL_SECRET, ASSERTION_SECRET,
                "http://erp-saas-control", 2000L, Clock.fixed(NOW, ZoneOffset.UTC));
    }

    private WebClient.Builder builder(List<ClientRequest> calls, TenantResponse tenantResponse,
            String tokenTenant) {
        ExchangeFunction exchangeFunction = request -> {
            calls.add(request);
            if ("erp-saas-control".equals(request.url().getHost())) {
                return Mono.just(tenantResponse.response());
            }
            return Mono.just(tokenResponse(tokenTenant));
        };
        return WebClient.builder().exchangeFunction(exchangeFunction);
    }

    private ClientResponse tokenResponse(String tenantId) {
        return ClientResponse.create(HttpStatus.OK)
                .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .body("{\"code\":0,\"message\":\"success\",\"data\":{"
                        + "\"userId\":1,\"userName\":\"workflow-user\",\"tenantId\":\"" + tenantId
                        + "\",\"tokenVersion\":2,\"expiresAt\":1893456000000}}")
                .build();
    }

    private ResolvedTenantAssertion assertion(HttpHeaders headers) {
        return new ResolvedTenantAssertion(
                headers.getFirst(TenantAssertionHeaders.TENANT_ID),
                headers.getFirst(TenantAssertionHeaders.HOST),
                headers.getFirst(TenantAssertionHeaders.METHOD),
                headers.getFirst(TenantAssertionHeaders.PATH),
                Long.parseLong(headers.getFirst(TenantAssertionHeaders.ISSUED_AT)),
                headers.getFirst(TenantAssertionHeaders.NONCE));
    }

    private enum TenantResponse {
        ACTIVE(HttpStatus.OK, "{\"host\":\"acme.example\",\"tenantId\":\"tenant-a\","
                + "\"deploymentMode\":\"SHARED\",\"lifecycleState\":\"ACTIVE\",\"verified\":true}"),
        PLATFORM(HttpStatus.OK, "{\"host\":\"platform.example\",\"tenantId\":\"000000\","
                + "\"deploymentMode\":\"SHARED\",\"lifecycleState\":\"ACTIVE\",\"verified\":true}"),
        READ_ONLY(HttpStatus.OK, "{\"host\":\"acme.example\",\"tenantId\":\"tenant-a\","
                + "\"deploymentMode\":\"SHARED\",\"lifecycleState\":\"READ_ONLY\",\"verified\":true}"),
        DEDICATED_OTHER(HttpStatus.OK, "{\"host\":\"other.example\",\"tenantId\":\"tenant-b\","
                + "\"deploymentMode\":\"DEDICATED\",\"lifecycleState\":\"ACTIVE\",\"verified\":true}"),
        SUSPENDED(HttpStatus.OK, "{\"host\":\"acme.example\",\"tenantId\":\"tenant-a\","
                + "\"deploymentMode\":\"SHARED\",\"lifecycleState\":\"SUSPENDED\",\"verified\":true}"),
        NOT_FOUND(HttpStatus.NOT_FOUND, "");

        private final HttpStatus status;
        private final String body;

        TenantResponse(HttpStatus status, String body) {
            this.status = status;
            this.body = body;
        }

        ClientResponse response() {
            ClientResponse.Builder response = ClientResponse.create(status);
            if (!body.isEmpty()) {
                response.header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE).body(body);
            }
            return response.build();
        }
    }
}

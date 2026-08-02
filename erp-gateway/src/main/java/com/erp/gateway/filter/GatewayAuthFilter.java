package com.erp.gateway.filter;

import com.erp.common.core.domain.ResultCode;
import com.erp.common.security.AuthHeaders;
import com.erp.common.security.AuthenticatedUserPrincipal;
import com.erp.common.security.InternalAuthSignatureUtils;
import com.erp.common.security.ResolvedTenantAssertion;
import com.erp.common.security.ResolvedTenantAssertionSignatureUtils;
import com.erp.common.security.TenantAssertionHeaders;
import com.erp.common.web.error.ApiErrorResponseWriter;
import com.erp.saas.contract.model.SaasHostResolution;
import com.erp.saas.contract.model.TenantLifecycleState;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpHeaders;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.util.StringUtils;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.util.UriComponentsBuilder;
import reactor.core.publisher.Mono;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Duration;
import java.util.EnumSet;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.regex.Pattern;

/**
 * Resolves tenant identity from the request host and relays only signed internal context.
 */
@Component
public class GatewayAuthFilter implements GlobalFilter, Ordered {
    private static final List<String> PROTECTED_PATTERNS =
            List.of("/system/**", "/workflow/**", "/business/**");
    private static final List<String> TENANT_PUBLIC_PATTERNS = List.of("/login", "/logout", "/auth/**");
    private static final List<String> GLOBAL_PUBLIC_PATTERNS = List.of(
            "/system/public/tenants/active",
            "/doc.html",
            "/webjars/**",
            "/v3/api-docs/**",
            "/swagger-ui/**");
    private static final EnumSet<TenantLifecycleState> ACCESS_ALLOWED = EnumSet.of(
            TenantLifecycleState.TRIAL, TenantLifecycleState.ACTIVE,
            TenantLifecycleState.GRACE, TenantLifecycleState.READ_ONLY);
    private static final Pattern TENANT_ID = Pattern.compile("[A-Za-z0-9_-]{1,20}");
    private static final long SERVICE_EXPIRES_AT = 4_102_444_800_000L;

    private final WebClient.Builder webClientBuilder;
    private final ObjectMapper objectMapper;
    private final AntPathMatcher pathMatcher = new AntPathMatcher();
    private final String internalSignatureSecret;
    private final String tenantAssertionSecret;
    private final String saasControlBaseUrl;
    private final Duration domainResolveTimeout;
    private final Clock clock;

    public GatewayAuthFilter(WebClient.Builder webClientBuilder,
            ObjectMapper objectMapper,
            @Value("${erp.internal.auth-signature-secret:}") String internalSignatureSecret,
            @Value("${erp.saas.tenant-assertion-signature-secret:}") String tenantAssertionSecret,
            @Value("${erp.internal.saas-base-url:http://erp-saas-control}") String saasControlBaseUrl,
            @Value("${erp.saas.domain-resolve-timeout-ms:2000}") long domainResolveTimeoutMs) {
        this(webClientBuilder, objectMapper, internalSignatureSecret, tenantAssertionSecret,
                saasControlBaseUrl, domainResolveTimeoutMs, Clock.systemUTC());
    }

    GatewayAuthFilter(WebClient.Builder webClientBuilder, ObjectMapper objectMapper,
            String internalSignatureSecret, String tenantAssertionSecret,
            String saasControlBaseUrl, long domainResolveTimeoutMs, Clock clock) {
        this.webClientBuilder = Objects.requireNonNull(webClientBuilder, "webClientBuilder must not be null");
        this.objectMapper = Objects.requireNonNull(objectMapper, "objectMapper must not be null");
        this.internalSignatureSecret = requiredSecret(internalSignatureSecret,
                "erp.internal.auth-signature-secret", 1);
        this.tenantAssertionSecret = requiredSecret(tenantAssertionSecret,
                "erp.saas.tenant-assertion-signature-secret", 32);
        this.saasControlBaseUrl = validateBaseUrl(saasControlBaseUrl);
        if (domainResolveTimeoutMs <= 0 || domainResolveTimeoutMs > 30_000L) {
            throw new IllegalStateException("erp.saas.domain-resolve-timeout-ms must be between 1 and 30000");
        }
        this.domainResolveTimeout = Duration.ofMillis(domainResolveTimeoutMs);
        this.clock = Objects.requireNonNull(clock, "clock must not be null");
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String requestPath = exchange.getRequest().getURI().getPath();
        ServerHttpRequest sanitizedRequest = sanitize(exchange.getRequest());
        ServerWebExchange sanitizedExchange = exchange.mutate().request(sanitizedRequest).build();

        if (matchesAny(requestPath, GLOBAL_PUBLIC_PATTERNS) || !isTenantScoped(requestPath)) {
            return chain.filter(sanitizedExchange);
        }

        return resolveTenant(sanitizedRequest)
                .flatMap(resolution -> forwardResolved(sanitizedExchange, sanitizedRequest,
                        resolution, requestPath, chain))
                .onErrorResume(error -> writeUnauthorized(sanitizedExchange, safeMessage(error)));
    }

    @Override
    public int getOrder() {
        return -100;
    }

    private Mono<Void> forwardResolved(ServerWebExchange exchange, ServerHttpRequest request,
            SaasHostResolution resolution, String requestPath, GatewayFilterChain chain) {
        ResolvedTenantAssertion assertion = tenantAssertion(request, resolution);
        ServerHttpRequest assertedRequest = request.mutate()
                .headers(headers -> writeAssertionHeaders(headers, assertion))
                .build();
        ServerWebExchange assertedExchange = exchange.mutate().request(assertedRequest).build();
        if (matchesAny(requestPath, TENANT_PUBLIC_PATTERNS)) {
            return chain.filter(assertedExchange);
        }
        String authorization = assertedRequest.getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
        return verifyToken(authorization, assertion.getTenantId())
                .flatMap(principal -> {
                    if (!assertion.getTenantId().equals(principal.getTenantId())) {
                        return Mono.error(accessDenied("租户与访问域名不匹配"));
                    }
                    String signature = InternalAuthSignatureUtils.sign(internalSignatureSecret, principal);
                    ServerHttpRequest relayRequest = assertedRequest.mutate()
                            .headers(headers -> writePrincipalHeaders(headers, principal, signature))
                            .build();
                    return chain.filter(assertedExchange.mutate().request(relayRequest).build());
                });
    }

    private Mono<SaasHostResolution> resolveTenant(ServerHttpRequest request) {
        String host = request.getHeaders().getFirst(HttpHeaders.HOST);
        if (!StringUtils.hasText(host) || host.trim().length() > 300) {
            return Mono.error(accessDenied("租户域名无效或未验证"));
        }
        URI uri = UriComponentsBuilder.fromHttpUrl(saasControlBaseUrl)
                .path("/internal/saas/hosts/resolve")
                .queryParam("host", "{host}")
                .encode()
                .buildAndExpand(host.trim())
                .toUri();
        return webClientBuilder.build().get().uri(uri)
                .headers(this::writeServicePrincipalHeaders)
                .retrieve()
                .bodyToMono(SaasHostResolution.class)
                .timeout(domainResolveTimeout)
                .switchIfEmpty(Mono.error(accessDenied("租户域名无效或未验证")))
                .map(resolution -> validateResolution(host, request, resolution))
                .onErrorMap(error -> error instanceof GatewayAccessException
                        ? error : accessDenied("租户域名无效或未验证"));
    }

    private SaasHostResolution validateResolution(String requestHost, ServerHttpRequest request,
            SaasHostResolution resolution) {
        if (resolution == null || !resolution.isVerified()
                || resolution.getDeploymentMode() == null || resolution.getLifecycleState() == null
                || !StringUtils.hasText(resolution.getTenantId())
                || !TENANT_ID.matcher(resolution.getTenantId().trim()).matches()
                || !StringUtils.hasText(resolution.getHost())) {
            throw accessDenied("租户域名无效或未验证");
        }
        if (!ACCESS_ALLOWED.contains(resolution.getLifecycleState())) {
            throw accessDenied("租户当前状态不允许访问");
        }
        long now = clock.millis();
        String method = request.getMethod() == null ? "GET" : request.getMethod().name();
        String path = rawPath(request);
        ResolvedTenantAssertion requested = new ResolvedTenantAssertion(
                resolution.getTenantId(), requestHost, method, path, now, "host-check");
        ResolvedTenantAssertion returned = new ResolvedTenantAssertion(
                resolution.getTenantId(), resolution.getHost(), method, path, now, "host-check");
        if (!requested.getHost().equals(returned.getHost())) {
            throw accessDenied("租户域名无效或未验证");
        }
        return resolution;
    }

    private ResolvedTenantAssertion tenantAssertion(ServerHttpRequest request, SaasHostResolution resolution) {
        String method = request.getMethod() == null ? "GET" : request.getMethod().name();
        return new ResolvedTenantAssertion(resolution.getTenantId(),
                request.getHeaders().getFirst(HttpHeaders.HOST), method, rawPath(request),
                clock.millis(), UUID.randomUUID().toString());
    }

    private Mono<AuthenticatedUserPrincipal> verifyToken(String authorization, String tenantId) {
        if (!StringUtils.hasText(authorization)) {
            return Mono.error(accessDenied("Token无效或已过期"));
        }
        return webClientBuilder.build()
                .get()
                .uri("http://erp-auth/auth/token/verify")
                .headers(headers -> {
                    headers.set(HttpHeaders.AUTHORIZATION, authorization);
                    headers.set("tenantId", tenantId);
                })
                .retrieve()
                .bodyToMono(JsonNode.class)
                .map(this::toVerifiedPrincipal)
                .onErrorMap(error -> error instanceof GatewayAccessException
                        ? error : accessDenied("Token无效或已过期"));
    }

    private AuthenticatedUserPrincipal toVerifiedPrincipal(JsonNode rootNode) {
        int code = rootNode.path("code").asInt((int) ResultCode.UNAUTHORIZED.getCode());
        if (code != (int) ResultCode.SUCCESS.getCode()) {
            throw accessDenied(rootNode.path("message").asText("Token无效或已过期"));
        }
        JsonNode dataNode = rootNode.path("data");
        return new AuthenticatedUserPrincipal(
                dataNode.path("userId").asLong(),
                dataNode.path("userName").asText(null),
                dataNode.path("tenantId").asText(null),
                dataNode.path("tokenVersion").asInt(),
                dataNode.path("expiresAt").asLong());
    }

    private ServerHttpRequest sanitize(ServerHttpRequest request) {
        return request.mutate().headers(headers -> {
            AuthHeaders.INTERNAL_HEADERS.forEach(headers::remove);
            TenantAssertionHeaders.INTERNAL_HEADERS.forEach(headers::remove);
            headers.remove("tenantId");
            headers.remove("Tenantid");
        }).build();
    }

    private void writeAssertionHeaders(HttpHeaders headers, ResolvedTenantAssertion assertion) {
        headers.set(TenantAssertionHeaders.TENANT_ID, assertion.getTenantId());
        headers.set(TenantAssertionHeaders.HOST, assertion.getHost());
        headers.set(TenantAssertionHeaders.METHOD, assertion.getMethod());
        headers.set(TenantAssertionHeaders.PATH, assertion.getPath());
        headers.set(TenantAssertionHeaders.ISSUED_AT, String.valueOf(assertion.getIssuedAt()));
        headers.set(TenantAssertionHeaders.NONCE, assertion.getNonce());
        headers.set(TenantAssertionHeaders.SIGNATURE,
                ResolvedTenantAssertionSignatureUtils.sign(tenantAssertionSecret, assertion));
    }

    private void writeServicePrincipalHeaders(HttpHeaders headers) {
        AuthenticatedUserPrincipal principal = new AuthenticatedUserPrincipal(
                0L, "erp-gateway", "000000", 0, SERVICE_EXPIRES_AT);
        writePrincipalHeaders(headers, principal,
                InternalAuthSignatureUtils.sign(internalSignatureSecret, principal));
        headers.set("tenantId", principal.getTenantId());
    }

    private void writePrincipalHeaders(HttpHeaders headers, AuthenticatedUserPrincipal principal,
            String signature) {
        headers.set(AuthHeaders.USER_ID, String.valueOf(principal.getUserId()));
        headers.set(AuthHeaders.USER_NAME, principal.getUserName());
        headers.set(AuthHeaders.TENANT_ID, principal.getTenantId());
        headers.set(AuthHeaders.TOKEN_VERSION, String.valueOf(principal.getTokenVersion()));
        headers.set(AuthHeaders.EXPIRES_AT, String.valueOf(principal.getExpiresAt()));
        headers.set(AuthHeaders.SIGNATURE, signature);
    }

    private boolean isTenantScoped(String requestPath) {
        return matchesAny(requestPath, TENANT_PUBLIC_PATTERNS) || matchesAny(requestPath, PROTECTED_PATTERNS);
    }

    private boolean matchesAny(String requestPath, List<String> patterns) {
        for (String pattern : patterns) {
            if (pathMatcher.match(pattern, requestPath)) return true;
        }
        return false;
    }

    private String rawPath(ServerHttpRequest request) {
        String rawPath = request.getURI().getRawPath();
        return StringUtils.hasText(rawPath) ? rawPath : "/";
    }

    private Mono<Void> writeUnauthorized(ServerWebExchange exchange, String message) {
        return ApiErrorResponseWriter.writeReactive(exchange, objectMapper, ResultCode.UNAUTHORIZED, message);
    }

    private String safeMessage(Throwable error) {
        return error instanceof GatewayAccessException && StringUtils.hasText(error.getMessage())
                ? error.getMessage() : "租户域名无效或未验证";
    }

    private static String requiredSecret(String value, String property, int minimumBytes) {
        if (!StringUtils.hasText(value)) {
            throw new IllegalStateException(property + " must not be blank");
        }
        String normalized = value.trim();
        if (normalized.getBytes(StandardCharsets.UTF_8).length < minimumBytes) {
            throw new IllegalStateException(property + " must contain at least " + minimumBytes + " UTF-8 bytes");
        }
        return normalized;
    }

    private static String validateBaseUrl(String value) {
        if (!StringUtils.hasText(value)) throw new IllegalStateException("erp.internal.saas-base-url must not be blank");
        String normalized = value.trim();
        if (normalized.endsWith("/")) normalized = normalized.substring(0, normalized.length() - 1);
        try {
            URI uri = URI.create(normalized);
            if (!("http".equalsIgnoreCase(uri.getScheme()) || "https".equalsIgnoreCase(uri.getScheme()))
                    || uri.getHost() == null || uri.getRawUserInfo() != null
                    || uri.getRawQuery() != null || uri.getRawFragment() != null
                    || (StringUtils.hasText(uri.getRawPath()) && !"/".equals(uri.getRawPath()))) {
                throw new IllegalArgumentException();
            }
            return normalized;
        } catch (IllegalArgumentException error) {
            throw new IllegalStateException("erp.internal.saas-base-url must be an HTTP(S) origin", error);
        }
    }

    private static GatewayAccessException accessDenied(String message) {
        return new GatewayAccessException(message);
    }

    private static final class GatewayAccessException extends RuntimeException {
        private GatewayAccessException(String message) {
            super(message);
        }
    }
}

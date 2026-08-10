package com.erp.gateway.filter;

import com.erp.common.core.domain.ResultCode;
import com.erp.common.security.AuthHeaders;
import com.erp.common.security.AuthenticatedUserPrincipal;
import com.erp.common.security.InternalAuthSignatureUtils;
import com.erp.common.security.ResolvedTenantAssertion;
import com.erp.common.security.ResolvedTenantAssertionSignatureUtils;
import com.erp.common.security.TenantAssertionHeaders;
import com.erp.common.web.error.ApiErrorResponseWriter;
import com.erp.saas.contract.model.DeploymentMode;
import com.erp.saas.contract.model.SaasHostResolution;
import com.erp.saas.contract.model.TenantLifecycleState;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
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
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.regex.Pattern;

/**
 * Resolves tenant identity from the request host and relays only signed internal context.
 */
@Component
public class GatewayAuthFilter implements GlobalFilter, Ordered {
    private static final List<String> PROTECTED_PATTERNS =
            List.of("/system/**", "/workflow/**", "/business/**", "/saas/**");
    /**
     * 仅供服务间调用的内部契约路径，一律不允许从公网入口进入。
     *
     * <p>这些端点的设计前提是「调用方是可信服务」：它们普遍接受显式的 userId 入参并据此取数或写数，
     * 自身不做归属校验。而网关对受保护路径签发的是**调用者本人**的身份头，下游的内部认证过滤器
     * 只校验签名有效性、不校验主体是不是服务账号——两者叠加会让任何已登录用户通过
     * {@code /system/internal/**} 传别人的 userId 读写他人数据。因此在入口直接拒绝，
     * 内部调用走服务间网络，不经过网关。</p>
     */
    private static final List<String> INTERNAL_ONLY_PATTERNS = List.of(
            "/system/internal/**",
            "/workflow/internal/**",
            "/business/internal/**",
            "/saas/internal/**");
    private static final List<String> TENANT_PUBLIC_PATTERNS = List.of(
            "/login", "/logout", "/auth/**", "/system/public/saas/activation");
    private static final List<String> GLOBAL_PUBLIC_PATTERNS = List.of(
            "/doc.html",
            "/webjars/**",
            "/v3/api-docs/**",
            "/swagger-ui/**");
    private static final List<String> READ_ONLY_POST_PATTERNS = List.of(
            "/system/**/export",
            "/business/**/export");
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
    private final Duration domainCacheTtl;
    private final Clock clock;
    private final DeploymentMode deploymentMode;
    private final String boundTenantId;
    private final String boundHost;
    private final ConcurrentMap<String, CachedHostResolution> hostResolutionCache = new ConcurrentHashMap<>();

    public GatewayAuthFilter(WebClient.Builder webClientBuilder,
            ObjectMapper objectMapper,
            @Value("${erp.internal.auth-signature-secret:}") String internalSignatureSecret,
            @Value("${erp.saas.tenant-assertion-signature-secret:}") String tenantAssertionSecret,
            @Value("${erp.internal.saas-base-url:http://erp-saas-control}") String saasControlBaseUrl,
            @Value("${erp.saas.domain-resolve-timeout-ms:2000}") long domainResolveTimeoutMs,
            @Value("${erp.saas.domain-cache-ttl-ms:86400000}") long domainCacheTtlMs,
            @Value("${erp.saas.deployment-mode:SHARED}") DeploymentMode deploymentMode,
            @Value("${erp.saas.bound-tenant-id:}") String boundTenantId,
            @Value("${erp.saas.bound-host:}") String boundHost) {
        this(webClientBuilder, objectMapper, internalSignatureSecret, tenantAssertionSecret,
                saasControlBaseUrl, domainResolveTimeoutMs, domainCacheTtlMs, Clock.systemUTC(),
                deploymentMode, boundTenantId, boundHost);
    }

    GatewayAuthFilter(WebClient.Builder webClientBuilder, ObjectMapper objectMapper,
            String internalSignatureSecret, String tenantAssertionSecret,
            String saasControlBaseUrl, long domainResolveTimeoutMs, Clock clock) {
        this(webClientBuilder, objectMapper, internalSignatureSecret, tenantAssertionSecret,
                saasControlBaseUrl, domainResolveTimeoutMs, Duration.ofHours(24).toMillis(), clock,
                DeploymentMode.SHARED, null, null);
    }

    GatewayAuthFilter(WebClient.Builder webClientBuilder, ObjectMapper objectMapper,
            String internalSignatureSecret, String tenantAssertionSecret,
            String saasControlBaseUrl, long domainResolveTimeoutMs, long domainCacheTtlMs, Clock clock,
            DeploymentMode deploymentMode, String boundTenantId, String boundHost) {
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
        if (domainCacheTtlMs <= 0 || domainCacheTtlMs > Duration.ofDays(7).toMillis()) {
            throw new IllegalStateException("erp.saas.domain-cache-ttl-ms must be between 1 and 604800000");
        }
        this.domainCacheTtl = Duration.ofMillis(domainCacheTtlMs);
        this.clock = Objects.requireNonNull(clock, "clock must not be null");
        this.deploymentMode = Objects.requireNonNull(deploymentMode, "erp.saas.deployment-mode must not be null");
        if (deploymentMode == DeploymentMode.DEDICATED) {
            if (!StringUtils.hasText(boundTenantId)
                    || !TENANT_ID.matcher(boundTenantId.trim()).matches()) {
                throw new IllegalStateException("erp.saas.bound-tenant-id is required for dedicated deployments");
            }
            if (!StringUtils.hasText(boundHost)) {
                throw new IllegalStateException("erp.saas.bound-host is required for dedicated deployments");
            }
            this.boundTenantId = boundTenantId.trim();
            this.boundHost = normalizedHost(boundHost);
        } else {
            if (StringUtils.hasText(boundTenantId) || StringUtils.hasText(boundHost)) {
                throw new IllegalStateException("erp.saas.bound-tenant-id and bound-host require dedicated mode");
            }
            this.boundTenantId = null;
            this.boundHost = null;
        }
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String requestPath = exchange.getRequest().getURI().getPath();
        ServerHttpRequest sanitizedRequest = sanitize(exchange.getRequest());
        ServerWebExchange sanitizedExchange = exchange.mutate().request(sanitizedRequest).build();

        // 内部契约路径直接拒绝，必须排在放行判断之前，否则会被 GLOBAL_PUBLIC / 非租户作用域分支绕过。
        if (matchesAny(requestPath, INTERNAL_ONLY_PATTERNS)) {
            return writeForbidden(sanitizedExchange, "内部接口不对外开放");
        }

        if (matchesAny(requestPath, GLOBAL_PUBLIC_PATTERNS) || !isTenantScoped(requestPath)) {
            return chain.filter(sanitizedExchange);
        }

        return resolveTenant(sanitizedRequest)
                .flatMap(resolution -> forwardResolved(sanitizedExchange, sanitizedRequest,
                        resolution, requestPath, chain))
                .onErrorResume(error -> writeError(sanitizedExchange, error));
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
        if (resolution.getLifecycleState() == TenantLifecycleState.READ_ONLY
                && !isReadOnlyAllowed(request, requestPath)) {
            return Mono.error(readOnlyDenied());
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
        String cacheKey = normalizedHost(host);
        URI uri = UriComponentsBuilder.fromHttpUrl(saasControlBaseUrl)
                .path("/internal/saas/hosts/resolve")
                .queryParam("host", "{host}")
                .encode()
                .buildAndExpand(host.trim())
                .toUri();
        return webClientBuilder.build().get().uri(uri)
                .headers(this::writeServicePrincipalHeaders)
                .retrieve()
                .onStatus(status -> status.is4xxClientError(),
                        response -> Mono.error(accessDenied("租户域名无效或未验证")))
                .bodyToMono(SaasHostResolution.class)
                .timeout(domainResolveTimeout)
                .switchIfEmpty(Mono.error(accessDenied("租户域名无效或未验证")))
                .map(resolution -> validateResolution(host, request, resolution))
                .doOnNext(resolution -> hostResolutionCache.put(cacheKey,
                        new CachedHostResolution(resolution, clock.millis())))
                .onErrorResume(error -> cachedResolution(cacheKey, host, request, error));
    }

    private Mono<SaasHostResolution> cachedResolution(String cacheKey, String requestHost,
            ServerHttpRequest request, Throwable controlPlaneError) {
        if (controlPlaneError instanceof GatewayAccessException) {
            return Mono.error(controlPlaneError);
        }
        CachedHostResolution cached = hostResolutionCache.get(cacheKey);
        if (cached != null && clock.millis() - cached.cachedAtEpochMs() <= domainCacheTtl.toMillis()) {
            try {
                return Mono.just(validateResolution(requestHost, request, cached.resolution()));
            } catch (GatewayAccessException validationError) {
                hostResolutionCache.remove(cacheKey, cached);
                return Mono.error(validationError);
            }
        }
        if (cached != null) hostResolutionCache.remove(cacheKey, cached);
        return Mono.error(accessDenied("租户域名无效或未验证"));
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
        validateDeploymentBinding(resolution);
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

    private void validateDeploymentBinding(SaasHostResolution resolution) {
        if (resolution.getDeploymentMode() != deploymentMode) {
            throw deploymentMode == DeploymentMode.DEDICATED
                    ? accessDenied("租户域名不属于当前独立部署实例")
                    : accessDenied("租户域名无效或未验证");
        }
        if (deploymentMode == DeploymentMode.DEDICATED
                && (!boundTenantId.equals(resolution.getTenantId())
                || !boundHost.equals(normalizedHost(resolution.getHost())))) {
            throw accessDenied("租户域名不属于当前独立部署实例");
        }
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

    private boolean isReadOnlyAllowed(ServerHttpRequest request, String requestPath) {
        HttpMethod method = request.getMethod();
        if (method == null || method == HttpMethod.GET || method == HttpMethod.HEAD || method == HttpMethod.OPTIONS) {
            return true;
        }
        return method == HttpMethod.POST && matchesAny(requestPath, READ_ONLY_POST_PATTERNS);
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

    /**
     * 写出内部接口拒绝响应。
     *
     * @param exchange 当前请求
     * @param message  提示信息
     * @return 响应流
     */
    private Mono<Void> writeForbidden(ServerWebExchange exchange, String message) {
        return ApiErrorResponseWriter.writeReactive(exchange, objectMapper, ResultCode.FORBIDDEN, message);
    }

    private Mono<Void> writeError(ServerWebExchange exchange, Throwable error) {
        ResultCode resultCode = error instanceof GatewayAccessException accessError
                ? accessError.resultCode() : ResultCode.UNAUTHORIZED;
        return ApiErrorResponseWriter.writeReactive(exchange, objectMapper, resultCode, safeMessage(error));
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
        return new GatewayAccessException(ResultCode.UNAUTHORIZED, message);
    }

    private static GatewayAccessException readOnlyDenied() {
        return new GatewayAccessException(ResultCode.FORBIDDEN, "租户当前处于只读状态，禁止业务写入");
    }

    private String normalizedHost(String host) {
        return new ResolvedTenantAssertion("cache", host, "GET", "/", clock.millis(), "cache").getHost();
    }

    private static final class GatewayAccessException extends RuntimeException {
        private final ResultCode resultCode;

        private GatewayAccessException(ResultCode resultCode, String message) {
            super(message);
            this.resultCode = resultCode;
        }

        private ResultCode resultCode() {
            return resultCode;
        }
    }

    private record CachedHostResolution(SaasHostResolution resolution, long cachedAtEpochMs) {
    }
}

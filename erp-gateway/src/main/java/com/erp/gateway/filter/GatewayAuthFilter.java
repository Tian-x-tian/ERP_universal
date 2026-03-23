package com.erp.gateway.filter;

import com.erp.common.core.domain.R;
import com.erp.common.core.domain.ResultCode;
import com.erp.common.security.AuthHeaders;
import com.erp.common.security.AuthenticatedUserPrincipal;
import com.erp.common.security.InternalAuthSignatureUtils;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * Gateway 统一认证过滤器。
 */
@Component
public class GatewayAuthFilter implements GlobalFilter, Ordered {
    private static final List<String> PROTECTED_PATTERNS = List.of("/system/**", "/workflow/**", "/business/**");
    private static final List<String> PUBLIC_PATTERNS = List.of(
            "/login",
            "/logout",
            "/auth/**",
            "/system/public/tenants/active",
            "/doc.html",
            "/webjars/**",
            "/v3/api-docs/**",
            "/swagger-ui/**");

    private final WebClient.Builder webClientBuilder;
    private final ObjectMapper objectMapper;
    private final AntPathMatcher pathMatcher = new AntPathMatcher();
    private final String internalSignatureSecret;

    public GatewayAuthFilter(WebClient.Builder webClientBuilder,
            ObjectMapper objectMapper,
            @Value("${erp.internal.auth-signature-secret:}") String internalSignatureSecret) {
        this.webClientBuilder = webClientBuilder;
        this.objectMapper = objectMapper;
        this.internalSignatureSecret = internalSignatureSecret == null ? "" : internalSignatureSecret.trim();
        if (!StringUtils.hasText(this.internalSignatureSecret)) {
            throw new IllegalStateException("erp.internal.auth-signature-secret 未配置，Gateway 无法透传内部身份头");
        }
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String requestPath = exchange.getRequest().getURI().getPath();
        ServerHttpRequest sanitizedRequest = exchange.getRequest()
                .mutate()
                .headers(headers -> AuthHeaders.INTERNAL_HEADERS.forEach(headers::remove))
                .build();
        ServerWebExchange sanitizedExchange = exchange.mutate().request(sanitizedRequest).build();

        if (matchesAny(requestPath, PUBLIC_PATTERNS) || !matchesAny(requestPath, PROTECTED_PATTERNS)) {
            return chain.filter(sanitizedExchange);
        }

        String authorization = sanitizedRequest.getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
        String tenantId = resolveTenantId(sanitizedRequest.getHeaders());
        return verifyToken(authorization, tenantId)
                .flatMap(principal -> {
                    String signature = InternalAuthSignatureUtils.sign(internalSignatureSecret, principal);
                    ServerHttpRequest relayRequest = sanitizedRequest.mutate()
                            .headers(headers -> {
                                headers.set(AuthHeaders.USER_ID, String.valueOf(principal.getUserId()));
                                headers.set(AuthHeaders.USER_NAME, principal.getUserName());
                                headers.set(AuthHeaders.TENANT_ID, principal.getTenantId());
                                headers.set(AuthHeaders.TOKEN_VERSION, String.valueOf(principal.getTokenVersion()));
                                headers.set(AuthHeaders.EXPIRES_AT, String.valueOf(principal.getExpiresAt()));
                                headers.set(AuthHeaders.SIGNATURE, signature);
                            })
                            .build();
                    return chain.filter(sanitizedExchange.mutate().request(relayRequest).build());
                })
                .onErrorResume(ex -> writeUnauthorized(sanitizedExchange.getResponse(),
                        StringUtils.hasText(ex.getMessage()) ? ex.getMessage() : "Token无效或已过期"));
    }

    @Override
    public int getOrder() {
        return -100;
    }

    /**
     * 调用认证中心权威校验接口。
     *
     * @param authorization Authorization 请求头
     * @param tenantId      租户编号
     * @return 认证通过的用户主体
     */
    private Mono<AuthenticatedUserPrincipal> verifyToken(String authorization, String tenantId) {
        return webClientBuilder.build()
                .get()
                .uri("http://erp-auth/auth/token/verify")
                .headers(headers -> {
                    if (StringUtils.hasText(authorization)) {
                        headers.set(HttpHeaders.AUTHORIZATION, authorization);
                    }
                    if (StringUtils.hasText(tenantId)) {
                        headers.set("tenantId", tenantId);
                    }
                })
                .retrieve()
                .bodyToMono(JsonNode.class)
                .map(this::toVerifiedPrincipal);
    }

    /**
     * 将认证中心返回值转换为内部主体。
     *
     * @param rootNode 认证中心返回 JSON
     * @return 已认证用户主体
     */
    private AuthenticatedUserPrincipal toVerifiedPrincipal(JsonNode rootNode) {
        int code = rootNode.path("code").asInt((int) ResultCode.UNAUTHORIZED.getCode());
        if (code != (int) ResultCode.SUCCESS.getCode()) {
            throw new IllegalArgumentException(rootNode.path("message").asText("Token无效或已过期"));
        }
        JsonNode dataNode = rootNode.path("data");
        return new AuthenticatedUserPrincipal(
                dataNode.path("userId").asLong(),
                dataNode.path("userName").asText(null),
                dataNode.path("tenantId").asText(null),
                dataNode.path("tokenVersion").asInt(),
                dataNode.path("expiresAt").asLong());
    }

    /**
     * 解析租户编号。
     *
     * @param headers 请求头
     * @return 租户编号
     */
    private String resolveTenantId(HttpHeaders headers) {
        String tenantId = headers.getFirst("tenantId");
        if (!StringUtils.hasText(tenantId)) {
            tenantId = headers.getFirst("Tenantid");
        }
        return StringUtils.hasText(tenantId) ? tenantId.trim() : null;
    }

    /**
     * 判断路径是否命中任一模式。
     *
     * @param requestPath 请求路径
     * @param patterns    模式列表
     * @return true 表示命中
     */
    private boolean matchesAny(String requestPath, List<String> patterns) {
        for (String pattern : patterns) {
            if (pathMatcher.match(pattern, requestPath)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 输出统一未授权响应。
     *
     * @param response 响应对象
     * @param message  提示信息
     * @return 写出结果
     */
    private Mono<Void> writeUnauthorized(ServerHttpResponse response, String message) {
        response.setStatusCode(HttpStatus.UNAUTHORIZED);
        response.getHeaders().setContentType(MediaType.APPLICATION_JSON);
        try {
            byte[] body = objectMapper.writeValueAsBytes(R.failed(ResultCode.UNAUTHORIZED, message));
            return response.writeWith(Mono.just(response.bufferFactory().wrap(body)));
        } catch (Exception ex) {
            byte[] body = "{\"code\":40101,\"message\":\"Token无效或已过期\"}".getBytes(StandardCharsets.UTF_8);
            return response.writeWith(Mono.just(response.bufferFactory().wrap(body)));
        }
    }
}

package com.erp.common.security.servlet;

import com.erp.common.core.context.TenantContextHolder;
import com.erp.common.core.domain.ResultCode;
import com.erp.common.security.AuthHeaders;
import com.erp.common.security.AuthenticatedUserPrincipal;
import com.erp.common.security.InternalAuthSignatureUtils;
import com.erp.common.web.error.ApiErrorResponseWriter;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Clock;
import java.util.ArrayList;

/**
 * 内部签名认证过滤器基类。
 */
public class InternalAuthenticationFilterSupport extends OncePerRequestFilter {
    private static final String JSON_CONTENT_TYPE = "application/json; charset=UTF-8";

    private final ObjectMapper objectMapper;
    private final String internalSignatureSecret;
    private final String protectedPathPrefix;
    private final Clock clock;

    /**
     * 创建内部认证过滤器。
     *
     * @param objectMapper            JSON 工具
     * @param internalSignatureSecret 内部签名密钥
     * @param protectedPathPrefix     受保护路径前缀
     * @param moduleName              模块名称
     */
    protected InternalAuthenticationFilterSupport(ObjectMapper objectMapper,
            String internalSignatureSecret,
            String protectedPathPrefix,
            String moduleName) {
        this(objectMapper, internalSignatureSecret, protectedPathPrefix, moduleName, Clock.systemUTC());
    }

    protected InternalAuthenticationFilterSupport(ObjectMapper objectMapper,
            String internalSignatureSecret,
            String protectedPathPrefix,
            String moduleName,
            Clock clock) {
        this.objectMapper = objectMapper;
        this.internalSignatureSecret = internalSignatureSecret == null ? "" : internalSignatureSecret.trim();
        this.protectedPathPrefix = protectedPathPrefix == null ? "" : protectedPathPrefix.trim();
        this.clock = java.util.Objects.requireNonNull(clock, "clock");
        if (!StringUtils.hasText(this.internalSignatureSecret)) {
            throw new IllegalStateException("erp.internal.auth-signature-secret 未配置，" + moduleName + "无法校验内部身份头");
        }
    }

    /**
     * 处理模块内部签名认证。
     *
     * @param request  请求对象
     * @param response 响应对象
     * @param chain    过滤器链
     * @throws ServletException Servlet 异常
     * @throws IOException      IO 异常
     */
    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        if ("OPTIONS".equalsIgnoreCase(request.getMethod()) || !requiresAuth(request)) {
            chain.doFilter(request, response);
            return;
        }
        String tenantIdFromHeader = resolveTenantIdFromHeader(request);
        try {
            AuthenticatedUserPrincipal principal = resolvePrincipal(request, tenantIdFromHeader);
            TenantContextHolder.setTenantId(principal.getTenantId());
            UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                    principal, null, new ArrayList<>());
            authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
            SecurityContextHolder.getContext().setAuthentication(authentication);
            chain.doFilter(request, response);
        } catch (IllegalArgumentException ex) {
            writeUnauthorized(request, response, StringUtils.hasText(ex.getMessage()) ? ex.getMessage() : "Token无效或已过期");
        } finally {
            SecurityContextHolder.clearContext();
            TenantContextHolder.clear();
        }
    }

    /**
     * 从请求头解析租户编号。
     *
     * @param request 请求对象
     * @return 租户编号
     */
    protected String resolveTenantIdFromHeader(HttpServletRequest request) {
        if (request == null) {
            return null;
        }
        String tenantId = request.getHeader("tenantId");
        if (!StringUtils.hasText(tenantId)) {
            tenantId = request.getHeader("Tenantid");
        }
        return normalizeTenantId(tenantId);
    }

    /**
     * 解析内部认证主体。
     *
     * @param request            请求对象
     * @param tenantIdFromHeader 外部租户头
     * @return 认证主体
     */
    protected AuthenticatedUserPrincipal resolvePrincipal(HttpServletRequest request, String tenantIdFromHeader) {
        Long userId = toLong(request.getHeader(AuthHeaders.USER_ID));
        String userName = trimToNull(request.getHeader(AuthHeaders.USER_NAME));
        String tenantId = trimToNull(request.getHeader(AuthHeaders.TENANT_ID));
        Integer tokenVersion = toInteger(request.getHeader(AuthHeaders.TOKEN_VERSION));
        Long expiresAt = toLong(request.getHeader(AuthHeaders.EXPIRES_AT));
        String signature = trimToNull(request.getHeader(AuthHeaders.SIGNATURE));

        if (userId == null || !StringUtils.hasText(userName) || !StringUtils.hasText(tenantId)
                || !StringUtils.hasText(signature) || expiresAt == null) {
            throw new IllegalArgumentException(ResultCode.UNAUTHORIZED.getMessage());
        }
        if (StringUtils.hasText(tenantIdFromHeader) && !tenantIdFromHeader.equals(tenantId)) {
            throw new IllegalArgumentException("租户与令牌不匹配");
        }
        if (expiresAt <= clock.millis()) {
            throw new IllegalArgumentException("Token无效或已过期");
        }

        AuthenticatedUserPrincipal principal = new AuthenticatedUserPrincipal(userId, userName, tenantId,
                tokenVersion, expiresAt);
        if (!InternalAuthSignatureUtils.matches(internalSignatureSecret, principal, signature)) {
            throw new IllegalArgumentException("Token无效或已过期");
        }
        return principal;
    }

    /**
     * 输出未授权响应。
     *
     * @param response 响应对象
     * @param message  提示信息
     * @throws IOException IO 异常
     */
    protected void writeUnauthorized(HttpServletRequest request, HttpServletResponse response, String message)
            throws java.io.IOException {
        TenantContextHolder.clear();
        SecurityContextHolder.clearContext();
        ApiErrorResponseWriter.writeServlet(request, response, objectMapper, com.erp.common.core.domain.ResultCode.UNAUTHORIZED, message);
    }

    /**
     * 兼容旧调用方的未授权写出入口。
     *
     * @param response 响应对象
     * @param message  提示信息
     * @throws java.io.IOException IO 异常
     */
    protected void writeUnauthorized(HttpServletResponse response, String message) throws java.io.IOException {
        writeUnauthorized(null, response, message);
    }

    /**
     * 判断请求是否需要内部认证。
     *
     * @param request 请求对象
     * @return true 表示需要鉴权
     */
    protected boolean requiresAuth(HttpServletRequest request) {
        String requestUri = request == null ? null : request.getRequestURI();
        return StringUtils.hasText(requestUri) && requestUri.startsWith(protectedPathPrefix);
    }

    /**
     * 转换为 Long。
     *
     * @param rawValue 原始值
     * @return Long 值
     */
    protected Long toLong(String rawValue) {
        try {
            return StringUtils.hasText(rawValue) ? Long.parseLong(rawValue.trim()) : null;
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    /**
     * 转换为 Integer。
     *
     * @param rawValue 原始值
     * @return Integer 值
     */
    protected Integer toInteger(String rawValue) {
        try {
            return StringUtils.hasText(rawValue) ? Integer.parseInt(rawValue.trim()) : 0;
        } catch (NumberFormatException ex) {
            return 0;
        }
    }

    /**
     * 去空白后为空时返回 null。
     *
     * @param rawValue 原始值
     * @return 规范化结果
     */
    protected String trimToNull(String rawValue) {
        return StringUtils.hasText(rawValue) ? rawValue.trim() : null;
    }

    /**
     * 规范化租户编号。
     *
     * @param rawTenantId 原始租户编号
     * @return 租户编号
     */
    protected String normalizeTenantId(Object rawTenantId) {
        if (rawTenantId == null) {
            return null;
        }
        String tenantId = String.valueOf(rawTenantId).trim();
        return StringUtils.hasText(tenantId) ? tenantId : null;
    }
}

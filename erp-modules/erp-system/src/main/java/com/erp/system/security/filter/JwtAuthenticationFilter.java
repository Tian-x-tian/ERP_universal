package com.erp.system.security.filter;

import com.erp.common.core.context.TenantContextHolder;
import com.erp.common.core.domain.R;
import com.erp.common.core.domain.ResultCode;
import com.erp.common.utils.JwtUtils;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.ArrayList;

/**
 * JWT 认证过滤器
 */
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {
    private static final String LOGIN_URI_SUFFIX = "/login";
    private static final String JSON_CONTENT_TYPE = "application/json; charset=UTF-8";
    private final ObjectMapper objectMapper;

    public JwtAuthenticationFilter(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String tenantIdFromHeader = resolveTenantIdFromHeader(request);
        boolean loginRequest = isLoginRequest(request);

        try {
            if (loginRequest && StringUtils.hasText(tenantIdFromHeader)) {
                TenantContextHolder.setTenantId(tenantIdFromHeader);
            }

            if (!loginRequest) {
                String token = parseJwt(request);
                if (token != null) {
                    try {
                        Claims claims = JwtUtils.parseToken(token);
                        String userId = claims.getSubject();
                        String tenantIdFromToken = toTenantId(claims.get("tenantId"));

                        if (StringUtils.hasText(tenantIdFromHeader) && StringUtils.hasText(tenantIdFromToken)
                                && !tenantIdFromHeader.equals(tenantIdFromToken)) {
                            writeUnauthorized(response, "租户与令牌不匹配");
                            return;
                        }

                        if (!StringUtils.hasText(tenantIdFromToken)) {
                            writeUnauthorized(response, ResultCode.UNAUTHORIZED.getMessage());
                            return;
                        }
                        TenantContextHolder.setTenantId(tenantIdFromToken);

                        // 将用户信息存入 SecurityContext
                        UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                                userId, null, new ArrayList<>());
                        authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                        SecurityContextHolder.getContext().setAuthentication(authentication);
                    } catch (Exception e) {
                        // Token 无效或过期时直接输出统一响应，避免部分场景漏套壳
                        writeUnauthorized(response, "Token无效或已过期");
                        return;
                    }
                }
            }
            chain.doFilter(request, response);
        } finally {
            SecurityContextHolder.clearContext();
            TenantContextHolder.clear();
        }
    }

    /**
     * 判断是否为登录请求。
     *
     * @param request 请求对象
     * @return true 表示登录请求
     */
    private boolean isLoginRequest(HttpServletRequest request) {
        String requestUri = request == null ? null : request.getRequestURI();
        return StringUtils.hasText(requestUri) && requestUri.endsWith(LOGIN_URI_SUFFIX);
    }

    /**
     * 解析请求头中的租户编号。
     *
     * @param request 请求对象
     * @return 租户编号（不存在返回 null）
     */
    private String resolveTenantIdFromHeader(HttpServletRequest request) {
        if (request == null) {
            return null;
        }
        String tenantId = request.getHeader("tenantId");
        if (!StringUtils.hasText(tenantId)) {
            tenantId = request.getHeader("Tenantid");
        }
        return toTenantId(tenantId);
    }

    /**
     * 规范化租户编号。
     *
     * @param rawTenantId 原始租户编号
     * @return 去空白后的租户编号，空值返回 null
     */
    private String toTenantId(Object rawTenantId) {
        if (rawTenantId == null) {
            return null;
        }
        String tenantId = String.valueOf(rawTenantId).trim();
        return StringUtils.hasText(tenantId) ? tenantId : null;
    }

    /**
     * 解析 Bearer Token。
     *
     * @param request 请求对象
     * @return JWT 字符串
     */
    private String parseJwt(HttpServletRequest request) {
        String headerAuth = request.getHeader("Authorization");
        if (StringUtils.hasText(headerAuth) && headerAuth.startsWith("Bearer ")) {
            return headerAuth.substring(7);
        }
        return null;
    }

    /**
     * 输出未登录响应。
     *
     * @param response 响应对象
     * @param message  提示信息
     * @throws IOException IO 异常
     */
    private void writeUnauthorized(HttpServletResponse response, String message) throws IOException {
        TenantContextHolder.clear();
        SecurityContextHolder.clearContext();
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setCharacterEncoding("UTF-8");
        response.setContentType(JSON_CONTENT_TYPE);
        R<Void> body = R.failed(ResultCode.UNAUTHORIZED, message);
        response.getWriter().write(objectMapper.writeValueAsString(body));
        response.getWriter().flush();
    }
}

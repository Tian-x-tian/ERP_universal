package com.erp.system.security.filter;

import com.erp.common.core.context.TenantContextHolder;
import com.erp.common.utils.JwtUtils;
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

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String tenantIdFromHeader = resolveTenantIdFromHeader(request);
        boolean loginRequest = isLoginRequest(request);

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
                        TenantContextHolder.clear();
                        SecurityContextHolder.clearContext();
                        chain.doFilter(request, response);
                        return;
                    }

                    if (!StringUtils.hasText(tenantIdFromToken)) {
                        TenantContextHolder.clear();
                        SecurityContextHolder.clearContext();
                        chain.doFilter(request, response);
                        return;
                    }
                    TenantContextHolder.setTenantId(tenantIdFromToken);

                    // 将用户信息存入 SecurityContext
                    UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                            userId, null, new ArrayList<>());
                    authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                    SecurityContextHolder.getContext().setAuthentication(authentication);
                } catch (Exception e) {
                    // Token 无效或过期
                    TenantContextHolder.clear();
                    SecurityContextHolder.clearContext();
                }
            }
        }

        try {
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
}

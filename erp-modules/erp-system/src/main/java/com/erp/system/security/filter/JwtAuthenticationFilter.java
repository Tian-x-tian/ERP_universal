package com.erp.system.security.filter;

import com.erp.common.core.context.TenantContextHolder;
import com.erp.common.core.domain.R;
import com.erp.common.core.domain.ResultCode;
import com.erp.common.utils.JwtUtils;
import com.erp.system.domain.SysUser;
import com.erp.system.service.ISysUserService;
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
    private static final String JSON_CONTENT_TYPE = "application/json; charset=UTF-8";
    private final ObjectMapper objectMapper;
    private final ISysUserService userService;

    public JwtAuthenticationFilter(ObjectMapper objectMapper, ISysUserService userService) {
        this.objectMapper = objectMapper;
        this.userService = userService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String tenantIdFromHeader = resolveTenantIdFromHeader(request);

        try {
            String token = parseJwt(request);
            if (token != null) {
                try {
                    Claims claims = JwtUtils.parseToken(token);
                    String userName = claims.getSubject();
                    String tenantIdFromToken = toTenantId(claims.get("tenantId"));
                    Integer tokenVersion = toInteger(claims.get("tokenVersion"));

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
                    SysUser user = userService.selectUserByUserName(userName);
                    if (user == null || !tenantIdFromToken.equals(toTenantId(user.getTenantId()))) {
                        writeUnauthorized(response, ResultCode.UNAUTHORIZED.getMessage());
                        return;
                    }
                    if (!"0".equals(user.getStatus()) || "2".equals(user.getDelFlag())) {
                        writeUnauthorized(response, "账号不可用");
                        return;
                    }
                    if (!tokenVersionMatches(user.getTokenVersion(), tokenVersion)) {
                        writeUnauthorized(response, "登录状态已失效，请重新登录");
                        return;
                    }

                    // 将用户信息存入 SecurityContext
                    UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                            userName, null, new ArrayList<>());
                    authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                    SecurityContextHolder.getContext().setAuthentication(authentication);
                } catch (Exception e) {
                    // Token 无效或过期时直接输出统一响应，避免部分场景漏套壳
                    writeUnauthorized(response, "Token无效或已过期");
                    return;
                }
            }
            chain.doFilter(request, response);
        } finally {
            SecurityContextHolder.clearContext();
            TenantContextHolder.clear();
        }
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
     * 规范化 Token 版本号。
     *
     * @param rawTokenVersion 原始版本号
     * @return 版本号，缺失时返回 0
     */
    private Integer toInteger(Object rawTokenVersion) {
        if (rawTokenVersion == null) {
            return 0;
        }
        if (rawTokenVersion instanceof Number) {
            return ((Number) rawTokenVersion).intValue();
        }
        String tokenVersion = String.valueOf(rawTokenVersion).trim();
        if (!StringUtils.hasText(tokenVersion)) {
            return 0;
        }
        return Integer.parseInt(tokenVersion);
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

    /**
     * 校验令牌版本号与数据库当前版本是否一致。
     *
     * @param currentTokenVersion 数据库当前版本号
     * @param tokenVersion        令牌中的版本号
     * @return true 表示令牌仍然有效
     */
    private boolean tokenVersionMatches(Integer currentTokenVersion, Integer tokenVersion) {
        int currentVersion = currentTokenVersion == null ? 0 : currentTokenVersion;
        int requestVersion = tokenVersion == null ? 0 : tokenVersion;
        return currentVersion == requestVersion;
    }
}

package com.erp.common.security.servlet;

import com.erp.common.core.context.TenantContextHolder;
import com.erp.common.security.AuthenticatedUserPrincipal;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.util.StringUtils;

/**
 * 当前登录用户解析器基类。
 */
public class SecurityUserResolverSupport {

    /**
     * 获取当前登录用户名。
     *
     * @return 当前用户名
     */
    public String getCurrentUsername() {
        AuthenticatedUserPrincipal principal = getCurrentPrincipal();
        return principal == null ? null : principal.getUserName();
    }

    /**
     * 获取当前登录租户编号。
     *
     * @return 当前租户编号
     */
    public String getCurrentTenantId() {
        AuthenticatedUserPrincipal principal = getCurrentPrincipal();
        String tenantId = principal == null ? TenantContextHolder.getTenantId() : principal.getTenantId();
        return StringUtils.hasText(tenantId) ? tenantId.trim() : null;
    }

    /**
     * 获取当前登录用户ID。
     *
     * @return 当前用户ID
     */
    public Long getCurrentUserId() {
        AuthenticatedUserPrincipal principal = getCurrentPrincipal();
        return principal == null ? null : principal.getUserId();
    }

    /**
     * 获取当前认证主体。
     *
     * @return 已认证主体
     */
    protected AuthenticatedUserPrincipal getCurrentPrincipal() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()
                || authentication instanceof AnonymousAuthenticationToken) {
            return null;
        }
        Object principal = authentication.getPrincipal();
        if (principal instanceof AuthenticatedUserPrincipal authenticatedUserPrincipal) {
            return authenticatedUserPrincipal;
        }
        return null;
    }
}

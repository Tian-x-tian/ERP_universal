package com.erp.business.security.service;

import com.erp.common.core.context.TenantContextHolder;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * 当前登录用户解析器。
 */
@Component
public class SecurityUserResolver {

    /**
     * 获取当前登录用户名。
     *
     * @return 当前用户名
     */
    public String getCurrentUsername() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()
                || authentication instanceof AnonymousAuthenticationToken) {
            return null;
        }
        Object principal = authentication.getPrincipal();
        if (principal == null) {
            return null;
        }
        String username = principal.toString();
        return StringUtils.hasText(username) ? username.trim() : null;
    }

    /**
     * 获取当前登录租户编号。
     *
     * @return 当前租户编号
     */
    public String getCurrentTenantId() {
        String tenantId = TenantContextHolder.getTenantId();
        return StringUtils.hasText(tenantId) ? tenantId.trim() : null;
    }
}

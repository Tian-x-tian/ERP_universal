package com.erp.system.security.service;

import com.erp.system.domain.SysUser;
import com.erp.system.service.ISysUserService;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * 当前登录用户解析器
 */
@Component
public class SecurityUserResolver {

    private final ISysUserService userService;

    public SecurityUserResolver(ISysUserService userService) {
        this.userService = userService;
    }

    /**
     * 获取当前登录用户账号。
     *
     * @return 当前账号，不存在时返回 null
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
        return StringUtils.hasText(username) ? username : null;
    }

    /**
     * 获取当前登录用户ID。
     *
     * @return 当前用户ID，不存在时返回 null
     */
    public Long getCurrentUserId() {
        String username = getCurrentUsername();
        if (!StringUtils.hasText(username)) {
            return null;
        }
        SysUser user = userService.selectUserByUserName(username);
        return user != null ? user.getUserId() : null;
    }
}

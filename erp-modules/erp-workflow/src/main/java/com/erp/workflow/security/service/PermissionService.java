package com.erp.workflow.security.service;

import com.erp.common.client.internal.InternalPlatformClient;
import com.erp.platform.contract.model.PlatformAuthorityBundle;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 工作流模块接口权限校验服务。
 */
@Component("ss")
public class PermissionService {
    private static final String ALL_PERMISSION = "*:*:*";
    private static final String SUPER_ADMIN_ROLE = "admin";
    private static final String AUTHORITY_CACHE_KEY = PermissionService.class.getName() + ".AUTHORITY_BUNDLE";

    private final SecurityUserResolver securityUserResolver;
    private final InternalPlatformClient internalPlatformClient;

    public PermissionService(SecurityUserResolver securityUserResolver,
            InternalPlatformClient internalPlatformClient) {
        this.securityUserResolver = securityUserResolver;
        this.internalPlatformClient = internalPlatformClient;
    }

    /**
     * 校验当前用户是否具备指定权限。
     *
     * @param permission 权限标识
     * @return true 表示具备权限
     */
    public boolean hasPermi(String permission) {
        if (!StringUtils.hasText(permission) || securityUserResolver.getCurrentUserId() == null) {
            return false;
        }
        PlatformAuthorityBundle authorityBundle = resolveAuthorityBundle();
        if (isSuperAdmin(authorityBundle)) {
            return true;
        }
        Set<String> permissions = normalize(authorityBundle == null ? null : authorityBundle.getPermissions());
        return containsPermission(permissions, permission.trim());
    }

    /**
     * 校验当前用户是否具备任一权限。
     *
     * @param permissions 权限标识集合
     * @return true 表示具备任一权限
     */
    public boolean hasAnyPermi(String... permissions) {
        if (permissions == null || permissions.length == 0) {
            return false;
        }
        for (String permission : permissions) {
            if (hasPermi(permission)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 判断权限包是否包含平台超级管理员角色。
     *
     * @param authorityBundle 权限包
     * @return true 表示平台超级管理员
     */
    private boolean isSuperAdmin(PlatformAuthorityBundle authorityBundle) {
        List<String> roleKeys = authorityBundle == null ? null : authorityBundle.getRoleKeys();
        if (roleKeys == null || roleKeys.isEmpty()) {
            return false;
        }
        return roleKeys.stream()
                .filter(StringUtils::hasText)
                .map(String::trim)
                .anyMatch(SUPER_ADMIN_ROLE::equals);
    }

    /**
     * 规范化权限集合。
     *
     * @param permissions 原始权限集合
     * @return 规范化权限集合
     */
    private Set<String> normalize(List<String> permissions) {
        if (permissions == null || permissions.isEmpty()) {
            return Collections.emptySet();
        }
        return permissions.stream()
                .filter(StringUtils::hasText)
                .map(String::trim)
                .collect(Collectors.toSet());
    }

    /**
     * 在用户权限集中匹配目标权限。
     *
     * @param userPerms 用户权限集合
     * @param requiredPermission 目标权限
     * @return true 表示匹配成功
     */
    private boolean containsPermission(Set<String> userPerms, String requiredPermission) {
        for (String grantedPerm : userPerms) {
            if (ALL_PERMISSION.equals(grantedPerm) || requiredPermission.equals(grantedPerm)) {
                return true;
            }
            if (grantedPerm.endsWith(":*")) {
                String prefix = grantedPerm.substring(0, grantedPerm.length() - 1);
                if (requiredPermission.startsWith(prefix)) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * 获取当前请求缓存的权限包，避免同一请求内重复远程查询。
     *
     * @return 权限包
     */
    private PlatformAuthorityBundle resolveAuthorityBundle() {
        HttpServletRequest request = currentRequest();
        if (request == null) {
            return internalPlatformClient.getAuthorities();
        }
        Object cachedValue = request.getAttribute(AUTHORITY_CACHE_KEY);
        if (cachedValue instanceof PlatformAuthorityBundle platformAuthorityBundle) {
            return platformAuthorityBundle;
        }
        PlatformAuthorityBundle authorityBundle = internalPlatformClient.getAuthorities();
        request.setAttribute(AUTHORITY_CACHE_KEY, authorityBundle);
        return authorityBundle;
    }

    /**
     * 获取当前线程绑定的 HTTP 请求。
     *
     * @return 当前请求
     */
    private HttpServletRequest currentRequest() {
        RequestAttributes requestAttributes = RequestContextHolder.getRequestAttributes();
        if (requestAttributes instanceof ServletRequestAttributes servletRequestAttributes) {
            return servletRequestAttributes.getRequest();
        }
        return null;
    }
}

package com.erp.business.security.service;

import com.erp.business.mapper.SecurityPermissionMapper;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 接口权限校验服务。
 */
@Component("ss")
public class PermissionService {
    private static final String ALL_PERMISSION = "*:*:*";
    private static final String SUPER_ADMIN_ROLE = "admin";

    private final SecurityUserResolver securityUserResolver;
    private final SecurityPermissionMapper permissionMapper;

    public PermissionService(SecurityUserResolver securityUserResolver, SecurityPermissionMapper permissionMapper) {
        this.securityUserResolver = securityUserResolver;
        this.permissionMapper = permissionMapper;
    }

    /**
     * 校验当前用户是否具备指定权限。
     *
     * @param permission 权限标识
     * @return true 表示具备权限
     */
    public boolean hasPermi(String permission) {
        if (!StringUtils.hasText(permission)) {
            return false;
        }
        String currentUsername = securityUserResolver.getCurrentUsername();
        String currentTenantId = securityUserResolver.getCurrentTenantId();
        if (!StringUtils.hasText(currentUsername) || !StringUtils.hasText(currentTenantId)) {
            return false;
        }
        if (isSuperAdmin(currentTenantId, currentUsername)) {
            return true;
        }
        Set<String> permissions = normalize(permissionMapper.selectPermissionsByUserName(currentTenantId, currentUsername));
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
     * 判断用户是否为超级管理员。
     *
     * @param tenantId 租户编号
     * @param userName 用户账号
     * @return true 表示平台超级管理员
     */
    private boolean isSuperAdmin(String tenantId, String userName) {
        List<String> roleKeys = permissionMapper.selectRoleKeysByUserName(tenantId, userName);
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
}

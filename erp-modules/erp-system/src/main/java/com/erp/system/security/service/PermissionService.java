package com.erp.system.security.service;

import com.erp.system.service.ISysMenuService;
import com.erp.system.service.ISysRoleService;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.Collections;
import java.util.Set;

/**
 * 接口权限校验服务。
 */
@Component("ss")
public class PermissionService {

    private static final String ALL_PERMISSION = "*:*:*";

    private final SecurityUserResolver securityUserResolver;
    private final ISysRoleService roleService;
    private final ISysMenuService menuService;

    public PermissionService(SecurityUserResolver securityUserResolver,
            ISysRoleService roleService,
            ISysMenuService menuService) {
        this.securityUserResolver = securityUserResolver;
        this.roleService = roleService;
        this.menuService = menuService;
    }

    /**
     * 校验当前用户是否具备指定权限。
     *
     * @param permission 权限标识
     * @return true 表示具备权限，false 表示无权限
     */
    public boolean hasPermi(String permission) {
        if (!StringUtils.hasText(permission)) {
            return false;
        }
        Long currentUserId = securityUserResolver.getCurrentUserId();
        if (currentUserId == null) {
            return false;
        }
        if (roleService.isPlatformSuperAdmin(currentUserId)) {
            return true;
        }
        Set<String> permissions = menuService.selectMenuPermsByUserId(currentUserId);
        return containsPermission(permissions, permission.trim());
    }

    /**
     * 校验当前用户是否具备任一权限。
     *
     * @param permissions 权限标识集合
     * @return true 表示至少具备一项权限，false 表示都不具备
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
     * 在用户权限集中匹配目标权限。
     *
     * @param userPerms          用户权限集合
     * @param requiredPermission 目标权限
     * @return true 表示匹配成功，false 表示匹配失败
     */
    private boolean containsPermission(Set<String> userPerms, String requiredPermission) {
        Set<String> safePerms = userPerms == null ? Collections.emptySet() : userPerms;
        for (String granted : safePerms) {
            if (!StringUtils.hasText(granted)) {
                continue;
            }
            String grantedPerm = granted.trim();
            if (ALL_PERMISSION.equals(grantedPerm)) {
                return true;
            }
            if (requiredPermission.equals(grantedPerm)) {
                return true;
            }
            // 支持 system:user:* 形式的通配权限。
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

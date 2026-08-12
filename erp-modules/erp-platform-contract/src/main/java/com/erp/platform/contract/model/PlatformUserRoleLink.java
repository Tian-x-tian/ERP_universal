package com.erp.platform.contract.model;

import java.io.Serializable;

/**
 * 平台用户角色关联只读投影。
 */
public class PlatformUserRoleLink implements Serializable {
    private static final long serialVersionUID = 1L;

    private String tenantId;
    private Long userId;
    private Long roleId;

    public String getTenantId() {
        return tenantId;
    }

    public void setTenantId(String tenantId) {
        this.tenantId = tenantId;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public Long getRoleId() {
        return roleId;
    }

    public void setRoleId(Long roleId) {
        this.roleId = roleId;
    }
}

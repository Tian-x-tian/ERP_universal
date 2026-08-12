package com.erp.system.domain;

import com.baomidou.mybatisplus.annotation.TableName;
import java.io.Serializable;

/**
 * 角色和菜单关联表 sys_role_menu
 */
@TableName("sys_role_menu")
public class SysRoleMenu implements Serializable {
    private static final long serialVersionUID = 1L;

    /** 租户编号 */
    private String tenantId;

    /** 角色ID */
    private Long roleId;

    /** 菜单ID */
    private Long menuId;


    public String getTenantId() {
        return tenantId;
    }

    public void setTenantId(String tenantId) {
        this.tenantId = tenantId;
    }

    public Long getRoleId() {
        return roleId;
    }

    public void setRoleId(Long roleId) {
        this.roleId = roleId;
    }

    public Long getMenuId() {
        return menuId;
    }

    public void setMenuId(Long menuId) {
        this.menuId = menuId;
    }
}

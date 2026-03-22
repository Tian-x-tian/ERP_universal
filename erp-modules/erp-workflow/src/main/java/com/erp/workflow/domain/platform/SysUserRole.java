package com.erp.workflow.domain.platform;

import com.baomidou.mybatisplus.annotation.TableName;
import java.io.Serializable;

/**
 * 用户和角色关联表 sys_user_role
 */
@TableName("sys_user_role")
public class SysUserRole implements Serializable {
    private static final long serialVersionUID = 1L;

    /** 租户编号 */
    private String tenantId;

    /** 用户ID */
    private Long userId;

    /** 角色ID */
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



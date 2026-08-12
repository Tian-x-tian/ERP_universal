package com.erp.system.domain;

import com.baomidou.mybatisplus.annotation.TableName;

import java.io.Serializable;

/**
 * 角色部门关联对象 sys_role_dept
 */
@TableName("sys_role_dept")
public class SysRoleDept implements Serializable {
    private static final long serialVersionUID = 1L;

    /** 租户编号 */
    private String tenantId;

    /** 角色ID */
    private Long roleId;

    /** 部门ID */
    private Long deptId;


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

    public Long getDeptId() {
        return deptId;
    }

    public void setDeptId(Long deptId) {
        this.deptId = deptId;
    }
}

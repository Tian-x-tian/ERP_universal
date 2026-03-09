package com.erp.system.domain;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.io.Serializable;

/**
 * 用户和角色关联表 sys_user_role
 */
@Data
@TableName("sys_user_role")
public class SysUserRole implements Serializable {
    private static final long serialVersionUID = 1L;

    /** 租户编号 */
    private String tenantId;

    /** 用户ID */
    private Long userId;

    /** 角色ID */
    private Long roleId;
}

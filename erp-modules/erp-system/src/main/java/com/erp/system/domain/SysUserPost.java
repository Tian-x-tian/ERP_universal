package com.erp.system.domain;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;

/**
 * 用户岗位关联对象 sys_user_post
 */
@Data
@TableName("sys_user_post")
public class SysUserPost implements Serializable {
    private static final long serialVersionUID = 1L;

    /** 租户编号 */
    private String tenantId;

    /** 用户ID */
    private Long userId;

    /** 岗位ID */
    private Long postId;
}

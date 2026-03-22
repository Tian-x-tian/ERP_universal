package com.erp.workflow.domain.platform;

import com.baomidou.mybatisplus.annotation.TableName;

import java.io.Serializable;

/**
 * 用户岗位关联对象 sys_user_post
 */
@TableName("sys_user_post")
public class SysUserPost implements Serializable {
    private static final long serialVersionUID = 1L;

    /** 租户编号 */
    private String tenantId;

    /** 用户ID */
    private Long userId;

    /** 岗位ID */
    private Long postId;


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

    public Long getPostId() {
        return postId;
    }

    public void setPostId(Long postId) {
        this.postId = postId;
    }
}



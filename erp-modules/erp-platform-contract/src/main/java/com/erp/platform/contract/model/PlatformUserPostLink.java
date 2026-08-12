package com.erp.platform.contract.model;

import java.io.Serializable;

/**
 * 平台用户岗位关联只读投影。
 */
public class PlatformUserPostLink implements Serializable {
    private static final long serialVersionUID = 1L;

    private String tenantId;
    private Long userId;
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

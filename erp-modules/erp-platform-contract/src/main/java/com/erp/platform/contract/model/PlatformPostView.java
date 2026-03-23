package com.erp.platform.contract.model;

import java.io.Serializable;

/**
 * 平台岗位只读投影。
 */
public class PlatformPostView implements Serializable {
    private static final long serialVersionUID = 1L;

    private Long postId;
    private String tenantId;
    private String postCode;
    private String postName;
    private String status;

    public Long getPostId() {
        return postId;
    }

    public void setPostId(Long postId) {
        this.postId = postId;
    }

    public String getTenantId() {
        return tenantId;
    }

    public void setTenantId(String tenantId) {
        this.tenantId = tenantId;
    }

    public String getPostCode() {
        return postCode;
    }

    public void setPostCode(String postCode) {
        this.postCode = postCode;
    }

    public String getPostName() {
        return postName;
    }

    public void setPostName(String postName) {
        this.postName = postName;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}

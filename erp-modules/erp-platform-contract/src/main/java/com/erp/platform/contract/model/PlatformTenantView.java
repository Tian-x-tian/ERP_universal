package com.erp.platform.contract.model;

import java.io.Serializable;

/**
 * 平台租户只读投影。
 */
public class PlatformTenantView implements Serializable {
    private static final long serialVersionUID = 1L;

    private Long id;
    private String tenantId;
    private String tenantName;
    private String status;
    private String delFlag;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getTenantId() {
        return tenantId;
    }

    public void setTenantId(String tenantId) {
        this.tenantId = tenantId;
    }

    public String getTenantName() {
        return tenantName;
    }

    public void setTenantName(String tenantName) {
        this.tenantName = tenantName;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getDelFlag() {
        return delFlag;
    }

    public void setDelFlag(String delFlag) {
        this.delFlag = delFlag;
    }
}


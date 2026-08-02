package com.erp.saas.contract.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.io.Serializable;

@JsonIgnoreProperties(ignoreUnknown = true)
public class SaasTenantPurgeRequest implements Serializable {
    private static final long serialVersionUID = 1L;

    private String requestId;
    private String tenantId;
    private String confirmationTenantId;

    public SaasTenantPurgeRequest() {
    }

    public SaasTenantPurgeRequest(String requestId, String tenantId, String confirmationTenantId) {
        this.requestId = requestId;
        this.tenantId = tenantId;
        this.confirmationTenantId = confirmationTenantId;
    }

    public String getRequestId() {
        return requestId;
    }

    public void setRequestId(String requestId) {
        this.requestId = requestId;
    }

    public String getTenantId() {
        return tenantId;
    }

    public void setTenantId(String tenantId) {
        this.tenantId = tenantId;
    }

    public String getConfirmationTenantId() {
        return confirmationTenantId;
    }

    public void setConfirmationTenantId(String confirmationTenantId) {
        this.confirmationTenantId = confirmationTenantId;
    }
}

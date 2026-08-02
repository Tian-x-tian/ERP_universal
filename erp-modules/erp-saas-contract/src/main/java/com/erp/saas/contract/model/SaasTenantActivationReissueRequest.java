package com.erp.saas.contract.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.io.Serializable;

@JsonIgnoreProperties(ignoreUnknown = true)
public class SaasTenantActivationReissueRequest implements Serializable {
    private static final long serialVersionUID = 1L;
    private String requestId;
    private String tenantId;

    public SaasTenantActivationReissueRequest() {
    }

    public SaasTenantActivationReissueRequest(String requestId, String tenantId) {
        this.requestId = requestId;
        this.tenantId = tenantId;
    }

    public String getRequestId() { return requestId; }
    public void setRequestId(String requestId) { this.requestId = requestId; }
    public String getTenantId() { return tenantId; }
    public void setTenantId(String tenantId) { this.tenantId = tenantId; }
}

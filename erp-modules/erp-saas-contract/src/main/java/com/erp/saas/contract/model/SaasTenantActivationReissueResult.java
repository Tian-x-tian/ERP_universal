package com.erp.saas.contract.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.io.Serializable;

@JsonIgnoreProperties(ignoreUnknown = true)
public class SaasTenantActivationReissueResult implements Serializable {
    private static final long serialVersionUID = 1L;
    private String requestId;
    private String tenantId;
    private Long userId;
    private String activationToken;
    private long activationExpiresAtEpochMs;

    public SaasTenantActivationReissueResult() {
    }

    public SaasTenantActivationReissueResult(String requestId, String tenantId, Long userId,
            String activationToken, long activationExpiresAtEpochMs) {
        this.requestId = requestId;
        this.tenantId = tenantId;
        this.userId = userId;
        this.activationToken = activationToken;
        this.activationExpiresAtEpochMs = activationExpiresAtEpochMs;
    }

    public String getRequestId() { return requestId; }
    public void setRequestId(String requestId) { this.requestId = requestId; }
    public String getTenantId() { return tenantId; }
    public void setTenantId(String tenantId) { this.tenantId = tenantId; }
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public String getActivationToken() { return activationToken; }
    public void setActivationToken(String activationToken) { this.activationToken = activationToken; }
    public long getActivationExpiresAtEpochMs() { return activationExpiresAtEpochMs; }
    public void setActivationExpiresAtEpochMs(long activationExpiresAtEpochMs) {
        this.activationExpiresAtEpochMs = activationExpiresAtEpochMs;
    }
}

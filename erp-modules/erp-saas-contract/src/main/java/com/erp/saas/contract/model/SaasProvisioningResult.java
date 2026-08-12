package com.erp.saas.contract.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.io.Serializable;

@JsonIgnoreProperties(ignoreUnknown = true)
public class SaasProvisioningResult implements Serializable {
    private static final long serialVersionUID = 1L;
    private String requestId;
    private String tenantId;
    private boolean success;
    private String message;
    private boolean activationRequired;
    private long completedAtEpochMs;

    public SaasProvisioningResult() {
    }

    public SaasProvisioningResult(String requestId, String tenantId, boolean success, String message,
            boolean activationRequired, long completedAtEpochMs) {
        this.requestId = requestId;
        this.tenantId = tenantId;
        this.success = success;
        this.message = message;
        this.activationRequired = activationRequired;
        this.completedAtEpochMs = completedAtEpochMs;
    }

    public String getRequestId() { return requestId; }
    public void setRequestId(String requestId) { this.requestId = requestId; }
    public String getTenantId() { return tenantId; }
    public void setTenantId(String tenantId) { this.tenantId = tenantId; }
    public boolean isSuccess() { return success; }
    public void setSuccess(boolean success) { this.success = success; }
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
    public boolean isActivationRequired() { return activationRequired; }
    public void setActivationRequired(boolean activationRequired) { this.activationRequired = activationRequired; }
    public long getCompletedAtEpochMs() { return completedAtEpochMs; }
    public void setCompletedAtEpochMs(long completedAtEpochMs) { this.completedAtEpochMs = completedAtEpochMs; }
}

package com.erp.saas.contract.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.io.Serializable;

@JsonIgnoreProperties(ignoreUnknown = true)
public class SaasTenantStoragePurgeResult implements Serializable {
    private static final long serialVersionUID = 1L;

    private String requestId;
    private String tenantId;
    private int objectsDeleted;
    private boolean replayed;

    public SaasTenantStoragePurgeResult() {
    }

    public SaasTenantStoragePurgeResult(String requestId, String tenantId,
            int objectsDeleted, boolean replayed) {
        this.requestId = requestId;
        this.tenantId = tenantId;
        this.objectsDeleted = objectsDeleted;
        this.replayed = replayed;
    }

    public String getRequestId() { return requestId; }
    public void setRequestId(String requestId) { this.requestId = requestId; }
    public String getTenantId() { return tenantId; }
    public void setTenantId(String tenantId) { this.tenantId = tenantId; }
    public int getObjectsDeleted() { return objectsDeleted; }
    public void setObjectsDeleted(int objectsDeleted) { this.objectsDeleted = objectsDeleted; }
    public boolean isReplayed() { return replayed; }
    public void setReplayed(boolean replayed) { this.replayed = replayed; }
}

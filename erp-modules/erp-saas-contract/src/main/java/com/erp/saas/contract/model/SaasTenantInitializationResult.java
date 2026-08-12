package com.erp.saas.contract.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.io.Serializable;

@JsonIgnoreProperties(ignoreUnknown = true)
public class SaasTenantInitializationResult implements Serializable {
    private static final long serialVersionUID = 1L;
    private String requestId;
    private String tenantId;
    private Long tenantRecordId;
    private Long companyId;
    private Long deptId;
    private Long roleId;
    private Long userId;
    private String activationToken;
    private long activationExpiresAtEpochMs;
    private boolean replayed;

    public SaasTenantInitializationResult() {
    }

    public SaasTenantInitializationResult(String requestId, String tenantId, Long tenantRecordId,
            Long companyId, Long deptId, Long roleId, Long userId, String activationToken,
            long activationExpiresAtEpochMs, boolean replayed) {
        this.requestId = requestId;
        this.tenantId = tenantId;
        this.tenantRecordId = tenantRecordId;
        this.companyId = companyId;
        this.deptId = deptId;
        this.roleId = roleId;
        this.userId = userId;
        this.activationToken = activationToken;
        this.activationExpiresAtEpochMs = activationExpiresAtEpochMs;
        this.replayed = replayed;
    }

    public String getRequestId() { return requestId; }
    public void setRequestId(String requestId) { this.requestId = requestId; }
    public String getTenantId() { return tenantId; }
    public void setTenantId(String tenantId) { this.tenantId = tenantId; }
    public Long getTenantRecordId() { return tenantRecordId; }
    public void setTenantRecordId(Long tenantRecordId) { this.tenantRecordId = tenantRecordId; }
    public Long getCompanyId() { return companyId; }
    public void setCompanyId(Long companyId) { this.companyId = companyId; }
    public Long getDeptId() { return deptId; }
    public void setDeptId(Long deptId) { this.deptId = deptId; }
    public Long getRoleId() { return roleId; }
    public void setRoleId(Long roleId) { this.roleId = roleId; }
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public String getActivationToken() { return activationToken; }
    public void setActivationToken(String activationToken) { this.activationToken = activationToken; }
    public long getActivationExpiresAtEpochMs() { return activationExpiresAtEpochMs; }
    public void setActivationExpiresAtEpochMs(long activationExpiresAtEpochMs) {
        this.activationExpiresAtEpochMs = activationExpiresAtEpochMs;
    }
    public boolean isReplayed() { return replayed; }
    public void setReplayed(boolean replayed) { this.replayed = replayed; }
}

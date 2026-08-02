package com.erp.saas.contract.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.io.Serializable;

@JsonIgnoreProperties(ignoreUnknown = true)
public class SaasTenantInitializationRequest implements Serializable {
    private static final long serialVersionUID = 1L;
    private String requestId;
    private String tenantId;
    private String tenantName;
    private String companyCode;
    private String companyName;
    private String adminUsername;
    private String adminDisplayName;
    private String adminEmail;

    public SaasTenantInitializationRequest() {
    }

    public SaasTenantInitializationRequest(String requestId, String tenantId, String tenantName,
            String companyCode, String companyName, String adminUsername,
            String adminDisplayName, String adminEmail) {
        this.requestId = requestId;
        this.tenantId = tenantId;
        this.tenantName = tenantName;
        this.companyCode = companyCode;
        this.companyName = companyName;
        this.adminUsername = adminUsername;
        this.adminDisplayName = adminDisplayName;
        this.adminEmail = adminEmail;
    }

    public String getRequestId() { return requestId; }
    public void setRequestId(String requestId) { this.requestId = requestId; }
    public String getTenantId() { return tenantId; }
    public void setTenantId(String tenantId) { this.tenantId = tenantId; }
    public String getTenantName() { return tenantName; }
    public void setTenantName(String tenantName) { this.tenantName = tenantName; }
    public String getCompanyCode() { return companyCode; }
    public void setCompanyCode(String companyCode) { this.companyCode = companyCode; }
    public String getCompanyName() { return companyName; }
    public void setCompanyName(String companyName) { this.companyName = companyName; }
    public String getAdminUsername() { return adminUsername; }
    public void setAdminUsername(String adminUsername) { this.adminUsername = adminUsername; }
    public String getAdminDisplayName() { return adminDisplayName; }
    public void setAdminDisplayName(String adminDisplayName) { this.adminDisplayName = adminDisplayName; }
    public String getAdminEmail() { return adminEmail; }
    public void setAdminEmail(String adminEmail) { this.adminEmail = adminEmail; }
}

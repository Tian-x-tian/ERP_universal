package com.erp.common.client.internal;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 平台内部调用配置。
 */
@ConfigurationProperties(prefix = "erp.internal")
public class InternalSystemClientProperties {
    private static final String DEFAULT_SYSTEM_BASE_URL = "http://erp-system";
    private static final String DEFAULT_WORKFLOW_BASE_URL = "http://erp-workflow";
    private static final String DEFAULT_BUSINESS_BASE_URL = "http://erp-business";

    private String authSignatureSecret = "erp-internal-auth-signature-2026";
    private String systemBaseUrl = DEFAULT_SYSTEM_BASE_URL;
    private String workflowBaseUrl = DEFAULT_WORKFLOW_BASE_URL;
    private String businessBaseUrl = DEFAULT_BUSINESS_BASE_URL;
    private Long serviceUserId = 0L;
    private String serviceUserName = "erp-service";
    private String serviceTenantId = "000000";
    private Integer serviceTokenVersion = 0;
    private Long serviceExpiresAt = 4102444800000L;

    public String getAuthSignatureSecret() {
        return authSignatureSecret;
    }

    public void setAuthSignatureSecret(String authSignatureSecret) {
        this.authSignatureSecret = authSignatureSecret;
    }

    public String getSystemBaseUrl() {
        return systemBaseUrl;
    }

    public void setSystemBaseUrl(String systemBaseUrl) {
        this.systemBaseUrl = systemBaseUrl;
    }

    public String getWorkflowBaseUrl() {
        return workflowBaseUrl;
    }

    public void setWorkflowBaseUrl(String workflowBaseUrl) {
        this.workflowBaseUrl = workflowBaseUrl;
    }

    public String getBusinessBaseUrl() {
        return businessBaseUrl;
    }

    public void setBusinessBaseUrl(String businessBaseUrl) {
        this.businessBaseUrl = businessBaseUrl;
    }

    public String resolveSystemBaseUrl() {
        return hasText(systemBaseUrl) ? systemBaseUrl.trim() : DEFAULT_SYSTEM_BASE_URL;
    }

    public String resolveWorkflowBaseUrl() {
        return hasText(workflowBaseUrl) ? workflowBaseUrl.trim() : DEFAULT_WORKFLOW_BASE_URL;
    }

    public String resolveBusinessBaseUrl() {
        return hasText(businessBaseUrl) ? businessBaseUrl.trim() : DEFAULT_BUSINESS_BASE_URL;
    }

    public Long getServiceUserId() {
        return serviceUserId;
    }

    public void setServiceUserId(Long serviceUserId) {
        this.serviceUserId = serviceUserId;
    }

    public String getServiceUserName() {
        return serviceUserName;
    }

    public void setServiceUserName(String serviceUserName) {
        this.serviceUserName = serviceUserName;
    }

    public String getServiceTenantId() {
        return serviceTenantId;
    }

    public void setServiceTenantId(String serviceTenantId) {
        this.serviceTenantId = serviceTenantId;
    }

    public Integer getServiceTokenVersion() {
        return serviceTokenVersion;
    }

    public void setServiceTokenVersion(Integer serviceTokenVersion) {
        this.serviceTokenVersion = serviceTokenVersion;
    }

    public Long getServiceExpiresAt() {
        return serviceExpiresAt;
    }

    public void setServiceExpiresAt(Long serviceExpiresAt) {
        this.serviceExpiresAt = serviceExpiresAt;
    }

    private boolean hasText(String value) {
        return value != null && !value.trim().isEmpty();
    }
}

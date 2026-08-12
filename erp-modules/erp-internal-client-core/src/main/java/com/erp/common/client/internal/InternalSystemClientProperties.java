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
    private static final String DEFAULT_SAAS_BASE_URL = "http://erp-saas-control";
    private static final int DEFAULT_SAAS_CONNECT_TIMEOUT_MS = 2000;
    private static final int DEFAULT_SAAS_READ_TIMEOUT_MS = 5000;

    private String authSignatureSecret;
    private String systemBaseUrl = DEFAULT_SYSTEM_BASE_URL;
    private String workflowBaseUrl = DEFAULT_WORKFLOW_BASE_URL;
    private String businessBaseUrl = DEFAULT_BUSINESS_BASE_URL;
    private String saasBaseUrl = DEFAULT_SAAS_BASE_URL;
    private Integer saasConnectTimeoutMs = DEFAULT_SAAS_CONNECT_TIMEOUT_MS;
    private Integer saasReadTimeoutMs = DEFAULT_SAAS_READ_TIMEOUT_MS;
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

    /**
     * 解析内部调用签名密钥；缺失时直接失败，避免仓库内默认密钥或空签名继续参与内部调用。
     *
     * @return 修剪后的签名密钥
     */
    public String resolveAuthSignatureSecret() {
        if (!hasText(authSignatureSecret)) {
            throw new IllegalStateException("内部签名密钥 erp.internal.auth-signature-secret 未配置，无法生成内部签名。");
        }
        return authSignatureSecret.trim();
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

    public String getSaasBaseUrl() {
        return saasBaseUrl;
    }

    public void setSaasBaseUrl(String saasBaseUrl) {
        this.saasBaseUrl = saasBaseUrl;
    }

    public String resolveSaasBaseUrl() {
        return hasText(saasBaseUrl) ? saasBaseUrl.trim() : DEFAULT_SAAS_BASE_URL;
    }

    public Integer getSaasConnectTimeoutMs() {
        return saasConnectTimeoutMs;
    }

    public void setSaasConnectTimeoutMs(Integer saasConnectTimeoutMs) {
        this.saasConnectTimeoutMs = saasConnectTimeoutMs;
    }

    public int resolveSaasConnectTimeoutMs() {
        return requirePositive(saasConnectTimeoutMs, "saasConnectTimeoutMs");
    }

    public Integer getSaasReadTimeoutMs() {
        return saasReadTimeoutMs;
    }

    public void setSaasReadTimeoutMs(Integer saasReadTimeoutMs) {
        this.saasReadTimeoutMs = saasReadTimeoutMs;
    }

    public int resolveSaasReadTimeoutMs() {
        return requirePositive(saasReadTimeoutMs, "saasReadTimeoutMs");
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

    private int requirePositive(Integer value, String field) {
        if (value == null || value <= 0) {
            throw new IllegalArgumentException(field + " must be positive");
        }
        return value;
    }
}

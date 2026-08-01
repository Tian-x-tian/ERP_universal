package com.erp.saas.contract.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public class SaasEntitlementSnapshot implements Serializable {
    private static final long serialVersionUID = 1L;
    private String tenantId;
    private TenantLifecycleState lifecycleState;
    private DeploymentMode deploymentMode;
    private SubscriptionState subscriptionState;
    private String planCode;
    private long version;
    private long issuedAtEpochMs;
    private long expiresAtEpochMs;
    private List<SaasFeatureGrant> featureGrants = new ArrayList<>();
    private List<SaasQuotaLimit> quotaLimits = new ArrayList<>();
    private String signatureKeyId;
    private String signature;

    public SaasEntitlementSnapshot() {
    }

    public String getTenantId() { return tenantId; }
    public void setTenantId(String tenantId) { this.tenantId = tenantId; }
    public TenantLifecycleState getLifecycleState() { return lifecycleState; }
    public void setLifecycleState(TenantLifecycleState lifecycleState) { this.lifecycleState = lifecycleState; }
    public DeploymentMode getDeploymentMode() { return deploymentMode; }
    public void setDeploymentMode(DeploymentMode deploymentMode) { this.deploymentMode = deploymentMode; }
    public SubscriptionState getSubscriptionState() { return subscriptionState; }
    public void setSubscriptionState(SubscriptionState subscriptionState) { this.subscriptionState = subscriptionState; }
    public String getPlanCode() { return planCode; }
    public void setPlanCode(String planCode) { this.planCode = planCode; }
    public long getVersion() { return version; }
    public void setVersion(long version) { this.version = version; }
    public long getIssuedAtEpochMs() { return issuedAtEpochMs; }
    public void setIssuedAtEpochMs(long issuedAtEpochMs) { this.issuedAtEpochMs = issuedAtEpochMs; }
    public long getExpiresAtEpochMs() { return expiresAtEpochMs; }
    public void setExpiresAtEpochMs(long expiresAtEpochMs) { this.expiresAtEpochMs = expiresAtEpochMs; }
    public List<SaasFeatureGrant> getFeatureGrants() { return featureGrants; }
    public void setFeatureGrants(List<SaasFeatureGrant> featureGrants) {
        this.featureGrants = featureGrants == null ? new ArrayList<>() : new ArrayList<>(featureGrants);
    }
    public List<SaasQuotaLimit> getQuotaLimits() { return quotaLimits; }
    public void setQuotaLimits(List<SaasQuotaLimit> quotaLimits) {
        this.quotaLimits = quotaLimits == null ? new ArrayList<>() : new ArrayList<>(quotaLimits);
    }
    public String getSignatureKeyId() { return signatureKeyId; }
    public void setSignatureKeyId(String signatureKeyId) { this.signatureKeyId = signatureKeyId; }
    public String getSignature() { return signature; }
    public void setSignature(String signature) { this.signature = signature; }
}

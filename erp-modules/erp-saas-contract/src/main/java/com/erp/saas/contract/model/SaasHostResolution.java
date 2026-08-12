package com.erp.saas.contract.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.io.Serializable;

@JsonIgnoreProperties(ignoreUnknown = true)
public class SaasHostResolution implements Serializable {
    private static final long serialVersionUID = 1L;
    private String host;
    private String tenantId;
    private DeploymentMode deploymentMode;
    private TenantLifecycleState lifecycleState;
    private boolean verified;

    public SaasHostResolution() {
    }

    public SaasHostResolution(String host, String tenantId, DeploymentMode deploymentMode,
            TenantLifecycleState lifecycleState, boolean verified) {
        this.host = host;
        this.tenantId = tenantId;
        this.deploymentMode = deploymentMode;
        this.lifecycleState = lifecycleState;
        this.verified = verified;
    }

    public String getHost() { return host; }
    public void setHost(String host) { this.host = host; }
    public String getTenantId() { return tenantId; }
    public void setTenantId(String tenantId) { this.tenantId = tenantId; }
    public DeploymentMode getDeploymentMode() { return deploymentMode; }
    public void setDeploymentMode(DeploymentMode deploymentMode) { this.deploymentMode = deploymentMode; }
    public TenantLifecycleState getLifecycleState() { return lifecycleState; }
    public void setLifecycleState(TenantLifecycleState lifecycleState) { this.lifecycleState = lifecycleState; }
    public boolean isVerified() { return verified; }
    public void setVerified(boolean verified) { this.verified = verified; }
}

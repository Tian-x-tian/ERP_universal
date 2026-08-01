package com.erp.saas.contract.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.io.Serializable;

@JsonIgnoreProperties(ignoreUnknown = true)
public class SaasProvisioningRequest implements Serializable {
    private static final long serialVersionUID = 1L;
    private String requestId;
    private String tenantId;
    private DeploymentMode deploymentMode;
    private String planCode;

    public SaasProvisioningRequest() {
    }

    public SaasProvisioningRequest(String requestId, String tenantId, DeploymentMode deploymentMode, String planCode) {
        this.requestId = requestId;
        this.tenantId = tenantId;
        this.deploymentMode = deploymentMode;
        this.planCode = planCode;
    }

    public String getRequestId() { return requestId; }
    public void setRequestId(String requestId) { this.requestId = requestId; }
    public String getTenantId() { return tenantId; }
    public void setTenantId(String tenantId) { this.tenantId = tenantId; }
    public DeploymentMode getDeploymentMode() { return deploymentMode; }
    public void setDeploymentMode(DeploymentMode deploymentMode) { this.deploymentMode = deploymentMode; }
    public String getPlanCode() { return planCode; }
    public void setPlanCode(String planCode) { this.planCode = planCode; }
}

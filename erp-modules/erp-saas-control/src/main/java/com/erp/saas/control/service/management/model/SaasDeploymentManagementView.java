package com.erp.saas.control.service.management.model;

import com.erp.saas.contract.model.DeploymentMode;
import com.erp.saas.control.domain.DeploymentStatus;

import java.time.LocalDateTime;

public record SaasDeploymentManagementView(Long deploymentId, String tenantId,
        DeploymentMode mode, DeploymentStatus status, String deploymentRef,
        String secretRef, Long versionNo, LocalDateTime updateTime) {
}

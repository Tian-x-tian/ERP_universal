package com.erp.saas.control.service.management.model;

import com.erp.saas.contract.model.DeploymentMode;
import com.erp.saas.contract.model.SubscriptionState;
import com.erp.saas.contract.model.TenantLifecycleState;
import com.erp.saas.control.domain.DeploymentStatus;
import com.erp.saas.control.domain.SaasProvisioningStatus;

import java.time.LocalDateTime;

public record SaasTenantManagementView(
        String tenantId,
        String slug,
        String tenantName,
        TenantLifecycleState lifecycleState,
        Long planId,
        String planCode,
        String planName,
        SubscriptionState subscriptionState,
        LocalDateTime subscriptionEndAt,
        LocalDateTime graceEndAt,
        Boolean nonExpiring,
        DeploymentMode deploymentMode,
        DeploymentStatus deploymentStatus,
        SaasProvisioningStatus provisioningStatus,
        Integer provisioningAttempts,
        Long versionNo,
        LocalDateTime updateTime) {
}

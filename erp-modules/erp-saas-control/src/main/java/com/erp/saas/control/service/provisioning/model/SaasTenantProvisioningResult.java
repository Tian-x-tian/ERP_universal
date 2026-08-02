package com.erp.saas.control.service.provisioning.model;

import com.erp.saas.contract.model.TenantLifecycleState;
import com.erp.saas.control.domain.SaasProvisioningStatus;

public record SaasTenantProvisioningResult(
        String requestId,
        String tenantId,
        SaasProvisioningStatus taskStatus,
        TenantLifecycleState lifecycleState,
        Long tenantRecordId,
        Long companyId,
        Long deptId,
        Long roleId,
        Long userId,
        String activationToken,
        Long activationExpiresAtEpochMs,
        boolean replayed) {
}

package com.erp.saas.control.service.lifecycle.model;

import com.erp.saas.control.service.lifecycle.SaasLifecycleValidation;

import java.time.LocalDateTime;

public record ActivateSubscriptionCommand(
        String tenantId,
        Long planId,
        Long expectedTenantVersion,
        LocalDateTime endAt,
        boolean nonExpiring,
        String operator) {
    public ActivateSubscriptionCommand {
        tenantId = SaasLifecycleValidation.tenantId(tenantId);
        planId = SaasLifecycleValidation.id(planId, "planId");
        expectedTenantVersion = SaasLifecycleValidation.version(expectedTenantVersion);
        endAt = SaasLifecycleValidation.endAt(endAt, nonExpiring);
        operator = SaasLifecycleValidation.operator(operator);
    }
}

package com.erp.saas.control.service.lifecycle.model;

import com.erp.saas.control.service.lifecycle.SaasLifecycleValidation;

public record StartTrialCommand(String tenantId, Long planId, Long expectedTenantVersion, String operator) {
    public StartTrialCommand {
        tenantId = SaasLifecycleValidation.tenantId(tenantId);
        planId = SaasLifecycleValidation.id(planId, "planId");
        expectedTenantVersion = SaasLifecycleValidation.version(expectedTenantVersion);
        operator = SaasLifecycleValidation.operator(operator);
    }
}

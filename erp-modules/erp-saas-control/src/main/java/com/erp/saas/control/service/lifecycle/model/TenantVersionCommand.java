package com.erp.saas.control.service.lifecycle.model;

import com.erp.saas.control.service.lifecycle.SaasLifecycleValidation;

public record TenantVersionCommand(String tenantId, Long expectedTenantVersion, String operator) {
    public TenantVersionCommand {
        tenantId = SaasLifecycleValidation.tenantId(tenantId);
        expectedTenantVersion = SaasLifecycleValidation.version(expectedTenantVersion);
        operator = SaasLifecycleValidation.operator(operator);
    }
}

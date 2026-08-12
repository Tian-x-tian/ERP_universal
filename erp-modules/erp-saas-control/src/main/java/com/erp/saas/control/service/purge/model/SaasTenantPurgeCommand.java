package com.erp.saas.control.service.purge.model;

import com.erp.saas.control.service.lifecycle.SaasLifecycleValidation;

import java.util.regex.Pattern;

public record SaasTenantPurgeCommand(String requestId, String tenantId,
        Long expectedTenantVersion, String confirmationTenantId, String operator) {
    private static final Pattern REQUEST_ID = Pattern.compile("[A-Za-z0-9_-]{8,128}");

    public SaasTenantPurgeCommand {
        requestId = requestId == null ? null : requestId.trim();
        if (requestId == null || !REQUEST_ID.matcher(requestId).matches()) {
            throw SaasLifecycleValidation.invalid("requestId has an invalid format");
        }
        tenantId = SaasLifecycleValidation.tenantId(tenantId);
        expectedTenantVersion = SaasLifecycleValidation.version(expectedTenantVersion);
        confirmationTenantId = SaasLifecycleValidation.tenantId(confirmationTenantId);
        if (!tenantId.equals(confirmationTenantId)) {
            throw SaasLifecycleValidation.invalid("Typed tenant confirmation does not match");
        }
        operator = SaasLifecycleValidation.operator(operator);
    }
}

package com.erp.saas.control.service.domain.model;

import com.erp.saas.control.domain.DomainVerificationMethod;
import com.erp.saas.control.service.domain.SaasDomainValidation;

public record RegisterDomainCommand(String tenantId, String host,
        DomainVerificationMethod verificationMethod, String operator) {
    public RegisterDomainCommand {
        tenantId = SaasDomainValidation.tenantId(tenantId);
        host = SaasDomainValidation.host(host);
        verificationMethod = SaasDomainValidation.verificationMethod(verificationMethod);
        operator = SaasDomainValidation.operator(operator);
    }
}

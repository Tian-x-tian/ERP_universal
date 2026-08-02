package com.erp.saas.control.service.domain.model;

import com.erp.saas.control.service.domain.SaasDomainValidation;

public record VerifyDomainCommand(Long domainId, Long expectedVersion, String operator) {
    public VerifyDomainCommand {
        domainId = SaasDomainValidation.id(domainId, "domainId");
        expectedVersion = SaasDomainValidation.version(expectedVersion);
        operator = SaasDomainValidation.operator(operator);
    }
}

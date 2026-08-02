package com.erp.saas.control.service.domain.model;

import com.erp.saas.control.service.domain.SaasDomainValidation;

public record RevokeDomainCommand(Long domainId, Long expectedVersion, String operator) {
    public RevokeDomainCommand {
        domainId = SaasDomainValidation.id(domainId, "domainId");
        expectedVersion = SaasDomainValidation.version(expectedVersion);
        operator = SaasDomainValidation.operator(operator);
    }
}

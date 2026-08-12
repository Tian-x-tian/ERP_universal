package com.erp.saas.control.service.domain.model;

import com.erp.saas.control.service.domain.SaasDomainValidation;

public record TransferDomainCommand(Long domainId, Long expectedVersion,
        String targetTenantId, String operator) {
    public TransferDomainCommand {
        domainId = SaasDomainValidation.id(domainId, "domainId");
        expectedVersion = SaasDomainValidation.version(expectedVersion);
        targetTenantId = SaasDomainValidation.tenantId(targetTenantId);
        operator = SaasDomainValidation.operator(operator);
    }
}

package com.erp.saas.control.service.management.model;

import com.erp.saas.control.domain.DomainVerificationMethod;
import com.erp.saas.control.domain.DomainVerificationState;

import java.time.LocalDateTime;

public record SaasDomainManagementView(Long domainId, String tenantId, String host,
        DomainVerificationState verificationState, DomainVerificationMethod verificationMethod,
        LocalDateTime verifiedAt, LocalDateTime revokedAt, Long versionNo, LocalDateTime updateTime) {
}

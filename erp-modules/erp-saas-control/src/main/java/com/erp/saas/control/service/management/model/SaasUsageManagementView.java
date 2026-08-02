package com.erp.saas.control.service.management.model;

import java.time.LocalDateTime;

public record SaasUsageManagementView(Long usageSummaryId, String tenantId, String metricKey,
        LocalDateTime periodStart, Long usedAmount, LocalDateTime lastOccurredAt,
        Long versionNo, LocalDateTime updateTime) {
}

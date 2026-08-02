package com.erp.saas.control.service.management.model;

import com.erp.saas.control.domain.QuotaPeriodType;

public record SaasPlanQuotaManagementView(String quotaKey, Long limitValue,
        QuotaPeriodType periodType) {
}

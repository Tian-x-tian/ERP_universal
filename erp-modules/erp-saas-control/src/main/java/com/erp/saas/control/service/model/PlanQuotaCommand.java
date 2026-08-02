package com.erp.saas.control.service.model;

import com.erp.saas.control.domain.QuotaPeriodType;
import com.erp.saas.control.service.SaasCatalogValidation;

public record PlanQuotaCommand(String quotaKey, Long limitValue, QuotaPeriodType periodType) {

    public PlanQuotaCommand {
        quotaKey = SaasCatalogValidation.knownQuotaKey(quotaKey);
        limitValue = SaasCatalogValidation.nonNegative(limitValue, "limitValue");
        SaasCatalogValidation.quotaPeriod(quotaKey, periodType);
    }
}

package com.erp.saas.control.service.model;

import com.erp.saas.control.service.SaasCatalogValidation;

import java.time.LocalDateTime;

public record QuotaOverrideCommand(
        String tenantId,
        String quotaKey,
        Long limitValue,
        LocalDateTime effectiveFrom,
        LocalDateTime effectiveUntil,
        String reason) {

    public QuotaOverrideCommand {
        tenantId = SaasCatalogValidation.tenantId(tenantId);
        quotaKey = SaasCatalogValidation.knownQuotaKey(quotaKey);
        limitValue = SaasCatalogValidation.nonNegative(limitValue, "limitValue");
        effectiveFrom = SaasCatalogValidation.required(effectiveFrom, "effectiveFrom");
        SaasCatalogValidation.window(effectiveFrom, effectiveUntil);
        reason = SaasCatalogValidation.optionalDescription(reason, "reason");
    }
}

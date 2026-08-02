package com.erp.saas.control.service.model;

import com.erp.saas.control.domain.FeatureOverrideState;
import com.erp.saas.control.service.SaasCatalogValidation;

import java.time.LocalDateTime;

public record FeatureOverrideCommand(
        String tenantId,
        String featureKey,
        FeatureOverrideState overrideState,
        LocalDateTime effectiveFrom,
        LocalDateTime effectiveUntil,
        String reason) {

    public FeatureOverrideCommand {
        tenantId = SaasCatalogValidation.tenantId(tenantId);
        featureKey = SaasCatalogValidation.featureKey(featureKey);
        overrideState = SaasCatalogValidation.required(overrideState, "overrideState");
        effectiveFrom = SaasCatalogValidation.required(effectiveFrom, "effectiveFrom");
        SaasCatalogValidation.window(effectiveFrom, effectiveUntil);
        reason = SaasCatalogValidation.optionalDescription(reason, "reason");
    }
}

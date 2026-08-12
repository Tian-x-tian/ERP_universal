package com.erp.saas.control.service.model;

import com.erp.saas.control.service.SaasCatalogValidation;

public record PlanDraftCommand(
        String planCode,
        Integer planVersion,
        String planName,
        Integer trialDays,
        Integer graceDays,
        String description) {

    public PlanDraftCommand {
        planCode = SaasCatalogValidation.planCode(planCode);
        planVersion = SaasCatalogValidation.range(planVersion, "planVersion", 1, Integer.MAX_VALUE);
        planName = SaasCatalogValidation.name(planName, "planName");
        trialDays = SaasCatalogValidation.range(trialDays, "trialDays", 0, 3650);
        graceDays = SaasCatalogValidation.range(graceDays, "graceDays", 0, 3650);
        description = SaasCatalogValidation.optionalDescription(description, "description");
    }
}

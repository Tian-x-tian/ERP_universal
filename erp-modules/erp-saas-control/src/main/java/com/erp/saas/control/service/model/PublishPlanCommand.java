package com.erp.saas.control.service.model;

import com.erp.saas.control.service.SaasCatalogValidation;

public record PublishPlanCommand(
        Long planId,
        Long expectedPlanVersion,
        Long expectedActivePlanId,
        Long expectedActivePlanVersion) {

    public PublishPlanCommand {
        planId = SaasCatalogValidation.required(planId, "planId");
        expectedPlanVersion = SaasCatalogValidation.required(expectedPlanVersion, "expectedPlanVersion");
        if ((expectedActivePlanId == null) != (expectedActivePlanVersion == null)) {
            throw SaasCatalogValidation.invalid(
                    "expectedActivePlanId and expectedActivePlanVersion must both be null or both be non-null");
        }
    }
}

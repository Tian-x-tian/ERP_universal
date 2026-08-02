package com.erp.saas.control.service.model;

import com.erp.saas.control.service.SaasCatalogValidation;

public record PlanFeatureGrantCommand(String featureKey, Boolean granted) {

    public PlanFeatureGrantCommand {
        featureKey = SaasCatalogValidation.featureKey(featureKey);
        granted = SaasCatalogValidation.required(granted, "granted");
    }
}

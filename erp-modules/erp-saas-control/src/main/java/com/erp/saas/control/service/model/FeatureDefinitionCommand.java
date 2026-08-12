package com.erp.saas.control.service.model;

import com.erp.saas.control.domain.FeatureStatus;
import com.erp.saas.control.service.SaasCatalogValidation;

public record FeatureDefinitionCommand(
        String featureKey,
        String featureName,
        FeatureStatus status,
        String description) {

    public FeatureDefinitionCommand {
        featureKey = SaasCatalogValidation.featureKey(featureKey);
        featureName = SaasCatalogValidation.name(featureName, "featureName");
        status = SaasCatalogValidation.required(status, "status");
        description = SaasCatalogValidation.optionalDescription(description, "description");
    }
}

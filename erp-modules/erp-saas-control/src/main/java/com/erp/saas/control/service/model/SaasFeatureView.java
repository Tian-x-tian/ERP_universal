package com.erp.saas.control.service.model;

import com.erp.saas.control.domain.FeatureStatus;

public record SaasFeatureView(
        Long featureId,
        String featureKey,
        String featureName,
        FeatureStatus status,
        String description,
        Long versionNo) {
}

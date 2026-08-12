package com.erp.saas.control.service.management.model;

import com.erp.saas.control.domain.FeatureStatus;

public record SaasFeatureManagementView(Long featureId, String featureKey, String featureName,
        FeatureStatus status, String description, Long versionNo) {
}

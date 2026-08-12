package com.erp.saas.control.service;

import com.erp.saas.control.service.model.FeatureOverrideCommand;
import com.erp.saas.control.service.model.EffectiveTenantEntitlements;
import com.erp.saas.control.service.model.QuotaOverrideCommand;

public interface SaasTenantEntitlementService {
    Long addFeatureOverride(FeatureOverrideCommand command, String operator);

    Long addQuotaOverride(QuotaOverrideCommand command, String operator);

    void deleteFutureFeatureOverride(Long overrideId, Long expectedVersion, String operator);

    void deleteFutureQuotaOverride(Long overrideId, Long expectedVersion, String operator);

    EffectiveTenantEntitlements effectiveEntitlements(String tenantId);
}

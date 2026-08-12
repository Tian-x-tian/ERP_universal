package com.erp.saas.control.service.model;

import com.erp.saas.control.service.SaasCatalogException;
import com.erp.saas.control.service.SaasCatalogValidation;

import java.util.Collections;
import java.util.SortedMap;
import java.util.TreeMap;

public record EffectiveTenantEntitlements(
        String tenantId,
        Long subscriptionId,
        Long planId,
        SortedMap<String, Boolean> features,
        SortedMap<String, QuotaEntitlement> quotas) {

    public EffectiveTenantEntitlements {
        tenantId = SaasCatalogValidation.tenantId(tenantId);
        if ((subscriptionId == null) != (planId == null)) {
            throw SaasCatalogValidation.invalid("subscriptionId and planId must both be null or both be non-null");
        }
        features = immutableFeatures(features);
        quotas = immutableQuotas(quotas);
    }

    public Boolean isFeatureEnabled(String featureKey) {
        String normalized = SaasCatalogValidation.featureKey(featureKey);
        Boolean enabled = features.get(normalized);
        if (enabled == null) {
            throw new SaasCatalogException(
                    SaasCatalogException.ErrorCode.UNKNOWN_FEATURE_KEY, "Unknown featureKey: " + normalized);
        }
        return enabled;
    }

    public QuotaEntitlement quotaLimit(String quotaKey) {
        String normalized = quotaKey == null ? null : quotaKey.trim();
        QuotaEntitlement entitlement = normalized == null ? null : quotas.get(normalized);
        if (entitlement == null) {
            throw new SaasCatalogException(
                    SaasCatalogException.ErrorCode.UNKNOWN_QUOTA_KEY, "Unknown quotaKey: " + normalized);
        }
        return entitlement;
    }

    private static SortedMap<String, Boolean> immutableFeatures(SortedMap<String, Boolean> source) {
        SaasCatalogValidation.required(source, "features");
        TreeMap<String, Boolean> copy = new TreeMap<>();
        source.forEach((key, value) -> copy.put(
                SaasCatalogValidation.featureKey(key), SaasCatalogValidation.required(value, "feature value")));
        return Collections.unmodifiableSortedMap(copy);
    }

    private static SortedMap<String, QuotaEntitlement> immutableQuotas(
            SortedMap<String, QuotaEntitlement> source) {
        SaasCatalogValidation.required(source, "quotas");
        TreeMap<String, QuotaEntitlement> copy = new TreeMap<>();
        source.forEach((key, value) -> copy.put(
                SaasCatalogValidation.knownQuotaKey(key), SaasCatalogValidation.required(value, "quota value")));
        return Collections.unmodifiableSortedMap(copy);
    }
}

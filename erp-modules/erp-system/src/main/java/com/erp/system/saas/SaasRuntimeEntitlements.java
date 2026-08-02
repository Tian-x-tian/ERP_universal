package com.erp.system.saas;

import com.erp.saas.contract.model.TenantLifecycleState;

import java.util.Collections;
import java.util.Map;
import java.util.TreeMap;

public record SaasRuntimeEntitlements(
        String tenantId,
        TenantLifecycleState lifecycleState,
        long snapshotVersion,
        SaasRuntimeSource source,
        boolean stale,
        boolean loginAllowed,
        boolean writeAllowed,
        Map<String, Boolean> features,
        Map<String, Long> quotas) {

    public SaasRuntimeEntitlements {
        features = immutable(features);
        quotas = immutable(quotas);
    }

    public boolean readOnly() { return !writeAllowed; }

    public boolean featureEnabled(String featureKey) {
        return featureKey != null && Boolean.TRUE.equals(features.get(featureKey.trim()));
    }

    public Long quotaLimit(String quotaKey) {
        if (quotaKey == null) return Long.valueOf(0L);
        String normalized = quotaKey.trim();
        return quotas.containsKey(normalized) ? quotas.get(normalized) : Long.valueOf(0L);
    }

    private static <T> Map<String, T> immutable(Map<String, T> source) {
        TreeMap<String, T> copy = new TreeMap<>();
        if (source != null) copy.putAll(source);
        return Collections.unmodifiableSortedMap(copy);
    }
}

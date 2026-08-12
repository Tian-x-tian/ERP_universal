package com.erp.saas.contract.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.io.Serializable;
import java.util.Collections;
import java.util.Map;
import java.util.TreeMap;

@JsonIgnoreProperties(ignoreUnknown = true)
public class SaasRuntimeAccess implements Serializable {
    private static final long serialVersionUID = 1L;

    private String tenantId;
    private TenantLifecycleState lifecycleState;
    private boolean stale;
    private boolean loginAllowed;
    private boolean writeAllowed;
    private Map<String, Boolean> features = Collections.emptyMap();

    public SaasRuntimeAccess() {
    }

    public SaasRuntimeAccess(String tenantId, TenantLifecycleState lifecycleState,
            boolean stale, boolean loginAllowed, boolean writeAllowed) {
        this(tenantId, lifecycleState, stale, loginAllowed, writeAllowed, Collections.emptyMap());
    }

    public SaasRuntimeAccess(String tenantId, TenantLifecycleState lifecycleState,
            boolean stale, boolean loginAllowed, boolean writeAllowed, Map<String, Boolean> features) {
        this.tenantId = tenantId;
        this.lifecycleState = lifecycleState;
        this.stale = stale;
        this.loginAllowed = loginAllowed;
        this.writeAllowed = writeAllowed;
        setFeatures(features);
    }

    public String getTenantId() {
        return tenantId;
    }

    public void setTenantId(String tenantId) {
        this.tenantId = tenantId;
    }

    public TenantLifecycleState getLifecycleState() {
        return lifecycleState;
    }

    public void setLifecycleState(TenantLifecycleState lifecycleState) {
        this.lifecycleState = lifecycleState;
    }

    public boolean isStale() {
        return stale;
    }

    public void setStale(boolean stale) {
        this.stale = stale;
    }

    public boolean isLoginAllowed() {
        return loginAllowed;
    }

    public void setLoginAllowed(boolean loginAllowed) {
        this.loginAllowed = loginAllowed;
    }

    public boolean isWriteAllowed() {
        return writeAllowed;
    }

    public void setWriteAllowed(boolean writeAllowed) {
        this.writeAllowed = writeAllowed;
    }

    public Map<String, Boolean> getFeatures() {
        return features;
    }

    public void setFeatures(Map<String, Boolean> features) {
        this.features = features == null
                ? Collections.emptyMap()
                : Collections.unmodifiableMap(new TreeMap<>(features));
    }

    public boolean isFeatureEnabled(String featureKey) {
        return featureKey != null && Boolean.TRUE.equals(features.get(featureKey.trim()));
    }
}

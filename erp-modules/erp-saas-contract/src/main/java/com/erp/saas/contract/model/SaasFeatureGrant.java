package com.erp.saas.contract.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.io.Serializable;

@JsonIgnoreProperties(ignoreUnknown = true)
public class SaasFeatureGrant implements Serializable {
    private static final long serialVersionUID = 1L;
    private String featureKey;
    private boolean granted;

    public SaasFeatureGrant() {
    }

    public SaasFeatureGrant(String featureKey, boolean granted) {
        this.featureKey = featureKey;
        this.granted = granted;
    }

    public String getFeatureKey() {
        return featureKey;
    }

    public void setFeatureKey(String featureKey) {
        this.featureKey = featureKey;
    }

    public boolean isGranted() {
        return granted;
    }

    public void setGranted(boolean granted) {
        this.granted = granted;
    }
}

package com.erp.platform.contract.model;

import java.io.Serializable;

/**
 * 平台 AI 动作策略项。
 */
public class PlatformAiActionPolicyItem implements Serializable {
    private static final long serialVersionUID = 1L;

    private String actionKey;
    private String actionLabel;
    private boolean enabled;
    private String riskLevel;

    public String getActionKey() {
        return actionKey;
    }

    public void setActionKey(String actionKey) {
        this.actionKey = actionKey;
    }

    public String getActionLabel() {
        return actionLabel;
    }

    public void setActionLabel(String actionLabel) {
        this.actionLabel = actionLabel;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getRiskLevel() {
        return riskLevel;
    }

    public void setRiskLevel(String riskLevel) {
        this.riskLevel = riskLevel;
    }
}

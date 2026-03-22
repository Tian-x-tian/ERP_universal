package com.erp.system.ai.model;

import java.io.Serializable;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * AI 待确认动作对象。
 */
public class AiPendingAction implements Serializable {
    private static final long serialVersionUID = 1L;

    /**
     * 动作编码。
     */
    private String actionKey;

    /**
     * 动作展示名称。
     */
    private String actionLabel;

    /**
     * 风险等级。
     */
    private String riskLevel;

    /**
     * 目标摘要。
     */
    private String targetLabel;

    /**
     * 确认摘要说明。
     */
    private String summary;

    /**
     * 服务端签名确认票据。
     */
    private String confirmationToken;

    /**
     * 服务端内部动作参数。
     */
    private Map<String, Object> actionArgs = new LinkedHashMap<>();

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

    public String getRiskLevel() {
        return riskLevel;
    }

    public void setRiskLevel(String riskLevel) {
        this.riskLevel = riskLevel;
    }

    public String getTargetLabel() {
        return targetLabel;
    }

    public void setTargetLabel(String targetLabel) {
        this.targetLabel = targetLabel;
    }

    public String getSummary() {
        return summary;
    }

    public void setSummary(String summary) {
        this.summary = summary;
    }

    public String getConfirmationToken() {
        return confirmationToken;
    }

    public void setConfirmationToken(String confirmationToken) {
        this.confirmationToken = confirmationToken;
    }

    public Map<String, Object> getActionArgs() {
        return actionArgs;
    }

    public void setActionArgs(Map<String, Object> actionArgs) {
        this.actionArgs = actionArgs;
    }
}

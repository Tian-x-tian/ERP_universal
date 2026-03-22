package com.erp.system.ai.model;

import java.io.Serializable;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * AI 动作确认票据载荷。
 */
public class AiConfirmationPayload implements Serializable {
    private static final long serialVersionUID = 1L;

    /**
     * 用户ID。
     */
    private Long userId;

    /**
     * 租户编号。
     */
    private String tenantId;

    /**
     * 动作编码。
     */
    private String actionKey;

    /**
     * 目标摘要。
     */
    private String targetLabel;

    /**
     * 动作参数。
     */
    private Map<String, Object> actionArgs = new LinkedHashMap<>();

    /**
     * 过期时间戳（毫秒）。
     */
    private Long expiresAt;

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getTenantId() {
        return tenantId;
    }

    public void setTenantId(String tenantId) {
        this.tenantId = tenantId;
    }

    public String getActionKey() {
        return actionKey;
    }

    public void setActionKey(String actionKey) {
        this.actionKey = actionKey;
    }

    public String getTargetLabel() {
        return targetLabel;
    }

    public void setTargetLabel(String targetLabel) {
        this.targetLabel = targetLabel;
    }

    public Map<String, Object> getActionArgs() {
        return actionArgs;
    }

    public void setActionArgs(Map<String, Object> actionArgs) {
        this.actionArgs = actionArgs;
    }

    public Long getExpiresAt() {
        return expiresAt;
    }

    public void setExpiresAt(Long expiresAt) {
        this.expiresAt = expiresAt;
    }
}

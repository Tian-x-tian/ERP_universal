package com.erp.platform.contract.model;

import java.io.Serializable;
import java.util.Date;

/**
 * 平台 AI 审计视图。
 */
public class PlatformAiAuditView implements Serializable {
    private static final long serialVersionUID = 1L;

    private Long auditId;
    private String tenantId;
    private Long userId;
    private String userName;
    private String questionType;
    private String interactionLevel;
    private String actionKey;
    private String actionConfirmed;
    private String successFlag;
    private String promptInjectionFlag;
    private String sensitiveHitFlag;
    private String requestExcerpt;
    private String responseExcerpt;
    private Long durationMs;
    private Date createTime;

    public Long getAuditId() {
        return auditId;
    }

    public void setAuditId(Long auditId) {
        this.auditId = auditId;
    }

    public String getTenantId() {
        return tenantId;
    }

    public void setTenantId(String tenantId) {
        this.tenantId = tenantId;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public String getQuestionType() {
        return questionType;
    }

    public void setQuestionType(String questionType) {
        this.questionType = questionType;
    }

    public String getInteractionLevel() {
        return interactionLevel;
    }

    public void setInteractionLevel(String interactionLevel) {
        this.interactionLevel = interactionLevel;
    }

    public String getActionKey() {
        return actionKey;
    }

    public void setActionKey(String actionKey) {
        this.actionKey = actionKey;
    }

    public String getActionConfirmed() {
        return actionConfirmed;
    }

    public void setActionConfirmed(String actionConfirmed) {
        this.actionConfirmed = actionConfirmed;
    }

    public String getSuccessFlag() {
        return successFlag;
    }

    public void setSuccessFlag(String successFlag) {
        this.successFlag = successFlag;
    }

    public String getPromptInjectionFlag() {
        return promptInjectionFlag;
    }

    public void setPromptInjectionFlag(String promptInjectionFlag) {
        this.promptInjectionFlag = promptInjectionFlag;
    }

    public String getSensitiveHitFlag() {
        return sensitiveHitFlag;
    }

    public void setSensitiveHitFlag(String sensitiveHitFlag) {
        this.sensitiveHitFlag = sensitiveHitFlag;
    }

    public String getRequestExcerpt() {
        return requestExcerpt;
    }

    public void setRequestExcerpt(String requestExcerpt) {
        this.requestExcerpt = requestExcerpt;
    }

    public String getResponseExcerpt() {
        return responseExcerpt;
    }

    public void setResponseExcerpt(String responseExcerpt) {
        this.responseExcerpt = responseExcerpt;
    }

    public Long getDurationMs() {
        return durationMs;
    }

    public void setDurationMs(Long durationMs) {
        this.durationMs = durationMs;
    }

    public Date getCreateTime() {
        return createTime;
    }

    public void setCreateTime(Date createTime) {
        this.createTime = createTime;
    }
}

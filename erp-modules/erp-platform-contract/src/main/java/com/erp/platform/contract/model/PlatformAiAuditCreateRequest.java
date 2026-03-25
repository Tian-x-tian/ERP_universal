package com.erp.platform.contract.model;

import java.io.Serializable;

/**
 * 平台 AI 审计写入请求。
 */
public class PlatformAiAuditCreateRequest implements Serializable {
    private static final long serialVersionUID = 1L;

    private String questionType;
    private String interactionLevel;
    private String actionKey;
    private Boolean actionConfirmed;
    private Boolean success;
    private Boolean promptInjectionDetected;
    private Boolean sensitiveHit;
    private String requestExcerpt;
    private String responseExcerpt;
    private Long durationMs;

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

    public Boolean getActionConfirmed() {
        return actionConfirmed;
    }

    public void setActionConfirmed(Boolean actionConfirmed) {
        this.actionConfirmed = actionConfirmed;
    }

    public Boolean getSuccess() {
        return success;
    }

    public void setSuccess(Boolean success) {
        this.success = success;
    }

    public Boolean getPromptInjectionDetected() {
        return promptInjectionDetected;
    }

    public void setPromptInjectionDetected(Boolean promptInjectionDetected) {
        this.promptInjectionDetected = promptInjectionDetected;
    }

    public Boolean getSensitiveHit() {
        return sensitiveHit;
    }

    public void setSensitiveHit(Boolean sensitiveHit) {
        this.sensitiveHit = sensitiveHit;
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
}

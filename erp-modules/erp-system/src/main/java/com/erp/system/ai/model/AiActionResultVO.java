package com.erp.system.ai.model;

import java.io.Serializable;

/**
 * AI 动作执行结果对象。
 */
public class AiActionResultVO implements Serializable {
    private static final long serialVersionUID = 1L;

    /**
     * 是否执行成功。
     */
    private boolean success;

    /**
     * 动作编码。
     */
    private String actionKey;

    /**
     * 目标摘要。
     */
    private String targetLabel;

    /**
     * 执行结果消息。
     */
    private String message;

    /**
     * 追加到聊天窗口的助手消息。
     */
    private String assistantMessage;

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
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

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getAssistantMessage() {
        return assistantMessage;
    }

    public void setAssistantMessage(String assistantMessage) {
        this.assistantMessage = assistantMessage;
    }
}

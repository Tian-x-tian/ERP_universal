package com.erp.ai.model;

import java.io.Serializable;

/**
 * AI 动作处理结果对象。
 */
public class AiActionHandleResult implements Serializable {
    private static final long serialVersionUID = 1L;

    /**
     * 返回给对话区的助手消息。
     */
    private String assistantMessage;

    /**
     * 待确认动作；为空表示无需确认。
     */
    private AiPendingAction pendingAction;

    public String getAssistantMessage() {
        return assistantMessage;
    }

    public void setAssistantMessage(String assistantMessage) {
        this.assistantMessage = assistantMessage;
    }

    public AiPendingAction getPendingAction() {
        return pendingAction;
    }

    public void setPendingAction(AiPendingAction pendingAction) {
        this.pendingAction = pendingAction;
    }
}

package com.erp.ai.model;

import java.util.ArrayList;
import java.util.List;

/**
 * AI 对话请求体。
 */
public class AiChatRequest {
    /**
     * 历史消息列表。
     */
    private List<AiChatMessage> messages = new ArrayList<>();

    /**
     * 当前页面上下文。
     */
    private AiPageContext pageContext;

    /**
     * 服务端会话ID；为空表示本轮开启新会话。
     */
    private Long sessionId;

    public List<AiChatMessage> getMessages() {
        return messages;
    }

    public void setMessages(List<AiChatMessage> messages) {
        this.messages = messages;
    }

    public AiPageContext getPageContext() {
        return pageContext;
    }

    public void setPageContext(AiPageContext pageContext) {
        this.pageContext = pageContext;
    }

    public Long getSessionId() {
        return sessionId;
    }

    public void setSessionId(Long sessionId) {
        this.sessionId = sessionId;
    }
}

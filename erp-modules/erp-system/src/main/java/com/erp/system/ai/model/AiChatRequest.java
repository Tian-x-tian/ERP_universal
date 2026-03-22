package com.erp.system.ai.model;

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
}

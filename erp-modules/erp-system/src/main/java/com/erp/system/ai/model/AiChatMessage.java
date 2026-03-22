package com.erp.system.ai.model;

/**
 * AI 对话消息。
 */
public class AiChatMessage {
    /**
     * 消息角色。
     */
    private String role;

    /**
     * 消息内容。
     */
    private String content;

    public AiChatMessage() {
    }

    public AiChatMessage(String role, String content) {
        this.role = role;
        this.content = content;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }
}

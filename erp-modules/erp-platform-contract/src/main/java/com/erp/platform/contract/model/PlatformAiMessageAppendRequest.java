package com.erp.platform.contract.model;

import java.io.Serializable;

/**
 * AI 会话消息追加请求。
 *
 * <p>sessionId 为空时由服务端新建会话，并以首条用户提问截断生成标题。</p>
 */
public class PlatformAiMessageAppendRequest implements Serializable {
    private static final long serialVersionUID = 1L;

    private Long sessionId;
    private String title;
    private String model;
    private String role;
    private String content;
    private String blocksJson;
    private String actionKey;
    private Integer promptTokens;
    private Integer completionTokens;

    public Long getSessionId() {
        return sessionId;
    }

    public void setSessionId(Long sessionId) {
        this.sessionId = sessionId;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getModel() {
        return model;
    }

    public void setModel(String model) {
        this.model = model;
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

    public String getBlocksJson() {
        return blocksJson;
    }

    public void setBlocksJson(String blocksJson) {
        this.blocksJson = blocksJson;
    }

    public String getActionKey() {
        return actionKey;
    }

    public void setActionKey(String actionKey) {
        this.actionKey = actionKey;
    }

    public Integer getPromptTokens() {
        return promptTokens;
    }

    public void setPromptTokens(Integer promptTokens) {
        this.promptTokens = promptTokens;
    }

    public Integer getCompletionTokens() {
        return completionTokens;
    }

    public void setCompletionTokens(Integer completionTokens) {
        this.completionTokens = completionTokens;
    }
}

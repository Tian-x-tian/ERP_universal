package com.erp.platform.contract.model;

import java.io.Serializable;
import java.util.Date;

/**
 * AI 会话消息视图。
 */
public class PlatformAiMessageView implements Serializable {
    private static final long serialVersionUID = 1L;

    private Long messageId;
    private Long sessionId;
    private String role;
    private String content;
    /** 结构化区块 JSON（指标卡/表格/图表），仅助手消息可能存在 */
    private String blocksJson;
    /** 本条消息关联的动作编码 */
    private String actionKey;
    private Integer promptTokens;
    private Integer completionTokens;
    private Date createTime;

    public Long getMessageId() {
        return messageId;
    }

    public void setMessageId(Long messageId) {
        this.messageId = messageId;
    }

    public Long getSessionId() {
        return sessionId;
    }

    public void setSessionId(Long sessionId) {
        this.sessionId = sessionId;
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

    public Date getCreateTime() {
        return createTime;
    }

    public void setCreateTime(Date createTime) {
        this.createTime = createTime;
    }
}

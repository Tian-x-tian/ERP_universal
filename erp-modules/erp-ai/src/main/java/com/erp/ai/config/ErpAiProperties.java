package com.erp.ai.config;

/**
 * ERP AI 功能配置。
 */
public class ErpAiProperties {
    /**
     * 是否启用 AI 功能。
     */
    private boolean enabled = true;

    /**
     * OpenAI 兼容服务基础地址。
     */
    private String baseUrl = "http://127.0.0.1:8317/v1";

    /**
     * Chat Completions 请求路径。
     */
    private String chatPath = "/chat/completions";

    /**
     * 默认模型编号。
     */
    private String model = "gpt-5.1";

    /**
     * 连接超时时间（毫秒）。
     */
    private long connectTimeoutMs = 5000L;

    /**
     * 读取超时时间（毫秒）。
     */
    private long readTimeoutMs = 120000L;

    /**
     * 最大保留历史轮数。
     */
    private int maxHistoryTurns = 12;

    /**
     * 注入提示词的最大待办条数。
     */
    private int maxTodoItems = 10;

    /**
     * 注入提示词的最大消息条数。
     */
    private int maxNoticeItems = 10;

    /**
     * 单次模型调用允许预留的最大输入 Token 数。
     */
    private int maxInputTokens = 32768;

    /**
     * 单次模型调用预留并传给上游的最大输出 Token 数。
     */
    private int maxOutputTokens = 4096;

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getBaseUrl() {
        return baseUrl;
    }

    public void setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    public String getChatPath() {
        return chatPath;
    }

    public void setChatPath(String chatPath) {
        this.chatPath = chatPath;
    }

    public String getModel() {
        return model;
    }

    public void setModel(String model) {
        this.model = model;
    }

    public long getConnectTimeoutMs() {
        return connectTimeoutMs;
    }

    public void setConnectTimeoutMs(long connectTimeoutMs) {
        this.connectTimeoutMs = connectTimeoutMs;
    }

    public long getReadTimeoutMs() {
        return readTimeoutMs;
    }

    public void setReadTimeoutMs(long readTimeoutMs) {
        this.readTimeoutMs = readTimeoutMs;
    }

    public int getMaxHistoryTurns() {
        return maxHistoryTurns;
    }

    public void setMaxHistoryTurns(int maxHistoryTurns) {
        this.maxHistoryTurns = maxHistoryTurns;
    }

    public int getMaxTodoItems() {
        return maxTodoItems;
    }

    public void setMaxTodoItems(int maxTodoItems) {
        this.maxTodoItems = maxTodoItems;
    }

    public int getMaxNoticeItems() {
        return maxNoticeItems;
    }

    public void setMaxNoticeItems(int maxNoticeItems) {
        this.maxNoticeItems = maxNoticeItems;
    }

    public int getMaxInputTokens() {
        return maxInputTokens;
    }

    public void setMaxInputTokens(int maxInputTokens) {
        this.maxInputTokens = maxInputTokens;
    }

    public int getMaxOutputTokens() {
        return maxOutputTokens;
    }

    public void setMaxOutputTokens(int maxOutputTokens) {
        this.maxOutputTokens = maxOutputTokens;
    }
}

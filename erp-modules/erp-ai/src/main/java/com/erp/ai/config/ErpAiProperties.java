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
     * 流式线程池核心线程数。
     *
     * <p>一次 SSE 对话会独占一个线程直到模型返回，线程数直接等于并发对话上限。</p>
     */
    private int streamCorePoolSize = 16;

    /**
     * 流式线程池最大线程数。
     */
    private int streamMaxPoolSize = 64;

    /**
     * 流式线程池队列容量。
     */
    private int streamQueueCapacity = 128;

    /**
     * SSE 连接超时时间（毫秒），0 表示永不超时。
     *
     * <p>务必保留一个有限值作为兜底：模型服务卡死时，永不超时的连接会一直占用线程。</p>
     */
    private long sseTimeoutMs = 300000L;

    /**
     * Agent 循环的最大工具调用轮次。
     */
    private int maxToolRounds = 4;

    /**
     * 单轮 Agent 循环中允许并行执行的工具数上限。
     */
    private int maxToolCallsPerRound = 3;

    /**
     * 一次对话的总耗时上限（毫秒），超过后不再发起新的模型轮次。
     */
    private long maxConversationMs = 180000L;

    /**
     * 是否启用只读工具集。
     */
    private boolean readToolsEnabled = true;

    /**
     * 只读数据集单次返回的最大行数。
     */
    private int maxDatasetRows = 20;

    /**
     * 是否启用服务端会话存档。
     */
    private boolean sessionPersistEnabled = true;

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

    public int getStreamCorePoolSize() {
        return streamCorePoolSize;
    }

    public void setStreamCorePoolSize(int streamCorePoolSize) {
        this.streamCorePoolSize = streamCorePoolSize;
    }

    public int getStreamMaxPoolSize() {
        return streamMaxPoolSize;
    }

    public void setStreamMaxPoolSize(int streamMaxPoolSize) {
        this.streamMaxPoolSize = streamMaxPoolSize;
    }

    public int getStreamQueueCapacity() {
        return streamQueueCapacity;
    }

    public void setStreamQueueCapacity(int streamQueueCapacity) {
        this.streamQueueCapacity = streamQueueCapacity;
    }

    public long getSseTimeoutMs() {
        return sseTimeoutMs;
    }

    public void setSseTimeoutMs(long sseTimeoutMs) {
        this.sseTimeoutMs = sseTimeoutMs;
    }

    public int getMaxToolRounds() {
        return maxToolRounds;
    }

    public void setMaxToolRounds(int maxToolRounds) {
        this.maxToolRounds = maxToolRounds;
    }

    public int getMaxToolCallsPerRound() {
        return maxToolCallsPerRound;
    }

    public void setMaxToolCallsPerRound(int maxToolCallsPerRound) {
        this.maxToolCallsPerRound = maxToolCallsPerRound;
    }

    public long getMaxConversationMs() {
        return maxConversationMs;
    }

    public void setMaxConversationMs(long maxConversationMs) {
        this.maxConversationMs = maxConversationMs;
    }

    public boolean isReadToolsEnabled() {
        return readToolsEnabled;
    }

    public void setReadToolsEnabled(boolean readToolsEnabled) {
        this.readToolsEnabled = readToolsEnabled;
    }

    public int getMaxDatasetRows() {
        return maxDatasetRows;
    }

    public void setMaxDatasetRows(int maxDatasetRows) {
        this.maxDatasetRows = maxDatasetRows;
    }

    public boolean isSessionPersistEnabled() {
        return sessionPersistEnabled;
    }

    public void setSessionPersistEnabled(boolean sessionPersistEnabled) {
        this.sessionPersistEnabled = sessionPersistEnabled;
    }
}

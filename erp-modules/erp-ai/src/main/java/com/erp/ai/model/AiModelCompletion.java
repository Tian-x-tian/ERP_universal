package com.erp.ai.model;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * AI 模型补全结果对象。
 */
public class AiModelCompletion implements Serializable {
    private static final long serialVersionUID = 1L;

    /**
     * 回复文本。
     */
    private String content;

    /**
     * 工具调用列表。
     */
    private List<AiToolCall> toolCalls = new ArrayList<>();

    /**
     * 本次调用的 token 用量。
     */
    private AiTokenUsage usage = new AiTokenUsage();

    /**
     * 模型返回的结束原因（stop/tool_calls/length…）。
     */
    private String finishReason;

    /**
     * 上游响应是否包含可信的 usage 数据。
     */
    private boolean usageReported;

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public List<AiToolCall> getToolCalls() {
        return toolCalls;
    }

    public void setToolCalls(List<AiToolCall> toolCalls) {
        this.toolCalls = toolCalls == null ? new ArrayList<>() : toolCalls;
    }

    public AiTokenUsage getUsage() {
        return usage;
    }

    public void setUsage(AiTokenUsage usage) {
        this.usage = usage == null ? new AiTokenUsage() : usage;
        this.usageReported = usage != null && usage.hasValue();
    }

    public String getFinishReason() {
        return finishReason;
    }

    public void setFinishReason(String finishReason) {
        this.finishReason = finishReason;
    }

    public Long getInputTokens() {
        return usageReported ? (long) usage.getPromptTokens() : null;
    }

    public void setInputTokens(Long inputTokens) {
        usage.setPromptTokens(tokenValue(inputTokens));
        usage.setTotalTokens(usage.getPromptTokens() + usage.getCompletionTokens());
    }

    public Long getOutputTokens() {
        return usageReported ? (long) usage.getCompletionTokens() : null;
    }

    public void setOutputTokens(Long outputTokens) {
        usage.setCompletionTokens(tokenValue(outputTokens));
        usage.setTotalTokens(usage.getPromptTokens() + usage.getCompletionTokens());
    }

    public boolean isUsageReported() {
        return usageReported;
    }

    public void setUsageReported(boolean usageReported) {
        this.usageReported = usageReported;
    }

    private int tokenValue(Long value) {
        if (value == null) {
            return 0;
        }
        if (value < 0 || value > Integer.MAX_VALUE) {
            throw new IllegalArgumentException("Token usage is outside the supported range");
        }
        return value.intValue();
    }
}

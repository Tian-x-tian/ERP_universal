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
     * 上游返回的输入 Token 用量。
     */
    private Long inputTokens;

    /**
     * 上游返回的输出 Token 用量。
     */
    private Long outputTokens;

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
        this.toolCalls = toolCalls;
    }

    public Long getInputTokens() {
        return inputTokens;
    }

    public void setInputTokens(Long inputTokens) {
        this.inputTokens = inputTokens;
    }

    public Long getOutputTokens() {
        return outputTokens;
    }

    public void setOutputTokens(Long outputTokens) {
        this.outputTokens = outputTokens;
    }

    public boolean isUsageReported() {
        return usageReported;
    }

    public void setUsageReported(boolean usageReported) {
        this.usageReported = usageReported;
    }
}

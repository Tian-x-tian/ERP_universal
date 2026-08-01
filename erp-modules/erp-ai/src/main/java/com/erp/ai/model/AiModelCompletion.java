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
    }

    public String getFinishReason() {
        return finishReason;
    }

    public void setFinishReason(String finishReason) {
        this.finishReason = finishReason;
    }
}

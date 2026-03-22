package com.erp.system.ai.model;

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
}

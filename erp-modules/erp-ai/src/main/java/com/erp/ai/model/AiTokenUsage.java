package com.erp.ai.model;

import java.io.Serializable;

/**
 * 模型 token 用量。
 *
 * <p>对应 OpenAI 兼容协议响应体中的 {@code usage} 字段；流式响应需要在请求里带上
 * {@code stream_options.include_usage=true} 才会在最后一个事件块中返回。</p>
 */
public class AiTokenUsage implements Serializable {
    private static final long serialVersionUID = 1L;

    private int promptTokens;
    private int completionTokens;
    private int totalTokens;

    /**
     * 累加另一次调用的用量，用于统计 Agent 多轮循环的总消耗。
     *
     * @param other 另一次调用的用量
     */
    public void add(AiTokenUsage other) {
        if (other == null) {
            return;
        }
        this.promptTokens += other.promptTokens;
        this.completionTokens += other.completionTokens;
        this.totalTokens += other.totalTokens > 0
                ? other.totalTokens
                : other.promptTokens + other.completionTokens;
    }

    /**
     * 判断是否已经采集到有效用量。
     *
     * @return true 表示有用量数据
     */
    public boolean hasValue() {
        return promptTokens > 0 || completionTokens > 0 || totalTokens > 0;
    }

    public int getPromptTokens() {
        return promptTokens;
    }

    public void setPromptTokens(int promptTokens) {
        this.promptTokens = promptTokens;
    }

    public int getCompletionTokens() {
        return completionTokens;
    }

    public void setCompletionTokens(int completionTokens) {
        this.completionTokens = completionTokens;
    }

    public int getTotalTokens() {
        return totalTokens;
    }

    public void setTotalTokens(int totalTokens) {
        this.totalTokens = totalTokens;
    }
}

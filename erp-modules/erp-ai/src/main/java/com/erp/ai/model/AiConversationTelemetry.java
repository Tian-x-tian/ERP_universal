package com.erp.ai.model;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * 一次对话的遥测数据。
 *
 * <p>Agent 循环可能跨多轮模型调用与多次工具执行，token 消耗、工具使用情况和产出的结构化区块
 * 需要跨轮累计，最终一次性写入审计与会话存档。</p>
 */
public class AiConversationTelemetry {
    private final String model;
    private final AiTokenUsage usage = new AiTokenUsage();
    private final Set<String> toolKeys = new LinkedHashSet<>();
    private final List<AiStructuredBlock> blocks = new ArrayList<>();
    private int toolRounds;
    private Long sessionId;

    public AiConversationTelemetry(String model) {
        this.model = model;
    }

    /**
     * 累加一次模型调用的 token 用量。
     *
     * @param roundUsage 本轮用量
     */
    public void addUsage(AiTokenUsage roundUsage) {
        usage.add(roundUsage);
    }

    /**
     * 记录进入了一轮工具调用。
     */
    public void markToolRound() {
        toolRounds++;
    }

    /**
     * 记录使用过的工具。
     *
     * @param toolName 工具名称
     */
    public void markToolUsed(String toolName) {
        if (toolName != null && !toolName.trim().isEmpty()) {
            toolKeys.add(toolName.trim());
        }
    }

    /**
     * 记录一个结构化区块。
     *
     * @param block 结构化区块
     */
    public void addBlock(AiStructuredBlock block) {
        if (block != null) {
            blocks.add(block);
        }
    }

    /**
     * 拼接使用过的工具名，用于审计留痕。
     *
     * @return 逗号分隔的工具名；未使用工具时返回 null
     */
    public String joinToolKeys() {
        return toolKeys.isEmpty() ? null : String.join(",", toolKeys);
    }

    public String getModel() {
        return model;
    }

    public AiTokenUsage getUsage() {
        return usage;
    }

    public List<AiStructuredBlock> getBlocks() {
        return blocks;
    }

    public int getToolRounds() {
        return toolRounds;
    }

    public Long getSessionId() {
        return sessionId;
    }

    public void setSessionId(Long sessionId) {
        this.sessionId = sessionId;
    }
}

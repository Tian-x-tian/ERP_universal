package com.erp.ai.model;

import java.util.ArrayList;
import java.util.List;

/**
 * AI 对话消息。
 *
 * <p>除普通的 user/assistant 文本消息外，还承载 OpenAI 兼容协议中 Agent 循环所需的两类消息：
 * 带 {@code tool_calls} 的 assistant 消息，以及带 {@code tool_call_id} 的 tool 结果消息。</p>
 */
public class AiChatMessage {
    /**
     * 消息角色（system/user/assistant/tool）。
     */
    private String role;

    /**
     * 消息内容。
     */
    private String content;

    /**
     * assistant 消息发起的工具调用列表。
     */
    private List<AiToolCall> toolCalls = new ArrayList<>();

    /**
     * tool 消息对应的工具调用标识。
     */
    private String toolCallId;

    /**
     * tool 消息对应的工具名称。
     */
    private String name;

    public AiChatMessage() {
    }

    public AiChatMessage(String role, String content) {
        this.role = role;
        this.content = content;
    }

    /**
     * 构造一条携带工具调用的 assistant 消息。
     *
     * @param content   文本内容，可为空
     * @param toolCalls 工具调用列表
     * @return assistant 消息
     */
    public static AiChatMessage assistantToolCalls(String content, List<AiToolCall> toolCalls) {
        AiChatMessage message = new AiChatMessage("assistant", content);
        message.setToolCalls(toolCalls);
        return message;
    }

    /**
     * 构造一条工具执行结果消息。
     *
     * @param toolCallId 工具调用标识
     * @param name       工具名称
     * @param content    工具结果文本
     * @return tool 消息
     */
    public static AiChatMessage toolResult(String toolCallId, String name, String content) {
        AiChatMessage message = new AiChatMessage("tool", content);
        message.setToolCallId(toolCallId);
        message.setName(name);
        return message;
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

    public List<AiToolCall> getToolCalls() {
        return toolCalls;
    }

    public void setToolCalls(List<AiToolCall> toolCalls) {
        this.toolCalls = toolCalls == null ? new ArrayList<>() : toolCalls;
    }

    public String getToolCallId() {
        return toolCallId;
    }

    public void setToolCallId(String toolCallId) {
        this.toolCallId = toolCallId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}

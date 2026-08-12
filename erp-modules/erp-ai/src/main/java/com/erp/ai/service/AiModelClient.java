package com.erp.ai.service;

import com.erp.ai.model.AiChatMessage;
import com.erp.ai.model.AiModelCompletion;
import com.erp.ai.model.AiToolDefinition;

import java.io.IOException;
import java.util.List;
import java.util.function.Consumer;

/**
 * AI 模型客户端。
 */
public interface AiModelClient {

    /**
     * 探测当前模型服务是否可用。
     *
     * @return true 表示可用
     */
    boolean isAvailable();

    /**
     * 以非流式方式请求模型补全，并支持工具调用返回。
     *
     * @param messages 对话消息列表
     * @param tools    可用工具列表
     * @return 模型补全结果
     * @throws IOException          IO 异常
     * @throws InterruptedException 中断异常
     */
    AiModelCompletion completeChat(List<AiChatMessage> messages, List<AiToolDefinition> tools)
            throws IOException, InterruptedException;

    /**
     * 以非流式方式请求指定模型补全，并支持工具调用返回。
     *
     * @param model    模型编号，为空时使用默认模型
     * @param messages 对话消息列表
     * @param tools    可用工具列表
     * @return 模型补全结果
     * @throws IOException          IO 异常
     * @throws InterruptedException 中断异常
     */
    AiModelCompletion completeChat(String model, List<AiChatMessage> messages, List<AiToolDefinition> tools)
            throws IOException, InterruptedException;

    /**
     * 以流式方式请求模型回复。
     *
     * @param messages      对话消息列表
     * @param deltaConsumer 流式增量回调
     * @return 最终完整回复
     * @throws IOException          IO 异常
     * @throws InterruptedException 中断异常
     */
    String streamChat(List<AiChatMessage> messages, Consumer<String> deltaConsumer) throws IOException, InterruptedException;

    /**
     * 以流式方式请求指定模型回复。
     *
     * @param model         模型编号，为空时使用默认模型
     * @param messages      对话消息列表
     * @param deltaConsumer 流式增量回调
     * @return 最终完整回复
     * @throws IOException          IO 异常
     * @throws InterruptedException 中断异常
     */
    String streamChat(String model, List<AiChatMessage> messages, Consumer<String> deltaConsumer)
            throws IOException, InterruptedException;

    /**
     * 以流式方式请求补全，同时支持文本增量与工具调用。
     *
     * <p>与 {@link #streamChat} 的区别在于它保留完整的补全语义：文本按 token 边推边回调，
     * 工具调用则按 OpenAI 协议的分片规则（同一 index 的 arguments 需要逐段拼接）还原后一次性返回。
     * 这样 Agent 循环既能拿到真流式体验，又不丢工具调用能力。</p>
     *
     * @param model         模型编号，为空时使用默认模型
     * @param messages      对话消息列表
     * @param tools         可用工具列表
     * @param deltaConsumer 文本增量回调
     * @return 模型补全结果
     * @throws IOException          IO 异常
     * @throws InterruptedException 中断异常
     */
    AiModelCompletion streamCompletion(String model,
            List<AiChatMessage> messages,
            List<AiToolDefinition> tools,
            Consumer<String> deltaConsumer) throws IOException, InterruptedException;
}

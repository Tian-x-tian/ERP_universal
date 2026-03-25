package com.erp.ai.service;

import com.erp.ai.model.AiActionDescriptor;
import com.erp.ai.model.AiActionHandleResult;
import com.erp.ai.model.AiActionResultVO;
import com.erp.ai.model.AiPromptContext;
import com.erp.ai.model.AiToolCall;
import com.erp.ai.model.AiToolDefinition;

import java.util.List;

/**
 * AI 动作编排服务。
 */
public interface AiActionService {

    /**
     * 查询当前用户可执行的动作列表。
     *
     * @return 动作列表
     */
    List<AiActionDescriptor> listAvailableActions();

    /**
     * 构造当前用户可用的模型工具列表。
     *
     * @return 工具定义列表
     */
    List<AiToolDefinition> buildAvailableTools();

    /**
     * 处理模型返回的工具调用。
     *
     * @param toolCall      工具调用
     * @param promptContext 提示词上下文
     * @return 动作处理结果
     */
    AiActionHandleResult handleToolCall(AiToolCall toolCall, AiPromptContext promptContext);

    /**
     * 确认执行高风险动作。
     *
     * @param confirmationToken 确认票据
     * @return 动作执行结果
     */
    AiActionResultVO confirm(String confirmationToken);
}

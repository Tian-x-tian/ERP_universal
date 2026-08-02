package com.erp.ai.service;

import com.erp.ai.model.AiPanelCardVO;
import com.erp.ai.model.AiReadToolResult;
import com.erp.ai.model.AiToolDefinition;

import java.util.List;
import java.util.Map;

/**
 * AI 只读工具服务。
 *
 * <p>只读工具让模型可以按需向各业务服务取数，而不是依赖一次性预取的固定上下文。
 * 每个工具都受 RBAC 权限约束，且只允许聚合统计，不暴露明细主键。</p>
 */
public interface AiReadToolService {

    /**
     * 构造当前用户可调用的只读工具定义。
     *
     * @return 工具定义列表
     */
    List<AiToolDefinition> buildAvailableTools();

    /**
     * 判断给定名称是否为只读工具。
     *
     * @param toolName 工具名称
     * @return true 表示是只读工具
     */
    boolean isReadTool(String toolName);

    /**
     * 执行只读工具。
     *
     * @param toolName  工具名称
     * @param arguments 工具参数
     * @return 执行结果
     */
    AiReadToolResult execute(String toolName, Map<String, Object> arguments);

    /**
     * 列出当前用户可用的面板卡片。
     *
     * @return 卡片列表
     */
    List<AiPanelCardVO> listAvailableCards();

    /**
     * 解析只读工具的展示名。
     *
     * @param toolName 工具名称
     * @return 展示名
     */
    String resolveToolLabel(String toolName);
}

package com.erp.ai.service;

import com.erp.ai.model.AiPendingAction;
import com.erp.ai.model.AiStructuredBlock;

/**
 * AI 流式输出监听器。
 */
public interface AiStreamListener {

    /**
     * 发送流式准备完成事件。
     *
     * @param model 当前模型编号
     */
    void onReady(String model);

    /**
     * 发送会话绑定事件，告知前端本轮消息落在哪个服务端会话上。
     *
     * @param sessionId 会话ID
     */
    default void onSession(Long sessionId) {
        // 默认不处理，兼容不关心会话的监听器实现。
    }

    /**
     * 发送工具开始执行事件，用于前端展示“正在查询…”的过程反馈。
     *
     * @param toolName  工具名称
     * @param toolLabel 工具展示名
     */
    default void onToolStart(String toolName, String toolLabel) {
        // 默认不处理。
    }

    /**
     * 发送结构化区块事件（指标卡 / 表格 / 图表）。
     *
     * @param block 结构化区块
     */
    default void onBlock(AiStructuredBlock block) {
        // 默认不处理。
    }

    /**
     * 发送流式增量事件。
     *
     * @param delta 增量文本
     */
    void onDelta(String delta);

    /**
     * 发送流式结束事件。
     *
     * @param content 最终完整文本
     */
    void onDone(String content);

    /**
     * 发送待确认动作事件。
     *
     * @param pendingAction 待确认动作对象
     */
    void onActionRequired(AiPendingAction pendingAction);

    /**
     * 发送流式错误事件。
     *
     * @param message 错误提示
     */
    void onError(String message);
}

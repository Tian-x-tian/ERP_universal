package com.erp.system.ai.service;

import com.erp.system.ai.model.AiPendingAction;

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

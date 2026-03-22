package com.erp.system.ai.service;

import com.erp.system.ai.model.AiChatRequest;
import com.erp.system.ai.model.AiActionResultVO;
import com.erp.system.ai.model.AiActionConfirmRequest;
import com.erp.system.ai.model.AiMetaVO;

/**
 * AI 对话服务。
 */
public interface AiChatService {

    /**
     * 查询 AI 元信息。
     *
     * @return AI 元信息
     */
    AiMetaVO getMeta();

    /**
     * 执行 AI 流式对话。
     *
     * @param request  对话请求
     * @param listener 流式监听器
     */
    void streamChat(AiChatRequest request, AiStreamListener listener);

    /**
     * 确认执行高风险动作。
     *
     * @param request 确认请求
     * @return 执行结果
     */
    AiActionResultVO confirmAction(AiActionConfirmRequest request);
}

package com.erp.ai.service;

import com.erp.ai.model.AiChatRequest;
import com.erp.ai.model.AiPromptContext;

/**
 * AI 上下文聚合服务。
 */
public interface AiContextService {

    /**
     * 聚合当前登录用户的 AI 提示词上下文。
     *
     * @param request 前端对话请求
     * @return 聚合后的提示词上下文
     */
    AiPromptContext buildPromptContext(AiChatRequest request);
}

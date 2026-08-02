package com.erp.ai.service;

import com.erp.ai.model.AiQuotaDecision;

/**
 * AI 配额服务。
 *
 * <p>面板会显著放大调用量：一屏卡片、每日简报、多轮 Agent 循环都会打模型。
 * 这里在对话入口做一次租户/用户级的日配额判定，避免单租户把模型服务打满或把成本跑飞。</p>
 */
public interface AiQuotaService {

    /**
     * 判定当前调用是否在配额内。
     *
     * @return 判定结果
     */
    AiQuotaDecision check();
}

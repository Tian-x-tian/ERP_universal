package com.erp.ai.service;

import com.erp.ai.model.AiActionDescriptor;
import com.erp.ai.model.AiPolicySummaryVO;
import com.erp.ai.model.AiRoleProfileVO;

import java.util.List;
import java.util.Map;

/**
 * AI 角色引导服务。
 */
public interface AiRoleGuideService {
    /**
     * 构造当前用户角色画像。
     *
     * @return 角色画像
     */
    AiRoleProfileVO buildCurrentRoleProfile();

    /**
     * 构造页面快捷提问模板。
     *
     * @param roleProfile 当前角色画像
     * @return 页面快捷提问模板
     */
    Map<String, List<String>> buildPageQuestionTemplates(AiRoleProfileVO roleProfile);

    /**
     * 构造策略摘要。
     *
     * @param actions 当前可执行动作
     * @return 策略摘要
     */
    AiPolicySummaryVO buildPolicySummary(List<AiActionDescriptor> actions);
}

package com.erp.ai.service;

import com.erp.ai.model.AiRuntimeConfig;
import com.erp.platform.contract.model.PlatformAiActionPolicyItem;
import com.erp.platform.contract.model.PlatformAiAuditCreateRequest;
import com.erp.platform.contract.model.PlatformAiAuditView;
import com.erp.platform.contract.model.PlatformAiConfigUpdateRequest;
import com.erp.platform.contract.model.PlatformAiConfigView;

import java.util.List;

/**
 * AI 租户配置服务。
 */
public interface AiTenantConfigService {
    /**
     * 查询当前租户 AI 配置。
     *
     * @return AI 配置
     */
    PlatformAiConfigView getConfig();

    /**
     * 更新当前租户 AI 配置。
     *
     * @param request 更新请求
     * @return 更新后的配置
     */
    PlatformAiConfigView updateConfig(PlatformAiConfigUpdateRequest request);

    /**
     * 查询当前租户动作策略。
     *
     * @return 动作策略列表
     */
    List<PlatformAiActionPolicyItem> listActionPolicies();

    /**
     * 更新当前租户动作策略。
     *
     * @param policyItems 动作策略列表
     * @return 更新后的动作策略列表
     */
    List<PlatformAiActionPolicyItem> updateActionPolicies(List<PlatformAiActionPolicyItem> policyItems);

    /**
     * 查询当前租户审计记录。
     *
     * @param limit 限制条数
     * @return 审计记录列表
     */
    List<PlatformAiAuditView> listAuditRecords(int limit);

    /**
     * 记录 AI 审计事件。
     *
     * @param request 审计请求
     */
    void recordAudit(PlatformAiAuditCreateRequest request);

    /**
     * 解析当前租户运行时配置。
     *
     * @return 运行时配置
     */
    AiRuntimeConfig resolveRuntimeConfig();

    /**
     * 判断当前账号是否拥有指定权限。
     *
     * @param permission 权限编码
     * @return true 表示有权限
     */
    boolean hasPermission(String permission);
}

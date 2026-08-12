package com.erp.system.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.erp.platform.contract.model.PlatformAiActionPolicyItem;
import com.erp.platform.contract.model.PlatformAiConfigUpdateRequest;
import com.erp.platform.contract.model.PlatformAiConfigView;
import com.erp.system.domain.SysAiConfig;

import java.util.List;

/**
 * AI 租户配置服务。
 */
public interface ISysAiConfigService extends IService<SysAiConfig> {
    /**
     * 查询租户 AI 配置。
     *
     * @param tenantId 租户编号
     * @return AI 配置
     */
    PlatformAiConfigView getTenantConfig(String tenantId);

    /**
     * 更新租户 AI 配置。
     *
     * @param tenantId 租户编号
     * @param request  更新请求
     * @return 更新后的配置
     */
    PlatformAiConfigView updateTenantConfig(String tenantId, PlatformAiConfigUpdateRequest request);

    /**
     * 查询租户动作策略。
     *
     * @param tenantId 租户编号
     * @return 动作策略列表
     */
    List<PlatformAiActionPolicyItem> listActionPolicies(String tenantId);

    /**
     * 更新租户动作策略。
     *
     * @param tenantId 租户编号
     * @param policyItems 动作策略列表
     * @return 更新后的动作策略列表
     */
    List<PlatformAiActionPolicyItem> updateActionPolicies(String tenantId, List<PlatformAiActionPolicyItem> policyItems);
}

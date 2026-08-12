package com.erp.ai.service.impl;

import com.erp.ai.config.ErpAiProperties;
import com.erp.ai.context.AiInvocationScope;
import com.erp.ai.model.AiRuntimeConfig;
import com.erp.ai.service.AiTenantConfigService;
import com.erp.common.client.internal.InternalSystemClient;
import com.erp.platform.contract.model.PlatformAiActionPolicyItem;
import com.erp.platform.contract.model.PlatformAiAuditCreateRequest;
import com.erp.platform.contract.model.PlatformAiAuditView;
import com.erp.platform.contract.model.PlatformAiConfigUpdateRequest;
import com.erp.platform.contract.model.PlatformAiConfigView;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * AI 租户配置服务实现。
 */
@Service
public class AiTenantConfigServiceImpl implements AiTenantConfigService {
    private static final int DEFAULT_AUDIT_LIMIT = 50;
    private static final String CACHE_KEY_CONFIG = "ai:config";
    private static final String CACHE_KEY_POLICIES = "ai:policies";
    private static final String CACHE_KEY_RUNTIME = "ai:runtime-config";
    private static final String CACHE_KEY_PERMISSION_PREFIX = "ai:perm:";

    private final InternalSystemClient internalSystemClient;
    private final ErpAiProperties erpAiProperties;

    public AiTenantConfigServiceImpl(InternalSystemClient internalSystemClient, ErpAiProperties erpAiProperties) {
        this.internalSystemClient = internalSystemClient;
        this.erpAiProperties = erpAiProperties;
    }

    /**
     * 查询当前租户 AI 配置。
     *
     * @return AI 配置
     */
    @Override
    public PlatformAiConfigView getConfig() {
        return AiInvocationScope.compute(CACHE_KEY_CONFIG, () -> {
            try {
                PlatformAiConfigView config = internalSystemClient.getAiConfig();
                return config == null ? buildDefaultConfigView() : config;
            } catch (Exception ignored) {
                return buildDefaultConfigView();
            }
        });
    }

    /**
     * 更新当前租户 AI 配置。
     *
     * @param request 更新请求
     * @return 更新后的配置
     */
    @Override
    public PlatformAiConfigView updateConfig(PlatformAiConfigUpdateRequest request) {
        try {
            PlatformAiConfigView config = internalSystemClient.updateAiConfig(request);
            return config == null ? buildDefaultConfigView() : config;
        } catch (Exception ignored) {
            return buildDefaultConfigView();
        } finally {
            evictConfigCache();
        }
    }

    /**
     * 查询当前租户动作策略。
     *
     * @return 动作策略列表
     */
    @Override
    public List<PlatformAiActionPolicyItem> listActionPolicies() {
        return AiInvocationScope.compute(CACHE_KEY_POLICIES, () -> {
            try {
                List<PlatformAiActionPolicyItem> policyList = internalSystemClient.listAiActionPolicies();
                return policyList == null ? new ArrayList<>() : policyList;
            } catch (Exception ignored) {
                return new ArrayList<>();
            }
        });
    }

    /**
     * 更新当前租户动作策略。
     *
     * @param policyItems 动作策略列表
     * @return 更新后的动作策略列表
     */
    @Override
    public List<PlatformAiActionPolicyItem> updateActionPolicies(List<PlatformAiActionPolicyItem> policyItems) {
        try {
            List<PlatformAiActionPolicyItem> updatedPolicyList = internalSystemClient.updateAiActionPolicies(policyItems);
            return updatedPolicyList == null ? new ArrayList<>() : updatedPolicyList;
        } catch (Exception ignored) {
            return new ArrayList<>();
        } finally {
            evictConfigCache();
        }
    }

    /**
     * 写操作后失效配置类缓存，保证同一请求内后续读取拿到新值。
     */
    private void evictConfigCache() {
        AiInvocationScope.evict(CACHE_KEY_CONFIG);
        AiInvocationScope.evict(CACHE_KEY_POLICIES);
        AiInvocationScope.evict(CACHE_KEY_RUNTIME);
    }

    /**
     * 查询当前租户审计记录。
     *
     * @param limit 限制条数
     * @return 审计记录列表
     */
    @Override
    public List<PlatformAiAuditView> listAuditRecords(int limit) {
        int safeLimit = limit <= 0 ? DEFAULT_AUDIT_LIMIT : Math.min(limit, 200);
        try {
            List<PlatformAiAuditView> auditViews = internalSystemClient.listAiAuditRecords(safeLimit);
            return auditViews == null ? new ArrayList<>() : auditViews;
        } catch (Exception ignored) {
            return new ArrayList<>();
        }
    }

    /**
     * 记录 AI 审计事件。
     *
     * @param request 审计请求
     */
    @Override
    public void recordAudit(PlatformAiAuditCreateRequest request) {
        try {
            internalSystemClient.recordAiAudit(request);
        } catch (Exception ignored) {
            // 审计失败不阻断主流程。
        }
    }

    /**
     * 解析当前租户运行时配置。
     *
     * @return 运行时配置
     */
    @Override
    public AiRuntimeConfig resolveRuntimeConfig() {
        return AiInvocationScope.compute(CACHE_KEY_RUNTIME, this::doResolveRuntimeConfig);
    }

    /**
     * 实际组装运行时配置。
     *
     * @return 运行时配置
     */
    private AiRuntimeConfig doResolveRuntimeConfig() {
        PlatformAiConfigView config = getConfig();
        List<PlatformAiActionPolicyItem> policyList = listActionPolicies();
        AiRuntimeConfig runtimeConfig = new AiRuntimeConfig();
        runtimeConfig.setEnabled(config.isEnabled());
        runtimeConfig.setModel(StringUtils.hasText(config.getModel()) ? config.getModel().trim() : erpAiProperties.getModel());
        runtimeConfig.setMaxHistoryTurns(normalizeCount(config.getMaxHistoryTurns(), erpAiProperties.getMaxHistoryTurns()));
        runtimeConfig.setMaxTodoItems(normalizeCount(config.getMaxTodoItems(), erpAiProperties.getMaxTodoItems()));
        runtimeConfig.setMaxNoticeItems(normalizeCount(config.getMaxNoticeItems(), erpAiProperties.getMaxNoticeItems()));
        runtimeConfig.setPromptTemplate(trimToNull(config.getPromptTemplate()));
        runtimeConfig.setTenantConfigVersion(StringUtils.hasText(config.getConfigVersion()) ? config.getConfigVersion() : "default");
        runtimeConfig.setActionPolicyList(policyList);
        return runtimeConfig;
    }

    /**
     * 判断当前账号是否拥有指定权限。
     *
     * @param permission 权限编码
     * @return true 表示有权限
     */
    @Override
    public boolean hasPermission(String permission) {
        if (!StringUtils.hasText(permission)) {
            return false;
        }
        String normalizedPermission = permission.trim();
        // 一次对话里同一权限会被反复问到，缓存到调用作用域，避免放大成几十次跨服务调用。
        return Boolean.TRUE.equals(AiInvocationScope.compute(CACHE_KEY_PERMISSION_PREFIX + normalizedPermission, () -> {
            try {
                return Boolean.TRUE.equals(internalSystemClient.hasPermission(normalizedPermission));
            } catch (Exception ignored) {
                return Boolean.FALSE;
            }
        }));
    }

    /**
     * 构造默认配置。
     *
     * @return 默认配置
     */
    private PlatformAiConfigView buildDefaultConfigView() {
        PlatformAiConfigView view = new PlatformAiConfigView();
        view.setEnabled(erpAiProperties.isEnabled());
        view.setModel(erpAiProperties.getModel());
        view.setMaxHistoryTurns(erpAiProperties.getMaxHistoryTurns());
        view.setMaxTodoItems(erpAiProperties.getMaxTodoItems());
        view.setMaxNoticeItems(erpAiProperties.getMaxNoticeItems());
        view.setConfigVersion("default");
        return view;
    }

    /**
     * 规范化计数配置。
     *
     * @param value 原始配置
     * @param defaultValue 默认值
     * @return 规范化后值
     */
    private int normalizeCount(Integer value, int defaultValue) {
        int target = value == null ? defaultValue : value;
        if (target < 1) {
            return 1;
        }
        return Math.min(target, 100);
    }

    /**
     * 去除空白并将空字符串转为 null。
     *
     * @param value 原始文本
     * @return 规范化文本
     */
    private String trimToNull(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }
}

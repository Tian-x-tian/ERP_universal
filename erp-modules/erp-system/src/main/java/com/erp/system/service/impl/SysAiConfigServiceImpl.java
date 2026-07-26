package com.erp.system.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.erp.platform.contract.model.PlatformAiActionPolicyItem;
import com.erp.platform.contract.model.PlatformAiConfigUpdateRequest;
import com.erp.platform.contract.model.PlatformAiConfigView;
import com.erp.system.domain.SysAiConfig;
import com.erp.system.mapper.SysAiConfigMapper;
import com.erp.system.service.ISysAiConfigService;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * AI 租户配置服务实现。
 */
@Service
public class SysAiConfigServiceImpl extends ServiceImpl<SysAiConfigMapper, SysAiConfig> implements ISysAiConfigService {
    private static final String FLAG_YES = "1";
    private static final String FLAG_NO = "0";

    private static final String DEFAULT_MODEL = "gpt-5.1";
    private static final int DEFAULT_MAX_HISTORY_TURNS = 12;
    private static final int DEFAULT_MAX_TODO_ITEMS = 10;
    private static final int DEFAULT_MAX_NOTICE_ITEMS = 10;

    private static final String ACTION_TODO_CLAIM = "todo_claim";
    private static final String ACTION_TODO_FINISH = "todo_finish";
    private static final String ACTION_NOTICE_READ = "notice_read";
    private static final String ACTION_NOTICE_READ_ALL = "notice_read_all";
    private static final String ACTION_WORKFLOW_APPROVE = "workflow_approve";
    private static final String ACTION_WORKFLOW_REJECT = "workflow_reject";
    private static final String ACTION_WORKFLOW_DEFINITION_PUBLISH = "workflow_definition_publish";

    private final ObjectMapper objectMapper;

    public SysAiConfigServiceImpl(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    /**
     * 查询租户 AI 配置。
     *
     * @param tenantId 租户编号
     * @return AI 配置
     */
    @Override
    public PlatformAiConfigView getTenantConfig(String tenantId) {
        String normalizedTenantId = normalizeTenantId(tenantId);
        if (!StringUtils.hasText(normalizedTenantId)) {
            return buildDefaultConfigView(null);
        }
        SysAiConfig entity = getOne(new LambdaQueryWrapper<SysAiConfig>()
                .eq(SysAiConfig::getTenantId, normalizedTenantId)
                .last("LIMIT 1"));
        if (entity == null) {
            return buildDefaultConfigView(normalizedTenantId);
        }
        return toView(entity);
    }

    /**
     * 更新租户 AI 配置。
     *
     * @param tenantId 租户编号
     * @param request 更新请求
     * @return 更新后的配置
     */
    @Override
    public PlatformAiConfigView updateTenantConfig(String tenantId, PlatformAiConfigUpdateRequest request) {
        String normalizedTenantId = normalizeTenantId(tenantId);
        if (!StringUtils.hasText(normalizedTenantId)) {
            return buildDefaultConfigView(null);
        }

        SysAiConfig entity = getOne(new LambdaQueryWrapper<SysAiConfig>()
                .eq(SysAiConfig::getTenantId, normalizedTenantId)
                .last("LIMIT 1"));
        boolean isNew = entity == null;
        if (isNew) {
            entity = new SysAiConfig();
            entity.setTenantId(normalizedTenantId);
            entity.setEnabled(FLAG_YES);
            entity.setModel(DEFAULT_MODEL);
            entity.setMaxHistoryTurns(DEFAULT_MAX_HISTORY_TURNS);
            entity.setMaxTodoItems(DEFAULT_MAX_TODO_ITEMS);
            entity.setMaxNoticeItems(DEFAULT_MAX_NOTICE_ITEMS);
        }

        applyConfigUpdate(entity, request);

        if (isNew) {
            save(entity);
        } else {
            updateById(entity);
        }
        return toView(entity);
    }

    /**
     * 查询租户动作策略。
     *
     * @param tenantId 租户编号
     * @return 动作策略列表
     */
    @Override
    public List<PlatformAiActionPolicyItem> listActionPolicies(String tenantId) {
        String normalizedTenantId = normalizeTenantId(tenantId);
        if (!StringUtils.hasText(normalizedTenantId)) {
            return defaultActionPolicyList();
        }
        SysAiConfig entity = getOne(new LambdaQueryWrapper<SysAiConfig>()
                .eq(SysAiConfig::getTenantId, normalizedTenantId)
                .last("LIMIT 1"));
        return mergeActionPolicies(entity == null ? null : entity.getActionPolicyJson());
    }

    /**
     * 更新租户动作策略。
     *
     * @param tenantId 租户编号
     * @param policyItems 动作策略列表
     * @return 更新后的动作策略列表
     */
    @Override
    public List<PlatformAiActionPolicyItem> updateActionPolicies(String tenantId, List<PlatformAiActionPolicyItem> policyItems) {
        String normalizedTenantId = normalizeTenantId(tenantId);
        if (!StringUtils.hasText(normalizedTenantId)) {
            return defaultActionPolicyList();
        }

        List<PlatformAiActionPolicyItem> mergedPolicyList = mergeActionPolicies(serializePolicyItems(policyItems));
        SysAiConfig entity = getOne(new LambdaQueryWrapper<SysAiConfig>()
                .eq(SysAiConfig::getTenantId, normalizedTenantId)
                .last("LIMIT 1"));
        boolean isNew = entity == null;
        if (isNew) {
            entity = new SysAiConfig();
            entity.setTenantId(normalizedTenantId);
            entity.setEnabled(FLAG_YES);
            entity.setModel(DEFAULT_MODEL);
            entity.setMaxHistoryTurns(DEFAULT_MAX_HISTORY_TURNS);
            entity.setMaxTodoItems(DEFAULT_MAX_TODO_ITEMS);
            entity.setMaxNoticeItems(DEFAULT_MAX_NOTICE_ITEMS);
        }
        entity.setActionPolicyJson(serializePolicyItems(mergedPolicyList));

        if (isNew) {
            save(entity);
        } else {
            updateById(entity);
        }
        return mergedPolicyList;
    }

    /**
     * 应用配置更新字段。
     *
     * @param entity 配置实体
     * @param request 更新请求
     */
    private void applyConfigUpdate(SysAiConfig entity, PlatformAiConfigUpdateRequest request) {
        if (entity == null || request == null) {
            return;
        }
        if (request.getEnabled() != null) {
            entity.setEnabled(Boolean.TRUE.equals(request.getEnabled()) ? FLAG_YES : FLAG_NO);
        }
        if (StringUtils.hasText(request.getModel())) {
            entity.setModel(request.getModel().trim());
        }
        if (request.getMaxHistoryTurns() != null) {
            entity.setMaxHistoryTurns(normalizeCount(request.getMaxHistoryTurns(), DEFAULT_MAX_HISTORY_TURNS));
        }
        if (request.getMaxTodoItems() != null) {
            entity.setMaxTodoItems(normalizeCount(request.getMaxTodoItems(), DEFAULT_MAX_TODO_ITEMS));
        }
        if (request.getMaxNoticeItems() != null) {
            entity.setMaxNoticeItems(normalizeCount(request.getMaxNoticeItems(), DEFAULT_MAX_NOTICE_ITEMS));
        }
        if (request.getPromptTemplate() != null) {
            entity.setPromptTemplate(trimToNull(request.getPromptTemplate()));
        }
    }

    /**
     * 合并默认动作策略与持久化策略。
     *
     * @param actionPolicyJson 策略 JSON
     * @return 合并后的动作策略
     */
    private List<PlatformAiActionPolicyItem> mergeActionPolicies(String actionPolicyJson) {
        List<PlatformAiActionPolicyItem> defaultPolicyList = defaultActionPolicyList();
        List<PlatformAiActionPolicyItem> persistedPolicyList = parsePolicyItems(actionPolicyJson);
        if (persistedPolicyList.isEmpty()) {
            return defaultPolicyList;
        }
        Map<String, PlatformAiActionPolicyItem> persistedPolicyMap = persistedPolicyList.stream()
                .filter(item -> item != null && StringUtils.hasText(item.getActionKey()))
                .collect(Collectors.toMap(
                        item -> item.getActionKey().trim(),
                        item -> item,
                        (left, right) -> right,
                        LinkedHashMap::new));

        List<PlatformAiActionPolicyItem> mergedPolicyList = new ArrayList<>();
        for (PlatformAiActionPolicyItem defaultItem : defaultPolicyList) {
            PlatformAiActionPolicyItem persistedItem = persistedPolicyMap.get(defaultItem.getActionKey());
            PlatformAiActionPolicyItem mergedItem = new PlatformAiActionPolicyItem();
            mergedItem.setActionKey(defaultItem.getActionKey());
            mergedItem.setActionLabel(defaultItem.getActionLabel());
            mergedItem.setEnabled(persistedItem == null ? defaultItem.isEnabled() : persistedItem.isEnabled());
            mergedItem.setRiskLevel(normalizeRiskLevel(persistedItem == null ? defaultItem.getRiskLevel() : persistedItem.getRiskLevel(),
                    defaultItem.getRiskLevel()));
            mergedPolicyList.add(mergedItem);
        }
        return mergedPolicyList;
    }

    /**
     * 解析动作策略列表。
     *
     * @param json JSON 文本
     * @return 动作策略列表
     */
    private List<PlatformAiActionPolicyItem> parsePolicyItems(String json) {
        if (!StringUtils.hasText(json)) {
            return new ArrayList<>();
        }
        try {
            List<PlatformAiActionPolicyItem> list = objectMapper.readValue(json,
                    new TypeReference<List<PlatformAiActionPolicyItem>>() {
                    });
            return list == null ? new ArrayList<>() : list;
        } catch (Exception ignored) {
            return new ArrayList<>();
        }
    }

    /**
     * 序列化动作策略列表。
     *
     * @param policyItems 动作策略列表
     * @return JSON 文本
     */
    private String serializePolicyItems(List<PlatformAiActionPolicyItem> policyItems) {
        List<PlatformAiActionPolicyItem> safePolicyList = policyItems == null ? new ArrayList<>() : policyItems.stream()
                .filter(item -> item != null && StringUtils.hasText(item.getActionKey()))
                .map(item -> {
                    PlatformAiActionPolicyItem safeItem = new PlatformAiActionPolicyItem();
                    safeItem.setActionKey(item.getActionKey().trim());
                    safeItem.setActionLabel(trimToNull(item.getActionLabel()));
                    safeItem.setEnabled(item.isEnabled());
                    safeItem.setRiskLevel(normalizeRiskLevel(item.getRiskLevel(), "low"));
                    return safeItem;
                })
                .collect(Collectors.toList());
        try {
            return objectMapper.writeValueAsString(safePolicyList);
        } catch (Exception ignored) {
            return "[]";
        }
    }

    /**
     * 构造默认动作策略。
     *
     * @return 默认策略列表
     */
    private List<PlatformAiActionPolicyItem> defaultActionPolicyList() {
        List<PlatformAiActionPolicyItem> list = new ArrayList<>();
        list.add(buildPolicyItem(ACTION_TODO_CLAIM, "签收待办", true, "low"));
        list.add(buildPolicyItem(ACTION_TODO_FINISH, "办结待办", true, "high"));
        list.add(buildPolicyItem(ACTION_NOTICE_READ, "标记消息已读", true, "low"));
        list.add(buildPolicyItem(ACTION_NOTICE_READ_ALL, "全部消息已读", true, "low"));
        list.add(buildPolicyItem(ACTION_WORKFLOW_APPROVE, "审批通过", true, "high"));
        list.add(buildPolicyItem(ACTION_WORKFLOW_REJECT, "审批驳回", true, "high"));
        list.add(buildPolicyItem(ACTION_WORKFLOW_DEFINITION_PUBLISH, "发布流程定义", true, "high"));
        return list;
    }

    /**
     * 构造策略项。
     *
     * @param actionKey 动作编码
     * @param actionLabel 动作名称
     * @param enabled 是否启用
     * @param riskLevel 风险等级
     * @return 策略项
     */
    private PlatformAiActionPolicyItem buildPolicyItem(String actionKey, String actionLabel, boolean enabled, String riskLevel) {
        PlatformAiActionPolicyItem item = new PlatformAiActionPolicyItem();
        item.setActionKey(actionKey);
        item.setActionLabel(actionLabel);
        item.setEnabled(enabled);
        item.setRiskLevel(riskLevel);
        return item;
    }

    /**
     * 转换配置实体为平台视图。
     *
     * @param entity 配置实体
     * @return 平台视图
     */
    private PlatformAiConfigView toView(SysAiConfig entity) {
        if (entity == null) {
            return buildDefaultConfigView(null);
        }
        PlatformAiConfigView view = new PlatformAiConfigView();
        view.setTenantId(entity.getTenantId());
        view.setEnabled(FLAG_YES.equals(entity.getEnabled()));
        view.setModel(StringUtils.hasText(entity.getModel()) ? entity.getModel().trim() : DEFAULT_MODEL);
        view.setMaxHistoryTurns(entity.getMaxHistoryTurns() == null ? DEFAULT_MAX_HISTORY_TURNS : entity.getMaxHistoryTurns());
        view.setMaxTodoItems(entity.getMaxTodoItems() == null ? DEFAULT_MAX_TODO_ITEMS : entity.getMaxTodoItems());
        view.setMaxNoticeItems(entity.getMaxNoticeItems() == null ? DEFAULT_MAX_NOTICE_ITEMS : entity.getMaxNoticeItems());
        view.setPromptTemplate(trimToNull(entity.getPromptTemplate()));
        view.setUpdateBy(trimToNull(entity.getUpdateBy()));
        view.setUpdateTime(entity.getUpdateTime());
        view.setConfigVersion(buildConfigVersion(entity.getUpdateTime()));
        return view;
    }

    /**
     * 构造默认配置视图。
     *
     * @param tenantId 租户编号
     * @return 默认配置视图
     */
    private PlatformAiConfigView buildDefaultConfigView(String tenantId) {
        PlatformAiConfigView view = new PlatformAiConfigView();
        view.setTenantId(tenantId);
        view.setEnabled(true);
        view.setModel(DEFAULT_MODEL);
        view.setMaxHistoryTurns(DEFAULT_MAX_HISTORY_TURNS);
        view.setMaxTodoItems(DEFAULT_MAX_TODO_ITEMS);
        view.setMaxNoticeItems(DEFAULT_MAX_NOTICE_ITEMS);
        view.setPromptTemplate(null);
        view.setConfigVersion("default");
        return view;
    }

    /**
     * 规范化计数值。
     *
     * @param value 原始值
     * @param defaultValue 默认值
     * @return 规范化值
     */
    private Integer normalizeCount(Integer value, Integer defaultValue) {
        if (value == null) {
            return defaultValue;
        }
        if (value < 1) {
            return 1;
        }
        if (value > 100) {
            return 100;
        }
        return value;
    }

    /**
     * 规范化风险等级。
     *
     * @param value 原始值
     * @param defaultValue 默认值
     * @return 规范化风险等级
     */
    private String normalizeRiskLevel(String value, String defaultValue) {
        String normalized = trimToNull(value);
        if (!StringUtils.hasText(normalized)) {
            return defaultValue;
        }
        return Objects.equals("high", normalized.toLowerCase()) ? "high" : "low";
    }

    /**
     * 构造配置版本号。
     *
     * @param updateTime 更新时间
     * @return 配置版本
     */
    private String buildConfigVersion(Date updateTime) {
        if (updateTime == null) {
            return "default";
        }
        return "v" + updateTime.getTime();
    }

    /**
     * 规范化租户编号。
     *
     * @param tenantId 原始租户编号
     * @return 规范化租户编号
     */
    private String normalizeTenantId(String tenantId) {
        return StringUtils.hasText(tenantId) ? tenantId.trim() : null;
    }

    /**
     * 去除空白并将空字符串转为 null。
     *
     * @param value 原始字符串
     * @return 规范化字符串
     */
    private String trimToNull(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }
}

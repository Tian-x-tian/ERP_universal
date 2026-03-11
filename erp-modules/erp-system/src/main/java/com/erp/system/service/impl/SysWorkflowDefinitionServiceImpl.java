package com.erp.system.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.erp.system.domain.SysWorkflowDefinition;
import com.erp.system.domain.SysWorkflowInstance;
import com.erp.system.mapper.SysWorkflowDefinitionMapper;
import com.erp.system.mapper.SysWorkflowInstanceMapper;
import com.erp.system.service.ISysWorkflowDefinitionService;
import com.erp.system.support.TenantWriteGuard;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * 流程定义服务实现
 */
@Service
public class SysWorkflowDefinitionServiceImpl extends ServiceImpl<SysWorkflowDefinitionMapper, SysWorkflowDefinition>
        implements ISysWorkflowDefinitionService {

    private static final String STATUS_DRAFT = "0";
    private static final String STATUS_PUBLISHED = "1";
    private static final String STATUS_DISABLED = "2";

    private static final Set<String> CATEGORY_SET = new HashSet<>(Arrays.asList(
            "purchaseReq", "purchase", "expense", "contract", "seal", "onboard", "offboard", "transfer", "custom",
            "purchaseApprove", "stamp", "inventoryTransfer"));

    private final SysWorkflowInstanceMapper workflowInstanceMapper;
    private final ObjectMapper objectMapper;

    public SysWorkflowDefinitionServiceImpl(SysWorkflowInstanceMapper workflowInstanceMapper) {
        this.workflowInstanceMapper = workflowInstanceMapper;
        this.objectMapper = new ObjectMapper();
    }

    /**
     * 查询流程定义列表。
     *
     * @param processName 流程名称关键字
     * @param processKey  流程标识关键字
     * @param category    流程分类
     * @param status      状态
     * @return 流程定义列表
     */
    @Override
    public List<SysWorkflowDefinition> selectList(String processName, String processKey, String category, String status) {
        LambdaQueryWrapper<SysWorkflowDefinition> queryWrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(processName)) {
            queryWrapper.like(SysWorkflowDefinition::getProcessName, processName.trim());
        }
        if (StringUtils.hasText(processKey)) {
            queryWrapper.like(SysWorkflowDefinition::getProcessKey, processKey.trim());
        }
        if (StringUtils.hasText(category)) {
            List<String> categoryCandidates = resolveCategoryCandidates(normalizeCategory(category));
            if (categoryCandidates.size() == 1) {
                queryWrapper.eq(SysWorkflowDefinition::getCategory, categoryCandidates.get(0));
            } else {
                queryWrapper.in(SysWorkflowDefinition::getCategory, categoryCandidates);
            }
        }
        if (StringUtils.hasText(status)) {
            queryWrapper.eq(SysWorkflowDefinition::getStatus, normalizeStatus(status, STATUS_DRAFT));
        }
        queryWrapper.orderByAsc(SysWorkflowDefinition::getProcessKey)
                .orderByDesc(SysWorkflowDefinition::getVersion)
                .orderByDesc(SysWorkflowDefinition::getUpdateTime)
                .orderByDesc(SysWorkflowDefinition::getCreateTime);
        return list(queryWrapper);
    }

    /**
     * 新增流程定义。
     *
     * @param definition 流程定义
     * @param operator   操作人账号
     * @return 新增结果
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean createDefinition(SysWorkflowDefinition definition, String operator) {
        if (!isValidDefinition(definition) || !StringUtils.hasText(operator)) {
            return false;
        }
        String processKey = definition.getProcessKey().trim();
        String processName = definition.getProcessName().trim();
        String tenantId = resolveTenantId();
        Date now = new Date();
        int nextVersion = resolveNextVersion(processKey);

        definition.setTenantId(tenantId);
        definition.setProcessKey(processKey);
        definition.setProcessName(processName);
        definition.setCategory(normalizeCategory(definition.getCategory()));
        definition.setVersion(resolveVersion(definition.getVersion(), nextVersion));
        definition.setStatus(normalizeStatus(definition.getStatus(), STATUS_DRAFT));
        definition.setCreateBy(operator);
        definition.setCreateTime(now);
        definition.setUpdateBy(operator);
        definition.setUpdateTime(now);

        if (count(new LambdaQueryWrapper<SysWorkflowDefinition>()
                .eq(SysWorkflowDefinition::getProcessKey, definition.getProcessKey())
                .eq(SysWorkflowDefinition::getVersion, definition.getVersion())) > 0) {
            return false;
        }

        if (!save(definition)) {
            return false;
        }
        if (STATUS_PUBLISHED.equals(definition.getStatus())) {
            return publishDefinition(definition.getDefinitionId(), operator);
        }
        return true;
    }

    /**
     * 修改流程定义。
     *
     * @param definition 流程定义
     * @param operator   操作人账号
     * @return 修改结果
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean updateDefinition(SysWorkflowDefinition definition, String operator) {
        if (definition == null || definition.getDefinitionId() == null || !StringUtils.hasText(operator)) {
            return false;
        }
        SysWorkflowDefinition existed = getById(definition.getDefinitionId());
        if (existed == null) {
            return false;
        }
        if (!STATUS_DRAFT.equals(existed.getStatus())) {
            return false;
        }
        if (hasEffectiveInstances(existed.getDefinitionId())) {
            return false;
        }
        if (StringUtils.hasText(definition.getProcessKey())
                && !existed.getProcessKey().equals(definition.getProcessKey().trim())) {
            return false;
        }

        SysWorkflowDefinition updateEntity = new SysWorkflowDefinition();
        updateEntity.setDefinitionId(existed.getDefinitionId());
        updateEntity.setProcessName(StringUtils.hasText(definition.getProcessName()) ? definition.getProcessName().trim() : existed.getProcessName());
        updateEntity.setCategory(StringUtils.hasText(definition.getCategory()) ? normalizeCategory(definition.getCategory()) : existed.getCategory());
        updateEntity.setFormSchema(definition.getFormSchema());
        updateEntity.setModelContent(definition.getModelContent());
        updateEntity.setRemark(definition.getRemark());
        updateEntity.setUpdateBy(operator);
        updateEntity.setUpdateTime(new Date());
        return updateById(updateEntity);
    }

    /**
     * 发布流程定义。
     *
     * @param definitionId 流程定义ID
     * @param operator     操作人账号
     * @return 发布结果
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean publishDefinition(Long definitionId, String operator) {
        if (definitionId == null || !StringUtils.hasText(operator)) {
            return false;
        }
        SysWorkflowDefinition definition = getById(definitionId);
        if (definition == null) {
            return false;
        }
        Date now = new Date();

        LambdaUpdateWrapper<SysWorkflowDefinition> disableOtherWrapper = new LambdaUpdateWrapper<>();
        disableOtherWrapper.eq(SysWorkflowDefinition::getProcessKey, definition.getProcessKey())
                .eq(SysWorkflowDefinition::getStatus, STATUS_PUBLISHED)
                .ne(SysWorkflowDefinition::getDefinitionId, definition.getDefinitionId())
                .set(SysWorkflowDefinition::getStatus, STATUS_DISABLED)
                .set(SysWorkflowDefinition::getUpdateBy, operator)
                .set(SysWorkflowDefinition::getUpdateTime, now);
        baseMapper.update(null, disableOtherWrapper);

        SysWorkflowDefinition updateEntity = new SysWorkflowDefinition();
        updateEntity.setDefinitionId(definitionId);
        updateEntity.setStatus(STATUS_PUBLISHED);
        updateEntity.setPublishBy(operator);
        updateEntity.setPublishTime(now);
        updateEntity.setUpdateBy(operator);
        updateEntity.setUpdateTime(now);
        if (!updateById(updateEntity)) {
            return false;
        }

        // 防御性兜底：再次确保同一 processKey 下仅保留一个已发布版本。
        LambdaUpdateWrapper<SysWorkflowDefinition> cleanupWrapper = new LambdaUpdateWrapper<>();
        cleanupWrapper.eq(SysWorkflowDefinition::getProcessKey, definition.getProcessKey())
                .eq(SysWorkflowDefinition::getStatus, STATUS_PUBLISHED)
                .ne(SysWorkflowDefinition::getDefinitionId, definitionId)
                .set(SysWorkflowDefinition::getStatus, STATUS_DISABLED)
                .set(SysWorkflowDefinition::getUpdateBy, operator)
                .set(SysWorkflowDefinition::getUpdateTime, now);
        baseMapper.update(null, cleanupWrapper);
        return true;
    }

    /**
     * 停用流程定义。
     *
     * @param definitionId 流程定义ID
     * @param operator     操作人账号
     * @return 停用结果
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean disableDefinition(Long definitionId, String operator) {
        if (definitionId == null || !StringUtils.hasText(operator)) {
            return false;
        }
        SysWorkflowDefinition existed = getById(definitionId);
        if (existed == null) {
            return false;
        }
        SysWorkflowDefinition updateEntity = new SysWorkflowDefinition();
        updateEntity.setDefinitionId(definitionId);
        updateEntity.setStatus(STATUS_DISABLED);
        updateEntity.setUpdateBy(operator);
        updateEntity.setUpdateTime(new Date());
        return updateById(updateEntity);
    }

    /**
     * 按流程标识查询版本历史。
     *
     * @param processKey 流程标识
     * @return 版本历史列表（按版本倒序）
     */
    @Override
    public List<SysWorkflowDefinition> selectHistoryByProcessKey(String processKey) {
        if (!StringUtils.hasText(processKey)) {
            return Collections.emptyList();
        }
        return list(new LambdaQueryWrapper<SysWorkflowDefinition>()
                .eq(SysWorkflowDefinition::getProcessKey, processKey.trim())
                .orderByDesc(SysWorkflowDefinition::getVersion)
                .orderByDesc(SysWorkflowDefinition::getUpdateTime)
                .orderByDesc(SysWorkflowDefinition::getCreateTime));
    }

    /**
     * 从已有流程定义创建新版本草稿。
     *
     * @param definitionId 来源流程定义ID
     * @param operator     操作人账号
     * @return 新版本草稿，创建失败返回 null
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public SysWorkflowDefinition createNewVersion(Long definitionId, String operator) {
        if (definitionId == null || !StringUtils.hasText(operator)) {
            return null;
        }
        SysWorkflowDefinition source = getById(definitionId);
        if (source == null || !StringUtils.hasText(source.getProcessKey())) {
            return null;
        }
        SysWorkflowDefinition draft = new SysWorkflowDefinition();
        Date now = new Date();
        draft.setTenantId(source.getTenantId());
        draft.setProcessKey(source.getProcessKey());
        draft.setProcessName(source.getProcessName());
        draft.setCategory(normalizeCategory(source.getCategory()));
        draft.setVersion(resolveNextVersion(source.getProcessKey()));
        draft.setStatus(STATUS_DRAFT);
        draft.setFormSchema(source.getFormSchema());
        draft.setModelContent(source.getModelContent());
        draft.setRemark(source.getRemark());
        draft.setCreateBy(operator);
        draft.setCreateTime(now);
        draft.setUpdateBy(operator);
        draft.setUpdateTime(now);
        if (!save(draft)) {
            return null;
        }
        return draft;
    }

    /**
     * 删除流程定义（受保护删除）。
     *
     * @param definitionIds 流程定义ID集合
     * @param operator      操作人账号
     * @return 删除结果
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean removeDefinitions(List<Long> definitionIds, String operator) {
        if (definitionIds == null || definitionIds.isEmpty() || !StringUtils.hasText(operator)) {
            return false;
        }
        List<SysWorkflowDefinition> targetDefinitions = listByIds(definitionIds);
        if (targetDefinitions.size() != definitionIds.size()) {
            return false;
        }
        for (SysWorkflowDefinition definition : targetDefinitions) {
            if (definition == null || definition.getDefinitionId() == null) {
                return false;
            }
            if (STATUS_PUBLISHED.equals(definition.getStatus())) {
                return false;
            }
            if (hasEffectiveInstances(definition.getDefinitionId())) {
                return false;
            }
        }
        return baseMapper.deleteBatchIds(definitionIds) > 0;
    }

    /**
     * 按流程标识查询最新发布版本。
     *
     * @param processKey 流程标识
     * @return 已发布流程定义
     */
    @Override
    public SysWorkflowDefinition selectLatestPublishedByProcessKey(String processKey) {
        if (!StringUtils.hasText(processKey)) {
            return null;
        }
        return getOne(new LambdaQueryWrapper<SysWorkflowDefinition>()
                .eq(SysWorkflowDefinition::getProcessKey, processKey.trim())
                .eq(SysWorkflowDefinition::getStatus, STATUS_PUBLISHED)
                .orderByDesc(SysWorkflowDefinition::getVersion)
                .last("LIMIT 1"));
    }

    /**
     * 校验流程定义新增参数是否完整。
     *
     * @param definition 流程定义
     * @return true 表示参数合法
     */
    private boolean isValidDefinition(SysWorkflowDefinition definition) {
        return definition != null
                && StringUtils.hasText(definition.getProcessKey())
                && StringUtils.hasText(definition.getProcessName())
                && StringUtils.hasText(definition.getModelContent())
                && isValidModelContent(definition.getModelContent());
    }

    /**
     * 校验流程模型结构完整性。
     *
     * @param modelContent 流程模型JSON
     * @return true 表示结构合法
     */
    private boolean isValidModelContent(String modelContent) {
        if (!StringUtils.hasText(modelContent)) {
            return false;
        }
        try {
            Map<String, Object> root = objectMapper.readValue(modelContent, new TypeReference<Map<String, Object>>() {
            });
            Object nodesObj = root.get("nodes");
            if (!(nodesObj instanceof List) || ((List<?>) nodesObj).isEmpty()) {
                return false;
            }
            LinkedHashSet<String> nodeKeySet = new LinkedHashSet<>();
            LinkedHashSet<String> startNodeSet = new LinkedHashSet<>();
            int approvalNodeCount = 0;
            for (Object nodeItem : (List<?>) nodesObj) {
                if (!(nodeItem instanceof Map)) {
                    return false;
                }
                Map<?, ?> nodeMap = (Map<?, ?>) nodeItem;
                String nodeKey = readString(nodeMap, "nodeKey", "id", "key");
                if (!StringUtils.hasText(nodeKey) || !nodeKeySet.add(nodeKey.trim())) {
                    return false;
                }
                String nodeType = normalizeNodeType(readString(nodeMap, "nodeType", "type"));
                if ("start".equals(nodeType)) {
                    startNodeSet.add(nodeKey.trim());
                }
                if ("approval".equals(nodeType)) {
                    approvalNodeCount++;
                }
            }
            if (approvalNodeCount <= 0) {
                return false;
            }

            String startNodeKey = readString(root, "startNodeKey");
            if (StringUtils.hasText(startNodeKey)) {
                if (!nodeKeySet.contains(startNodeKey.trim())) {
                    return false;
                }
            } else if (nodeKeySet.size() > 1 && startNodeSet.isEmpty()) {
                return false;
            }

            Object edgesObj = root.get("edges");
            if (edgesObj instanceof List) {
                for (Object edgeItem : (List<?>) edgesObj) {
                    if (!(edgeItem instanceof Map)) {
                        return false;
                    }
                    Map<?, ?> edgeMap = (Map<?, ?>) edgeItem;
                    String from = readString(edgeMap, "from", "source", "sourceNodeKey");
                    String to = readString(edgeMap, "to", "target", "targetNodeKey");
                    if (!StringUtils.hasText(from) || !StringUtils.hasText(to)) {
                        return false;
                    }
                    if (!nodeKeySet.contains(from.trim()) || !nodeKeySet.contains(to.trim())) {
                        return false;
                    }
                }
            }
            return true;
        } catch (Exception ignore) {
            return false;
        }
    }

    /**
     * 读取映射中的首个非空字符串。
     *
     * @param source 映射对象
     * @param keys   候选字段
     * @return 字符串值
     */
    private String readString(Map<?, ?> source, String... keys) {
        if (source == null || keys == null) {
            return null;
        }
        for (String key : keys) {
            Object value = source.get(key);
            if (value != null && StringUtils.hasText(String.valueOf(value))) {
                return String.valueOf(value);
            }
        }
        return null;
    }

    /**
     * 规范化流程节点类型。
     *
     * @param nodeType 原始类型
     * @return 规范化类型
     */
    private String normalizeNodeType(String nodeType) {
        if (!StringUtils.hasText(nodeType)) {
            return "approval";
        }
        String normalized = nodeType.trim().toLowerCase(Locale.ROOT);
        if ("start".equals(normalized) || "end".equals(normalized) || "cc".equals(normalized)
                || "gateway".equals(normalized) || "parallel".equals(normalized)) {
            return normalized;
        }
        return "approval";
    }

    /**
     * 校验流程定义是否已产生生效实例。
     *
     * @param definitionId 流程定义ID
     * @return true 表示存在已生效实例
     */
    private boolean hasEffectiveInstances(Long definitionId) {
        if (definitionId == null) {
            return false;
        }
        Long count = workflowInstanceMapper.selectCount(new LambdaQueryWrapper<SysWorkflowInstance>()
                .eq(SysWorkflowInstance::getDefinitionId, definitionId));
        return count != null && count > 0;
    }

    /**
     * 计算流程标识的下一个版本号。
     *
     * @param processKey 流程标识
     * @return 下一个版本号
     */
    private int resolveNextVersion(String processKey) {
        SysWorkflowDefinition latest = getOne(new LambdaQueryWrapper<SysWorkflowDefinition>()
                .eq(SysWorkflowDefinition::getProcessKey, processKey)
                .orderByDesc(SysWorkflowDefinition::getVersion)
                .last("LIMIT 1"));
        int currentVersion = latest == null || latest.getVersion() == null ? 0 : latest.getVersion();
        return currentVersion + 1;
    }

    /**
     * 解析流程版本号。
     *
     * @param version     原始版本号
     * @param nextVersion 建议版本号
     * @return 规范化版本号
     */
    private Integer resolveVersion(Integer version, int nextVersion) {
        if (version == null || version <= 0) {
            return nextVersion;
        }
        return version;
    }

    /**
     * 规范化流程分类值。
     *
     * @param category 原始流程分类
     * @return 规范化后的流程分类
     */
    private String normalizeCategory(String category) {
        if (!StringUtils.hasText(category)) {
            return "custom";
        }
        String normalized = category.trim();
        if ("purchaseApprove".equals(normalized)) {
            return "purchase";
        }
        if ("inventoryTransfer".equals(normalized)) {
            return "transfer";
        }
        if ("stamp".equals(normalized)) {
            return "seal";
        }
        if (CATEGORY_SET.contains(normalized)) {
            return normalized;
        }
        return "custom";
    }

    /**
     * 根据分类编码生成兼容查询候选列表。
     *
     * @param category 规范化分类编码
     * @return 候选分类集合
     */
    private List<String> resolveCategoryCandidates(String category) {
        if (!StringUtils.hasText(category)) {
            return Collections.singletonList("custom");
        }
        if ("purchase".equals(category)) {
            return Arrays.asList("purchase", "purchaseApprove");
        }
        if ("transfer".equals(category)) {
            return Arrays.asList("transfer", "inventoryTransfer");
        }
        if ("seal".equals(category)) {
            return Arrays.asList("seal", "stamp");
        }
        List<String> single = new ArrayList<>(1);
        single.add(category);
        return single;
    }

    /**
     * 规范化流程定义状态。
     *
     * @param status       原始状态
     * @param defaultValue 默认状态
     * @return 规范化后的状态值
     */
    private String normalizeStatus(String status, String defaultValue) {
        if (!StringUtils.hasText(status)) {
            return defaultValue;
        }
        String normalizedStatus = status.trim();
        if (STATUS_DRAFT.equals(normalizedStatus) || STATUS_PUBLISHED.equals(normalizedStatus) || STATUS_DISABLED.equals(normalizedStatus)) {
            return normalizedStatus;
        }
        return defaultValue;
    }

    /**
     * 解析当前租户编号，缺失时回退到平台租户。
     *
     * @return 租户编号
     */
    private String resolveTenantId() {
        String tenantId = TenantWriteGuard.currentTenantId();
        return StringUtils.hasText(tenantId) ? tenantId : "000000";
    }
}

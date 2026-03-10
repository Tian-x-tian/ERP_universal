package com.erp.system.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.erp.system.domain.SysWorkflowDefinition;
import com.erp.system.mapper.SysWorkflowDefinitionMapper;
import com.erp.system.service.ISysWorkflowDefinitionService;
import com.erp.system.support.TenantWriteGuard;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.Date;
import java.util.List;

/**
 * 流程定义服务实现
 */
@Service
public class SysWorkflowDefinitionServiceImpl extends ServiceImpl<SysWorkflowDefinitionMapper, SysWorkflowDefinition>
        implements ISysWorkflowDefinitionService {

    private static final String STATUS_DRAFT = "0";
    private static final String STATUS_PUBLISHED = "1";
    private static final String STATUS_DISABLED = "2";

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
            queryWrapper.eq(SysWorkflowDefinition::getCategory, category.trim());
        }
        if (StringUtils.hasText(status)) {
            queryWrapper.eq(SysWorkflowDefinition::getStatus, normalizeStatus(status, STATUS_DRAFT));
        }
        queryWrapper.orderByDesc(SysWorkflowDefinition::getUpdateTime)
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
        if (!isValidDefinition(definition)) {
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
        definition.setVersion(definition.getVersion() != null && definition.getVersion() > 0 ? definition.getVersion() : nextVersion);
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
        update(disableOtherWrapper);

        SysWorkflowDefinition updateEntity = new SysWorkflowDefinition();
        updateEntity.setDefinitionId(definitionId);
        updateEntity.setStatus(STATUS_PUBLISHED);
        updateEntity.setPublishBy(operator);
        updateEntity.setPublishTime(now);
        updateEntity.setUpdateBy(operator);
        updateEntity.setUpdateTime(now);
        return updateById(updateEntity);
    }

    /**
     * 停用流程定义。
     *
     * @param definitionId 流程定义ID
     * @param operator     操作人账号
     * @return 停用结果
     */
    @Override
    public boolean disableDefinition(Long definitionId, String operator) {
        if (definitionId == null || !StringUtils.hasText(operator)) {
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
                && StringUtils.hasText(definition.getProcessName());
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
     * 规范化流程分类值。
     *
     * @param category 原始流程分类
     * @return 规范化后的流程分类
     */
    private String normalizeCategory(String category) {
        return StringUtils.hasText(category) ? category.trim() : "custom";
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


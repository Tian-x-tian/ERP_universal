package com.erp.system.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.erp.system.domain.MdmCostCenter;
import com.erp.system.domain.MdmOrg;
import com.erp.system.domain.MdmProject;
import com.erp.workflow.contract.domain.SysWorkflowInstance;
import com.erp.system.mapper.MdmCostCenterMapper;
import com.erp.system.mapper.MdmOrgMapper;
import com.erp.system.mapper.MdmProjectMapper;
import com.erp.system.service.IMdmAuditTrailService;
import com.erp.system.service.IWorkflowBusinessCallback;
import com.erp.system.support.MdmChangeTypeSupport;
import com.erp.system.support.MdmDomainTypeSupport;
import com.erp.system.support.MdmStatusSupport;
import com.erp.system.support.MdmValueSupport;
import com.erp.system.support.MdmWorkflowActionSupport;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.Date;
import java.util.Map;

/**
 * 维度主数据工作流终态回调实现。
 */
@Service
public class MdmDimensionWorkflowCallbackServiceImpl implements IWorkflowBusinessCallback {
    private static final String DEL_FLAG_EXIST = "0";
    private static final String BUSINESS_TYPE_ORG = "MDM_ORG";
    private static final String BUSINESS_TYPE_COST_CENTER = "MDM_COST_CENTER";
    private static final String BUSINESS_TYPE_PROJECT = "MDM_PROJECT";
    private static final String META_KEY_ORG = "__mdmOrgMeta";
    private static final String META_KEY_COST_CENTER = "__mdmCostCenterMeta";
    private static final String META_KEY_PROJECT = "__mdmProjectMeta";

    private final MdmOrgMapper orgMapper;
    private final MdmCostCenterMapper costCenterMapper;
    private final MdmProjectMapper projectMapper;
    private final IMdmAuditTrailService auditTrailService;
    private final ObjectMapper objectMapper;

    public MdmDimensionWorkflowCallbackServiceImpl(MdmOrgMapper orgMapper,
            MdmCostCenterMapper costCenterMapper,
            MdmProjectMapper projectMapper,
            IMdmAuditTrailService auditTrailService) {
        this.orgMapper = orgMapper;
        this.costCenterMapper = costCenterMapper;
        this.projectMapper = projectMapper;
        this.auditTrailService = auditTrailService;
        this.objectMapper = new ObjectMapper();
    }

    @Override
    public boolean supports(String businessType) {
        String normalized = StringUtils.trimWhitespace(businessType);
        return BUSINESS_TYPE_ORG.equalsIgnoreCase(normalized)
                || BUSINESS_TYPE_COST_CENTER.equalsIgnoreCase(normalized)
                || BUSINESS_TYPE_PROJECT.equalsIgnoreCase(normalized);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void onWorkflowCompleted(SysWorkflowInstance instance) {
        String businessType = StringUtils.trimWhitespace(instance == null ? null : instance.getBusinessType());
        if (BUSINESS_TYPE_ORG.equalsIgnoreCase(businessType)) {
            handleOrgCompleted(instance);
            return;
        }
        if (BUSINESS_TYPE_COST_CENTER.equalsIgnoreCase(businessType)) {
            handleCostCenterCompleted(instance);
            return;
        }
        if (BUSINESS_TYPE_PROJECT.equalsIgnoreCase(businessType)) {
            handleProjectCompleted(instance);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void onWorkflowRejected(SysWorkflowInstance instance) {
        rollbackSubmittedDraft(instance);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void onWorkflowWithdrawn(SysWorkflowInstance instance) {
        rollbackSubmittedDraft(instance);
    }

    private void handleOrgCompleted(SysWorkflowInstance instance) {
        Map<String, Object> meta = readMeta(instance, META_KEY_ORG);
        String action = readString(meta.get("action"));
        Long orgId = readLong(meta.get("orgId"));
        Integer baseVersionNo = readInteger(meta.get("baseVersionNo"));
        if (orgId == null || !StringUtils.hasText(action)) {
            return;
        }
        if (MdmWorkflowActionSupport.ACTIVATE.equalsIgnoreCase(action)) {
            activateOrg(orgId, baseVersionNo, instance);
            return;
        }
        if (MdmWorkflowActionSupport.UPDATE.equalsIgnoreCase(action)) {
            applyOrgChange(orgId, baseVersionNo, meta, instance);
            return;
        }
        if (MdmWorkflowActionSupport.DISABLE.equalsIgnoreCase(action)) {
            disableOrg(orgId, baseVersionNo, instance);
        }
    }

    private void handleCostCenterCompleted(SysWorkflowInstance instance) {
        Map<String, Object> meta = readMeta(instance, META_KEY_COST_CENTER);
        String action = readString(meta.get("action"));
        Long ccId = readLong(meta.get("ccId"));
        Integer baseVersionNo = readInteger(meta.get("baseVersionNo"));
        if (ccId == null || !StringUtils.hasText(action)) {
            return;
        }
        if (MdmWorkflowActionSupport.ACTIVATE.equalsIgnoreCase(action)) {
            activateCostCenter(ccId, baseVersionNo, instance);
            return;
        }
        if (MdmWorkflowActionSupport.UPDATE.equalsIgnoreCase(action)) {
            applyCostCenterChange(ccId, baseVersionNo, meta, instance);
            return;
        }
        if (MdmWorkflowActionSupport.DISABLE.equalsIgnoreCase(action)) {
            disableCostCenter(ccId, baseVersionNo, instance);
        }
    }

    private void handleProjectCompleted(SysWorkflowInstance instance) {
        Map<String, Object> meta = readMeta(instance, META_KEY_PROJECT);
        String action = readString(meta.get("action"));
        Long projectId = readLong(meta.get("projectId"));
        Integer baseVersionNo = readInteger(meta.get("baseVersionNo"));
        if (projectId == null || !StringUtils.hasText(action)) {
            return;
        }
        if (MdmWorkflowActionSupport.ACTIVATE.equalsIgnoreCase(action)) {
            activateProject(projectId, baseVersionNo, instance);
            return;
        }
        if (MdmWorkflowActionSupport.UPDATE.equalsIgnoreCase(action)) {
            applyProjectChange(projectId, baseVersionNo, meta, instance);
            return;
        }
        if (MdmWorkflowActionSupport.DISABLE.equalsIgnoreCase(action)) {
            disableProject(projectId, baseVersionNo, instance);
        }
    }

    private void rollbackSubmittedDraft(SysWorkflowInstance instance) {
        String businessType = StringUtils.trimWhitespace(instance == null ? null : instance.getBusinessType());
        if (BUSINESS_TYPE_ORG.equalsIgnoreCase(businessType)) {
            rollbackOrgDraft(instance);
            return;
        }
        if (BUSINESS_TYPE_COST_CENTER.equalsIgnoreCase(businessType)) {
            rollbackCostCenterDraft(instance);
            return;
        }
        if (BUSINESS_TYPE_PROJECT.equalsIgnoreCase(businessType)) {
            rollbackProjectDraft(instance);
        }
    }

    private void rollbackOrgDraft(SysWorkflowInstance instance) {
        Map<String, Object> meta = readMeta(instance, META_KEY_ORG);
        String action = readString(meta.get("action"));
        Long orgId = readLong(meta.get("orgId"));
        if (orgId == null) {
            return;
        }
        String rollbackStatus = MdmWorkflowActionSupport.ACTIVATE.equalsIgnoreCase(action)
                ? MdmStatusSupport.DRAFT
                : MdmStatusSupport.ACTIVE;
        orgMapper.update(new MdmOrg(), new LambdaUpdateWrapper<MdmOrg>()
                .eq(MdmOrg::getOrgId, orgId)
                .eq(MdmOrg::getDelFlag, DEL_FLAG_EXIST)
                .eq(MdmOrg::getStatus, MdmStatusSupport.SUBMITTED)
                .set(MdmOrg::getStatus, rollbackStatus)
                .set(MdmOrg::getUpdateBy, resolveOperator(instance))
                .set(MdmOrg::getUpdateTime, new Date()));
    }

    private void rollbackCostCenterDraft(SysWorkflowInstance instance) {
        Map<String, Object> meta = readMeta(instance, META_KEY_COST_CENTER);
        String action = readString(meta.get("action"));
        Long ccId = readLong(meta.get("ccId"));
        if (ccId == null) {
            return;
        }
        String rollbackStatus = MdmWorkflowActionSupport.ACTIVATE.equalsIgnoreCase(action)
                ? MdmStatusSupport.DRAFT
                : MdmStatusSupport.ACTIVE;
        costCenterMapper.update(new MdmCostCenter(), new LambdaUpdateWrapper<MdmCostCenter>()
                .eq(MdmCostCenter::getCcId, ccId)
                .eq(MdmCostCenter::getDelFlag, DEL_FLAG_EXIST)
                .eq(MdmCostCenter::getStatus, MdmStatusSupport.SUBMITTED)
                .set(MdmCostCenter::getStatus, rollbackStatus)
                .set(MdmCostCenter::getUpdateBy, resolveOperator(instance))
                .set(MdmCostCenter::getUpdateTime, new Date()));
    }

    private void rollbackProjectDraft(SysWorkflowInstance instance) {
        Map<String, Object> meta = readMeta(instance, META_KEY_PROJECT);
        String action = readString(meta.get("action"));
        Long projectId = readLong(meta.get("projectId"));
        if (projectId == null) {
            return;
        }
        String rollbackStatus = MdmWorkflowActionSupport.ACTIVATE.equalsIgnoreCase(action)
                ? MdmStatusSupport.DRAFT
                : MdmStatusSupport.ACTIVE;
        projectMapper.update(new MdmProject(), new LambdaUpdateWrapper<MdmProject>()
                .eq(MdmProject::getProjectId, projectId)
                .eq(MdmProject::getDelFlag, DEL_FLAG_EXIST)
                .eq(MdmProject::getStatus, MdmStatusSupport.SUBMITTED)
                .set(MdmProject::getStatus, rollbackStatus)
                .set(MdmProject::getUpdateBy, resolveOperator(instance))
                .set(MdmProject::getUpdateTime, new Date()));
    }

    private void activateOrg(Long orgId, Integer baseVersionNo, SysWorkflowInstance instance) {
        MdmOrg before = loadOrg(orgId);
        ensureVersion(before, baseVersionNo, "组织");
        boolean updated = orgMapper.update(new MdmOrg(), new LambdaUpdateWrapper<MdmOrg>()
                .eq(MdmOrg::getOrgId, orgId)
                .eq(MdmOrg::getDelFlag, DEL_FLAG_EXIST)
                .eq(MdmOrg::getStatus, MdmStatusSupport.SUBMITTED)
                .eq(baseVersionNo != null, MdmOrg::getVersionNo, baseVersionNo)
                .set(MdmOrg::getStatus, MdmStatusSupport.ACTIVE)
                .set(MdmOrg::getUpdateBy, resolveOperator(instance))
                .set(MdmOrg::getUpdateTime, new Date())) > 0;
        if (!updated) {
            throw new IllegalStateException("组织状态已变化，无法完成审批回写");
        }
        auditTrailService.record(MdmDomainTypeSupport.ORG, orgId, MdmChangeTypeSupport.STATUS, loadOrg(orgId).getVersionNo(), MdmStatusSupport.ACTIVE, before, loadOrg(orgId));
    }

    private void applyOrgChange(Long orgId, Integer baseVersionNo, Map<String, Object> meta, SysWorkflowInstance instance) {
        MdmOrg before = loadOrg(orgId);
        ensureVersion(before, baseVersionNo, "组织");
        if (!MdmStatusSupport.isSubmitted(before.getStatus())) {
            throw new IllegalStateException("组织状态已变化，请重新发起审批");
        }
        MdmOrg afterOrg = objectMapper.convertValue(meta.get("afterOrg"), MdmOrg.class);
        MdmOrg updateEntity = new MdmOrg();
        BeanUtils.copyProperties(afterOrg, updateEntity);
        updateEntity.setOrgId(orgId);
        updateEntity.setTenantId(before.getTenantId());
        updateEntity.setOrgCode(before.getOrgCode());
        updateEntity.setStatus(MdmStatusSupport.ACTIVE);
        updateEntity.setVersionNo(MdmValueSupport.resolveNextVersionNo(before.getVersionNo()));
        updateEntity.setUpdateBy(resolveOperator(instance));
        updateEntity.setCreateBy(null);
        updateEntity.setCreateTime(null);
        updateEntity.setDelFlag(null);
        boolean updated = orgMapper.update(updateEntity, new LambdaUpdateWrapper<MdmOrg>()
                .eq(MdmOrg::getOrgId, orgId)
                .eq(MdmOrg::getDelFlag, DEL_FLAG_EXIST)
                .eq(MdmOrg::getStatus, MdmStatusSupport.SUBMITTED)
                .eq(baseVersionNo != null, MdmOrg::getVersionNo, baseVersionNo)) > 0;
        if (!updated) {
            throw new IllegalStateException("组织版本已变化，请重新发起审批");
        }
        auditTrailService.record(MdmDomainTypeSupport.ORG, orgId, MdmChangeTypeSupport.UPDATE, loadOrg(orgId).getVersionNo(), loadOrg(orgId).getStatus(), before, loadOrg(orgId));
    }

    private void disableOrg(Long orgId, Integer baseVersionNo, SysWorkflowInstance instance) {
        MdmOrg before = loadOrg(orgId);
        ensureVersion(before, baseVersionNo, "组织");
        if (!MdmStatusSupport.isSubmitted(before.getStatus())) {
            throw new IllegalStateException("组织状态已变化，请重新发起审批");
        }
        MdmOrg updateEntity = new MdmOrg();
        updateEntity.setOrgId(orgId);
        updateEntity.setStatus(MdmStatusSupport.DISABLED);
        updateEntity.setVersionNo(MdmValueSupport.resolveNextVersionNo(before.getVersionNo()));
        updateEntity.setUpdateBy(resolveOperator(instance));
        boolean updated = orgMapper.update(updateEntity, new LambdaUpdateWrapper<MdmOrg>()
                .eq(MdmOrg::getOrgId, orgId)
                .eq(MdmOrg::getDelFlag, DEL_FLAG_EXIST)
                .eq(MdmOrg::getStatus, MdmStatusSupport.SUBMITTED)
                .eq(baseVersionNo != null, MdmOrg::getVersionNo, baseVersionNo)) > 0;
        if (!updated) {
            throw new IllegalStateException("组织版本已变化，请重新发起审批");
        }
        auditTrailService.record(MdmDomainTypeSupport.ORG, orgId, MdmChangeTypeSupport.STATUS, loadOrg(orgId).getVersionNo(), MdmStatusSupport.DISABLED, before, loadOrg(orgId));
    }

    private void activateCostCenter(Long ccId, Integer baseVersionNo, SysWorkflowInstance instance) {
        MdmCostCenter before = loadCostCenter(ccId);
        ensureVersion(before, baseVersionNo, "成本中心");
        boolean updated = costCenterMapper.update(new MdmCostCenter(), new LambdaUpdateWrapper<MdmCostCenter>()
                .eq(MdmCostCenter::getCcId, ccId)
                .eq(MdmCostCenter::getDelFlag, DEL_FLAG_EXIST)
                .eq(MdmCostCenter::getStatus, MdmStatusSupport.SUBMITTED)
                .eq(baseVersionNo != null, MdmCostCenter::getVersionNo, baseVersionNo)
                .set(MdmCostCenter::getStatus, MdmStatusSupport.ACTIVE)
                .set(MdmCostCenter::getUpdateBy, resolveOperator(instance))
                .set(MdmCostCenter::getUpdateTime, new Date())) > 0;
        if (!updated) {
            throw new IllegalStateException("成本中心状态已变化，无法完成审批回写");
        }
        auditTrailService.record(MdmDomainTypeSupport.COST_CENTER, ccId, MdmChangeTypeSupport.STATUS, loadCostCenter(ccId).getVersionNo(), MdmStatusSupport.ACTIVE, before, loadCostCenter(ccId));
    }

    private void applyCostCenterChange(Long ccId, Integer baseVersionNo, Map<String, Object> meta, SysWorkflowInstance instance) {
        MdmCostCenter before = loadCostCenter(ccId);
        ensureVersion(before, baseVersionNo, "成本中心");
        if (!MdmStatusSupport.isSubmitted(before.getStatus())) {
            throw new IllegalStateException("成本中心状态已变化，请重新发起审批");
        }
        MdmCostCenter afterCostCenter = objectMapper.convertValue(meta.get("afterCostCenter"), MdmCostCenter.class);
        MdmCostCenter updateEntity = new MdmCostCenter();
        BeanUtils.copyProperties(afterCostCenter, updateEntity);
        updateEntity.setCcId(ccId);
        updateEntity.setTenantId(before.getTenantId());
        updateEntity.setCcCode(before.getCcCode());
        updateEntity.setStatus(MdmStatusSupport.ACTIVE);
        updateEntity.setVersionNo(MdmValueSupport.resolveNextVersionNo(before.getVersionNo()));
        updateEntity.setUpdateBy(resolveOperator(instance));
        updateEntity.setCreateBy(null);
        updateEntity.setCreateTime(null);
        updateEntity.setDelFlag(null);
        boolean updated = costCenterMapper.update(updateEntity, new LambdaUpdateWrapper<MdmCostCenter>()
                .eq(MdmCostCenter::getCcId, ccId)
                .eq(MdmCostCenter::getDelFlag, DEL_FLAG_EXIST)
                .eq(MdmCostCenter::getStatus, MdmStatusSupport.SUBMITTED)
                .eq(baseVersionNo != null, MdmCostCenter::getVersionNo, baseVersionNo)) > 0;
        if (!updated) {
            throw new IllegalStateException("成本中心版本已变化，请重新发起审批");
        }
        auditTrailService.record(MdmDomainTypeSupport.COST_CENTER, ccId, MdmChangeTypeSupport.UPDATE, loadCostCenter(ccId).getVersionNo(), loadCostCenter(ccId).getStatus(), before, loadCostCenter(ccId));
    }

    private void disableCostCenter(Long ccId, Integer baseVersionNo, SysWorkflowInstance instance) {
        MdmCostCenter before = loadCostCenter(ccId);
        ensureVersion(before, baseVersionNo, "成本中心");
        if (!MdmStatusSupport.isSubmitted(before.getStatus())) {
            throw new IllegalStateException("成本中心状态已变化，请重新发起审批");
        }
        MdmCostCenter updateEntity = new MdmCostCenter();
        updateEntity.setCcId(ccId);
        updateEntity.setStatus(MdmStatusSupport.DISABLED);
        updateEntity.setVersionNo(MdmValueSupport.resolveNextVersionNo(before.getVersionNo()));
        updateEntity.setUpdateBy(resolveOperator(instance));
        boolean updated = costCenterMapper.update(updateEntity, new LambdaUpdateWrapper<MdmCostCenter>()
                .eq(MdmCostCenter::getCcId, ccId)
                .eq(MdmCostCenter::getDelFlag, DEL_FLAG_EXIST)
                .eq(MdmCostCenter::getStatus, MdmStatusSupport.SUBMITTED)
                .eq(baseVersionNo != null, MdmCostCenter::getVersionNo, baseVersionNo)) > 0;
        if (!updated) {
            throw new IllegalStateException("成本中心版本已变化，请重新发起审批");
        }
        auditTrailService.record(MdmDomainTypeSupport.COST_CENTER, ccId, MdmChangeTypeSupport.STATUS, loadCostCenter(ccId).getVersionNo(), MdmStatusSupport.DISABLED, before, loadCostCenter(ccId));
    }

    private void activateProject(Long projectId, Integer baseVersionNo, SysWorkflowInstance instance) {
        MdmProject before = loadProject(projectId);
        ensureVersion(before, baseVersionNo, "项目");
        boolean updated = projectMapper.update(new MdmProject(), new LambdaUpdateWrapper<MdmProject>()
                .eq(MdmProject::getProjectId, projectId)
                .eq(MdmProject::getDelFlag, DEL_FLAG_EXIST)
                .eq(MdmProject::getStatus, MdmStatusSupport.SUBMITTED)
                .eq(baseVersionNo != null, MdmProject::getVersionNo, baseVersionNo)
                .set(MdmProject::getStatus, MdmStatusSupport.ACTIVE)
                .set(MdmProject::getUpdateBy, resolveOperator(instance))
                .set(MdmProject::getUpdateTime, new Date())) > 0;
        if (!updated) {
            throw new IllegalStateException("项目状态已变化，无法完成审批回写");
        }
        auditTrailService.record(MdmDomainTypeSupport.PROJECT, projectId, MdmChangeTypeSupport.STATUS, loadProject(projectId).getVersionNo(), MdmStatusSupport.ACTIVE, before, loadProject(projectId));
    }

    private void applyProjectChange(Long projectId, Integer baseVersionNo, Map<String, Object> meta, SysWorkflowInstance instance) {
        MdmProject before = loadProject(projectId);
        ensureVersion(before, baseVersionNo, "项目");
        if (!MdmStatusSupport.isSubmitted(before.getStatus())) {
            throw new IllegalStateException("项目状态已变化，请重新发起审批");
        }
        MdmProject afterProject = objectMapper.convertValue(meta.get("afterProject"), MdmProject.class);
        MdmProject updateEntity = new MdmProject();
        BeanUtils.copyProperties(afterProject, updateEntity);
        updateEntity.setProjectId(projectId);
        updateEntity.setTenantId(before.getTenantId());
        updateEntity.setProjectCode(before.getProjectCode());
        updateEntity.setStatus(MdmStatusSupport.ACTIVE);
        updateEntity.setVersionNo(MdmValueSupport.resolveNextVersionNo(before.getVersionNo()));
        updateEntity.setUpdateBy(resolveOperator(instance));
        updateEntity.setCreateBy(null);
        updateEntity.setCreateTime(null);
        updateEntity.setDelFlag(null);
        boolean updated = projectMapper.update(updateEntity, new LambdaUpdateWrapper<MdmProject>()
                .eq(MdmProject::getProjectId, projectId)
                .eq(MdmProject::getDelFlag, DEL_FLAG_EXIST)
                .eq(MdmProject::getStatus, MdmStatusSupport.SUBMITTED)
                .eq(baseVersionNo != null, MdmProject::getVersionNo, baseVersionNo)) > 0;
        if (!updated) {
            throw new IllegalStateException("项目版本已变化，请重新发起审批");
        }
        auditTrailService.record(MdmDomainTypeSupport.PROJECT, projectId, MdmChangeTypeSupport.UPDATE, loadProject(projectId).getVersionNo(), loadProject(projectId).getStatus(), before, loadProject(projectId));
    }

    private void disableProject(Long projectId, Integer baseVersionNo, SysWorkflowInstance instance) {
        MdmProject before = loadProject(projectId);
        ensureVersion(before, baseVersionNo, "项目");
        if (!MdmStatusSupport.isSubmitted(before.getStatus())) {
            throw new IllegalStateException("项目状态已变化，请重新发起审批");
        }
        MdmProject updateEntity = new MdmProject();
        updateEntity.setProjectId(projectId);
        updateEntity.setStatus(MdmStatusSupport.DISABLED);
        updateEntity.setVersionNo(MdmValueSupport.resolveNextVersionNo(before.getVersionNo()));
        updateEntity.setUpdateBy(resolveOperator(instance));
        boolean updated = projectMapper.update(updateEntity, new LambdaUpdateWrapper<MdmProject>()
                .eq(MdmProject::getProjectId, projectId)
                .eq(MdmProject::getDelFlag, DEL_FLAG_EXIST)
                .eq(MdmProject::getStatus, MdmStatusSupport.SUBMITTED)
                .eq(baseVersionNo != null, MdmProject::getVersionNo, baseVersionNo)) > 0;
        if (!updated) {
            throw new IllegalStateException("项目版本已变化，请重新发起审批");
        }
        auditTrailService.record(MdmDomainTypeSupport.PROJECT, projectId, MdmChangeTypeSupport.STATUS, loadProject(projectId).getVersionNo(), MdmStatusSupport.DISABLED, before, loadProject(projectId));
    }

    private MdmOrg loadOrg(Long orgId) {
        return orgMapper.selectOne(new LambdaQueryWrapper<MdmOrg>().eq(MdmOrg::getOrgId, orgId).eq(MdmOrg::getDelFlag, DEL_FLAG_EXIST));
    }

    private MdmCostCenter loadCostCenter(Long ccId) {
        return costCenterMapper.selectOne(new LambdaQueryWrapper<MdmCostCenter>().eq(MdmCostCenter::getCcId, ccId).eq(MdmCostCenter::getDelFlag, DEL_FLAG_EXIST));
    }

    private MdmProject loadProject(Long projectId) {
        return projectMapper.selectOne(new LambdaQueryWrapper<MdmProject>().eq(MdmProject::getProjectId, projectId).eq(MdmProject::getDelFlag, DEL_FLAG_EXIST));
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> readMeta(SysWorkflowInstance instance, String metaKey) {
        if (instance == null || !StringUtils.hasText(instance.getFormData())) {
            return Map.of();
        }
        try {
            Map<String, Object> formData = objectMapper.readValue(instance.getFormData(), Map.class);
            Object meta = formData.get(metaKey);
            return meta instanceof Map ? (Map<String, Object>) meta : Map.of();
        } catch (Exception ex) {
            throw new IllegalStateException("解析维度审批元数据失败", ex);
        }
    }

    private void ensureVersion(Object entity, Integer baseVersionNo, String label) {
        if (entity == null) {
            throw new IllegalStateException(label + "不存在，无法完成审批回写");
        }
        Integer currentVersion = null;
        if (entity instanceof MdmOrg) {
            currentVersion = ((MdmOrg) entity).getVersionNo();
        } else if (entity instanceof MdmCostCenter) {
            currentVersion = ((MdmCostCenter) entity).getVersionNo();
        } else if (entity instanceof MdmProject) {
            currentVersion = ((MdmProject) entity).getVersionNo();
        }
        if (baseVersionNo != null && currentVersion != null && !baseVersionNo.equals(currentVersion)) {
            throw new IllegalStateException(label + "版本已变化，请重新发起审批");
        }
    }

    private String readString(Object value) {
        return value == null ? null : StringUtils.trimWhitespace(String.valueOf(value));
    }

    private Long readLong(Object value) {
        if (value == null) {
            return null;
        }
        try {
            return Long.valueOf(String.valueOf(value));
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private Integer readInteger(Object value) {
        if (value == null) {
            return null;
        }
        try {
            return Integer.valueOf(String.valueOf(value));
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private String resolveOperator(SysWorkflowInstance instance) {
        if (instance != null && StringUtils.hasText(instance.getLastActionUserName())) {
            return instance.getLastActionUserName().trim();
        }
        return "system";
    }
}


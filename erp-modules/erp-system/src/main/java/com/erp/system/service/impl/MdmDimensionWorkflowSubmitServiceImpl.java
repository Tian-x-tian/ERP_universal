package com.erp.system.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.erp.system.domain.MdmCostCenter;
import com.erp.system.domain.MdmCustomer;
import com.erp.system.domain.MdmEmployee;
import com.erp.system.domain.MdmOrg;
import com.erp.system.domain.MdmProject;
import com.erp.system.domain.SysUser;
import com.erp.system.domain.SysWorkflowInstance;
import com.erp.system.domain.vo.WorkflowStartBody;
import com.erp.system.mapper.MdmCostCenterMapper;
import com.erp.system.mapper.MdmCustomerMapper;
import com.erp.system.mapper.MdmEmployeeMapper;
import com.erp.system.mapper.MdmOrgMapper;
import com.erp.system.mapper.MdmProjectMapper;
import com.erp.system.mapper.SysWorkflowInstanceMapper;
import com.erp.system.security.service.SecurityUserResolver;
import com.erp.system.service.IMdmCostCenterService;
import com.erp.system.service.IMdmDimensionWorkflowSubmitService;
import com.erp.system.service.IMdmOrgService;
import com.erp.system.service.IMdmProjectService;
import com.erp.system.service.ISysUserService;
import com.erp.system.service.ISysWorkflowEngineService;
import com.erp.system.support.MdmStatusSupport;
import com.erp.system.support.MdmValueSupport;
import com.erp.system.support.MdmWorkflowActionSupport;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.Date;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 维度主数据审批提交流程服务实现。
 */
@Service
public class MdmDimensionWorkflowSubmitServiceImpl implements IMdmDimensionWorkflowSubmitService {
    private static final String DEL_FLAG_EXIST = "0";
    private static final String WORKFLOW_STATUS_RUNNING = "0";
    private static final Long ROOT_PARENT_ID = 0L;
    private static final String ROOT_ANCESTORS = "0";

    private static final String BUSINESS_TYPE_ORG = "MDM_ORG";
    private static final String BUSINESS_TYPE_COST_CENTER = "MDM_COST_CENTER";
    private static final String BUSINESS_TYPE_PROJECT = "MDM_PROJECT";
    private static final String BUSINESS_NO_PREFIX_ORG = "MDM:ORG:";
    private static final String BUSINESS_NO_PREFIX_COST_CENTER = "MDM:COST_CENTER:";
    private static final String BUSINESS_NO_PREFIX_PROJECT = "MDM:PROJECT:";
    private static final String META_KEY_ORG = "__mdmOrgMeta";
    private static final String META_KEY_COST_CENTER = "__mdmCostCenterMeta";
    private static final String META_KEY_PROJECT = "__mdmProjectMeta";

    private final IMdmOrgService orgService;
    private final IMdmCostCenterService costCenterService;
    private final IMdmProjectService projectService;
    private final ISysWorkflowEngineService workflowEngineService;
    private final SecurityUserResolver securityUserResolver;
    private final ISysUserService userService;
    private final SysWorkflowInstanceMapper workflowInstanceMapper;
    private final MdmOrgMapper orgMapper;
    private final MdmCostCenterMapper costCenterMapper;
    private final MdmProjectMapper projectMapper;
    private final MdmCustomerMapper customerMapper;
    private final MdmEmployeeMapper employeeMapper;
    private final ObjectMapper objectMapper;

    public MdmDimensionWorkflowSubmitServiceImpl(IMdmOrgService orgService,
            IMdmCostCenterService costCenterService,
            IMdmProjectService projectService,
            ISysWorkflowEngineService workflowEngineService,
            SecurityUserResolver securityUserResolver,
            ISysUserService userService,
            SysWorkflowInstanceMapper workflowInstanceMapper,
            MdmOrgMapper orgMapper,
            MdmCostCenterMapper costCenterMapper,
            MdmProjectMapper projectMapper,
            MdmCustomerMapper customerMapper,
            MdmEmployeeMapper employeeMapper) {
        this.orgService = orgService;
        this.costCenterService = costCenterService;
        this.projectService = projectService;
        this.workflowEngineService = workflowEngineService;
        this.securityUserResolver = securityUserResolver;
        this.userService = userService;
        this.workflowInstanceMapper = workflowInstanceMapper;
        this.orgMapper = orgMapper;
        this.costCenterMapper = costCenterMapper;
        this.projectMapper = projectMapper;
        this.customerMapper = customerMapper;
        this.employeeMapper = employeeMapper;
        this.objectMapper = new ObjectMapper();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean submitOrgDraftActivation(Long orgId, String processKey, String remark) {
        MdmOrg org = loadOrg(orgId);
        ensureDraft(org.getStatus(), "仅草稿组织允许提交生效审批");
        ensureNoRunningWorkflow(BUSINESS_TYPE_ORG, buildOrgBusinessNo(orgId), "该组织已有审批流程在处理中");
        if (!startWorkflow(buildOrgStartBody(processKey, remark, orgId, MdmWorkflowActionSupport.ACTIVATE, org.getVersionNo(), null, org))) {
            throw new IllegalStateException("组织审批流程发起失败");
        }
        if (!updateOrgStatus(orgId, org.getVersionNo(), MdmStatusSupport.SUBMITTED)) {
            throw new IllegalStateException("组织状态已变化，请刷新后重试");
        }
        return true;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean submitOrgChange(Long orgId, MdmOrg targetOrg, String processKey, String remark) {
        MdmOrg currentOrg = loadOrg(orgId);
        ensureActiveForChange(currentOrg.getStatus(), "组织审批中，暂不允许提交新的变更", "仅已生效组织允许提交变更审批");
        ensureNoRunningWorkflow(BUSINESS_TYPE_ORG, buildOrgBusinessNo(orgId), "该组织已有审批流程在处理中");
        MdmOrg afterOrg = normalizeTargetOrg(currentOrg, targetOrg);
        if (!startWorkflow(buildOrgStartBody(processKey, remark, orgId, MdmWorkflowActionSupport.UPDATE, currentOrg.getVersionNo(), currentOrg, afterOrg))) {
            throw new IllegalStateException("组织变更审批流程发起失败");
        }
        if (!updateOrgStatus(orgId, currentOrg.getVersionNo(), MdmStatusSupport.SUBMITTED)) {
            throw new IllegalStateException("组织状态已变化，请刷新后重试");
        }
        return true;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean submitOrgDisable(Long orgId, String processKey, String remark) {
        MdmOrg currentOrg = loadOrg(orgId);
        ensureActiveForDisable(currentOrg.getStatus(), "组织审批中，暂不允许提交停用", "仅已生效组织允许提交停用审批");
        ensureNoRunningWorkflow(BUSINESS_TYPE_ORG, buildOrgBusinessNo(orgId), "该组织已有审批流程在处理中");
        MdmOrg afterOrg = new MdmOrg();
        BeanUtils.copyProperties(currentOrg, afterOrg);
        afterOrg.setStatus(MdmStatusSupport.DISABLED);
        if (!startWorkflow(buildOrgStartBody(processKey, remark, orgId, MdmWorkflowActionSupport.DISABLE, currentOrg.getVersionNo(), currentOrg, afterOrg))) {
            throw new IllegalStateException("组织停用审批流程发起失败");
        }
        if (!updateOrgStatus(orgId, currentOrg.getVersionNo(), MdmStatusSupport.SUBMITTED)) {
            throw new IllegalStateException("组织状态已变化，请刷新后重试");
        }
        return true;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean submitCostCenterDraftActivation(Long ccId, String processKey, String remark) {
        MdmCostCenter costCenter = loadCostCenter(ccId);
        ensureDraft(costCenter.getStatus(), "仅草稿成本中心允许提交生效审批");
        ensureNoRunningWorkflow(BUSINESS_TYPE_COST_CENTER, buildCostCenterBusinessNo(ccId), "该成本中心已有审批流程在处理中");
        if (!startWorkflow(buildCostCenterStartBody(processKey, remark, ccId, MdmWorkflowActionSupport.ACTIVATE, costCenter.getVersionNo(), null, costCenter))) {
            throw new IllegalStateException("成本中心审批流程发起失败");
        }
        if (!updateCostCenterStatus(ccId, costCenter.getVersionNo(), MdmStatusSupport.SUBMITTED)) {
            throw new IllegalStateException("成本中心状态已变化，请刷新后重试");
        }
        return true;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean submitCostCenterChange(Long ccId, MdmCostCenter targetCostCenter, String processKey, String remark) {
        MdmCostCenter currentCostCenter = loadCostCenter(ccId);
        ensureActiveForChange(currentCostCenter.getStatus(), "成本中心审批中，暂不允许提交新的变更", "仅已生效成本中心允许提交变更审批");
        ensureNoRunningWorkflow(BUSINESS_TYPE_COST_CENTER, buildCostCenterBusinessNo(ccId), "该成本中心已有审批流程在处理中");
        MdmCostCenter afterCostCenter = normalizeTargetCostCenter(currentCostCenter, targetCostCenter);
        if (!startWorkflow(buildCostCenterStartBody(processKey, remark, ccId, MdmWorkflowActionSupport.UPDATE, currentCostCenter.getVersionNo(), currentCostCenter, afterCostCenter))) {
            throw new IllegalStateException("成本中心变更审批流程发起失败");
        }
        if (!updateCostCenterStatus(ccId, currentCostCenter.getVersionNo(), MdmStatusSupport.SUBMITTED)) {
            throw new IllegalStateException("成本中心状态已变化，请刷新后重试");
        }
        return true;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean submitCostCenterDisable(Long ccId, String processKey, String remark) {
        MdmCostCenter currentCostCenter = loadCostCenter(ccId);
        ensureActiveForDisable(currentCostCenter.getStatus(), "成本中心审批中，暂不允许提交停用", "仅已生效成本中心允许提交停用审批");
        ensureNoRunningWorkflow(BUSINESS_TYPE_COST_CENTER, buildCostCenterBusinessNo(ccId), "该成本中心已有审批流程在处理中");
        MdmCostCenter afterCostCenter = new MdmCostCenter();
        BeanUtils.copyProperties(currentCostCenter, afterCostCenter);
        afterCostCenter.setStatus(MdmStatusSupport.DISABLED);
        if (!startWorkflow(buildCostCenterStartBody(processKey, remark, ccId, MdmWorkflowActionSupport.DISABLE, currentCostCenter.getVersionNo(), currentCostCenter, afterCostCenter))) {
            throw new IllegalStateException("成本中心停用审批流程发起失败");
        }
        if (!updateCostCenterStatus(ccId, currentCostCenter.getVersionNo(), MdmStatusSupport.SUBMITTED)) {
            throw new IllegalStateException("成本中心状态已变化，请刷新后重试");
        }
        return true;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean submitProjectDraftActivation(Long projectId, String processKey, String remark) {
        MdmProject project = loadProject(projectId);
        ensureDraft(project.getStatus(), "仅草稿项目允许提交生效审批");
        ensureNoRunningWorkflow(BUSINESS_TYPE_PROJECT, buildProjectBusinessNo(projectId), "该项目已有审批流程在处理中");
        if (!startWorkflow(buildProjectStartBody(processKey, remark, projectId, MdmWorkflowActionSupport.ACTIVATE, project.getVersionNo(), null, project))) {
            throw new IllegalStateException("项目审批流程发起失败");
        }
        if (!updateProjectStatus(projectId, project.getVersionNo(), MdmStatusSupport.SUBMITTED)) {
            throw new IllegalStateException("项目状态已变化，请刷新后重试");
        }
        return true;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean submitProjectChange(Long projectId, MdmProject targetProject, String processKey, String remark) {
        MdmProject currentProject = loadProject(projectId);
        ensureActiveForChange(currentProject.getStatus(), "项目审批中，暂不允许提交新的变更", "仅已生效项目允许提交变更审批");
        ensureNoRunningWorkflow(BUSINESS_TYPE_PROJECT, buildProjectBusinessNo(projectId), "该项目已有审批流程在处理中");
        MdmProject afterProject = normalizeTargetProject(currentProject, targetProject);
        if (!startWorkflow(buildProjectStartBody(processKey, remark, projectId, MdmWorkflowActionSupport.UPDATE, currentProject.getVersionNo(), currentProject, afterProject))) {
            throw new IllegalStateException("项目变更审批流程发起失败");
        }
        if (!updateProjectStatus(projectId, currentProject.getVersionNo(), MdmStatusSupport.SUBMITTED)) {
            throw new IllegalStateException("项目状态已变化，请刷新后重试");
        }
        return true;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean submitProjectDisable(Long projectId, String processKey, String remark) {
        MdmProject currentProject = loadProject(projectId);
        ensureActiveForDisable(currentProject.getStatus(), "项目审批中，暂不允许提交停用", "仅已生效项目允许提交停用审批");
        ensureNoRunningWorkflow(BUSINESS_TYPE_PROJECT, buildProjectBusinessNo(projectId), "该项目已有审批流程在处理中");
        MdmProject afterProject = new MdmProject();
        BeanUtils.copyProperties(currentProject, afterProject);
        afterProject.setStatus(MdmStatusSupport.DISABLED);
        if (!startWorkflow(buildProjectStartBody(processKey, remark, projectId, MdmWorkflowActionSupport.DISABLE, currentProject.getVersionNo(), currentProject, afterProject))) {
            throw new IllegalStateException("项目停用审批流程发起失败");
        }
        if (!updateProjectStatus(projectId, currentProject.getVersionNo(), MdmStatusSupport.SUBMITTED)) {
            throw new IllegalStateException("项目状态已变化，请刷新后重试");
        }
        return true;
    }

    private MdmOrg loadOrg(Long orgId) {
        if (orgId == null) {
            throw new IllegalArgumentException("组织ID不能为空");
        }
        MdmOrg org = orgService.getOne(new LambdaQueryWrapper<MdmOrg>()
                .eq(MdmOrg::getOrgId, orgId)
                .eq(MdmOrg::getDelFlag, DEL_FLAG_EXIST));
        if (org == null) {
            throw new IllegalArgumentException("组织不存在");
        }
        return org;
    }

    private MdmCostCenter loadCostCenter(Long ccId) {
        if (ccId == null) {
            throw new IllegalArgumentException("成本中心ID不能为空");
        }
        MdmCostCenter costCenter = costCenterService.getOne(new LambdaQueryWrapper<MdmCostCenter>()
                .eq(MdmCostCenter::getCcId, ccId)
                .eq(MdmCostCenter::getDelFlag, DEL_FLAG_EXIST));
        if (costCenter == null) {
            throw new IllegalArgumentException("成本中心不存在");
        }
        return costCenter;
    }

    private MdmProject loadProject(Long projectId) {
        if (projectId == null) {
            throw new IllegalArgumentException("项目ID不能为空");
        }
        MdmProject project = projectService.getOne(new LambdaQueryWrapper<MdmProject>()
                .eq(MdmProject::getProjectId, projectId)
                .eq(MdmProject::getDelFlag, DEL_FLAG_EXIST));
        if (project == null) {
            throw new IllegalArgumentException("项目不存在");
        }
        return project;
    }

    private void ensureDraft(String status, String message) {
        if (!MdmStatusSupport.isDraft(status)) {
            throw new IllegalStateException(message);
        }
    }

    private void ensureActiveForChange(String status, String submittedMessage, String activeMessage) {
        if (MdmStatusSupport.isSubmitted(status)) {
            throw new IllegalStateException(submittedMessage);
        }
        if (!MdmStatusSupport.isActive(status)) {
            throw new IllegalStateException(activeMessage);
        }
    }

    private void ensureActiveForDisable(String status, String submittedMessage, String activeMessage) {
        ensureActiveForChange(status, submittedMessage, activeMessage);
    }

    private void ensureNoRunningWorkflow(String businessType, String businessNo, String message) {
        Long count = workflowInstanceMapper.selectCount(new LambdaQueryWrapper<SysWorkflowInstance>()
                .eq(SysWorkflowInstance::getBusinessType, businessType)
                .eq(SysWorkflowInstance::getBusinessNo, businessNo)
                .eq(SysWorkflowInstance::getStatus, WORKFLOW_STATUS_RUNNING));
        if (count != null && count > 0) {
            throw new IllegalStateException(message);
        }
    }

    private boolean startWorkflow(WorkflowStartBody startBody) {
        Long userId = securityUserResolver.getCurrentUserId();
        String userName = securityUserResolver.getCurrentUsername();
        if (userId == null || !StringUtils.hasText(userName)) {
            throw new IllegalStateException("当前登录用户无效，无法提交审批");
        }
        SysUser user = userService.selectUserByUserName(userName);
        String nickName = user == null ? userName : user.getNickName();
        return workflowEngineService.startProcess(startBody, userId, userName, nickName);
    }

    private boolean updateOrgStatus(Long orgId, Integer currentVersion, String targetStatus) {
        MdmOrg updateEntity = new MdmOrg();
        updateEntity.setOrgId(orgId);
        updateEntity.setStatus(targetStatus);
        updateEntity.setUpdateBy(resolveOperator());
        updateEntity.setUpdateTime(new Date());
        return orgService.update(updateEntity, new LambdaUpdateWrapper<MdmOrg>()
                .eq(MdmOrg::getOrgId, orgId)
                .eq(MdmOrg::getDelFlag, DEL_FLAG_EXIST)
                .eq(currentVersion != null, MdmOrg::getVersionNo, currentVersion));
    }

    private boolean updateCostCenterStatus(Long ccId, Integer currentVersion, String targetStatus) {
        MdmCostCenter updateEntity = new MdmCostCenter();
        updateEntity.setCcId(ccId);
        updateEntity.setStatus(targetStatus);
        updateEntity.setUpdateBy(resolveOperator());
        updateEntity.setUpdateTime(new Date());
        return costCenterService.update(updateEntity, new LambdaUpdateWrapper<MdmCostCenter>()
                .eq(MdmCostCenter::getCcId, ccId)
                .eq(MdmCostCenter::getDelFlag, DEL_FLAG_EXIST)
                .eq(currentVersion != null, MdmCostCenter::getVersionNo, currentVersion));
    }

    private boolean updateProjectStatus(Long projectId, Integer currentVersion, String targetStatus) {
        MdmProject updateEntity = new MdmProject();
        updateEntity.setProjectId(projectId);
        updateEntity.setStatus(targetStatus);
        updateEntity.setUpdateBy(resolveOperator());
        updateEntity.setUpdateTime(new Date());
        return projectService.update(updateEntity, new LambdaUpdateWrapper<MdmProject>()
                .eq(MdmProject::getProjectId, projectId)
                .eq(MdmProject::getDelFlag, DEL_FLAG_EXIST)
                .eq(currentVersion != null, MdmProject::getVersionNo, currentVersion));
    }

    private WorkflowStartBody buildOrgStartBody(String processKey, String remark, Long orgId, String action, Integer baseVersionNo, MdmOrg beforeOrg, MdmOrg afterOrg) {
        Map<String, Object> meta = new LinkedHashMap<>();
        meta.put("action", action);
        meta.put("orgId", orgId);
        meta.put("baseVersionNo", baseVersionNo);
        meta.put("beforeOrg", beforeOrg);
        meta.put("afterOrg", afterOrg);
        return buildStartBody(processKey, remark, buildOrgBusinessNo(orgId), BUSINESS_TYPE_ORG, META_KEY_ORG, meta, afterOrg);
    }

    private WorkflowStartBody buildCostCenterStartBody(String processKey, String remark, Long ccId, String action, Integer baseVersionNo, MdmCostCenter beforeCostCenter, MdmCostCenter afterCostCenter) {
        Map<String, Object> meta = new LinkedHashMap<>();
        meta.put("action", action);
        meta.put("ccId", ccId);
        meta.put("baseVersionNo", baseVersionNo);
        meta.put("beforeCostCenter", beforeCostCenter);
        meta.put("afterCostCenter", afterCostCenter);
        return buildStartBody(processKey, remark, buildCostCenterBusinessNo(ccId), BUSINESS_TYPE_COST_CENTER, META_KEY_COST_CENTER, meta, afterCostCenter);
    }

    private WorkflowStartBody buildProjectStartBody(String processKey, String remark, Long projectId, String action, Integer baseVersionNo, MdmProject beforeProject, MdmProject afterProject) {
        Map<String, Object> meta = new LinkedHashMap<>();
        meta.put("action", action);
        meta.put("projectId", projectId);
        meta.put("baseVersionNo", baseVersionNo);
        meta.put("beforeProject", beforeProject);
        meta.put("afterProject", afterProject);
        return buildStartBody(processKey, remark, buildProjectBusinessNo(projectId), BUSINESS_TYPE_PROJECT, META_KEY_PROJECT, meta, afterProject);
    }

    private WorkflowStartBody buildStartBody(String processKey,
            String remark,
            String businessNo,
            String businessType,
            String metaKey,
            Map<String, Object> meta,
            Object afterValue) {
        if (!StringUtils.hasText(processKey)) {
            throw new IllegalArgumentException("流程标识不能为空");
        }
        Map<String, Object> formData = new LinkedHashMap<>();
        if (afterValue != null) {
            formData.putAll(objectMapper.convertValue(afterValue, new TypeReference<Map<String, Object>>() { }));
        }
        formData.put(metaKey, meta);
        WorkflowStartBody startBody = new WorkflowStartBody();
        startBody.setProcessKey(processKey.trim());
        startBody.setBusinessNo(businessNo);
        startBody.setBusinessType(businessType);
        startBody.setRemark(MdmValueSupport.trimToNull(remark));
        startBody.setFormData(writeJson(formData));
        return startBody;
    }

    private MdmOrg normalizeTargetOrg(MdmOrg currentOrg, MdmOrg targetOrg) {
        if (currentOrg == null || targetOrg == null) {
            throw new IllegalArgumentException("组织变更数据不能为空");
        }
        MdmOrg normalized = new MdmOrg();
        BeanUtils.copyProperties(currentOrg, normalized);
        normalized.setOrgId(currentOrg.getOrgId());
        normalized.setTenantId(currentOrg.getTenantId());
        normalized.setOrgCode(currentOrg.getOrgCode());
        normalized.setOrgName(trimRequired(targetOrg.getOrgName(), "组织名称不能为空"));
        normalized.setOrgType(MdmValueSupport.trimToNull(targetOrg.getOrgType()));
        Long parentId = normalizeParentId(targetOrg.getParentId());
        if (parentId.equals(currentOrg.getOrgId()) || !isOrgParentValid(parentId) || hasOrgCycle(parentId, currentOrg.getOrgId())) {
            throw new IllegalArgumentException("组织父子层级无效");
        }
        normalized.setParentId(parentId);
        normalized.setAncestors(buildOrgAncestors(parentId));
        normalized.setRemark(MdmValueSupport.trimToNull(targetOrg.getRemark()));
        normalized.setStatus(currentOrg.getStatus());
        normalized.setVersionNo(currentOrg.getVersionNo());
        return normalized;
    }

    private MdmCostCenter normalizeTargetCostCenter(MdmCostCenter currentCostCenter, MdmCostCenter targetCostCenter) {
        if (currentCostCenter == null || targetCostCenter == null) {
            throw new IllegalArgumentException("成本中心变更数据不能为空");
        }
        MdmCostCenter normalized = new MdmCostCenter();
        BeanUtils.copyProperties(currentCostCenter, normalized);
        normalized.setCcId(currentCostCenter.getCcId());
        normalized.setTenantId(currentCostCenter.getTenantId());
        normalized.setCcCode(currentCostCenter.getCcCode());
        normalized.setCcName(trimRequired(targetCostCenter.getCcName(), "成本中心名称不能为空"));
        Long parentId = normalizeParentId(targetCostCenter.getParentId());
        if (parentId.equals(currentCostCenter.getCcId()) || !isCostCenterParentValid(parentId) || hasCostCenterCycle(parentId, currentCostCenter.getCcId())) {
            throw new IllegalArgumentException("成本中心父子层级无效");
        }
        normalized.setParentId(parentId);
        normalized.setOrgId(targetCostCenter.getOrgId());
        if (!isOrgExists(normalized.getOrgId())) {
            throw new IllegalArgumentException("成本中心组织无效");
        }
        normalized.setRemark(MdmValueSupport.trimToNull(targetCostCenter.getRemark()));
        normalized.setStatus(currentCostCenter.getStatus());
        normalized.setVersionNo(currentCostCenter.getVersionNo());
        return normalized;
    }

    private MdmProject normalizeTargetProject(MdmProject currentProject, MdmProject targetProject) {
        if (currentProject == null || targetProject == null) {
            throw new IllegalArgumentException("项目变更数据不能为空");
        }
        Date startDate = targetProject.getStartDate();
        Date endDate = targetProject.getEndDate();
        if (!isDateRangeValid(startDate, endDate)) {
            throw new IllegalArgumentException("项目日期区间无效");
        }
        MdmProject normalized = new MdmProject();
        BeanUtils.copyProperties(currentProject, normalized);
        normalized.setProjectId(currentProject.getProjectId());
        normalized.setTenantId(currentProject.getTenantId());
        normalized.setProjectCode(currentProject.getProjectCode());
        normalized.setProjectName(trimRequired(targetProject.getProjectName(), "项目名称不能为空"));
        normalized.setCustomerId(targetProject.getCustomerId());
        normalized.setOrgId(targetProject.getOrgId());
        normalized.setManagerEmpId(targetProject.getManagerEmpId());
        if (!isProjectReferenceValid(normalized)) {
            throw new IllegalArgumentException("项目客户、组织或负责人无效");
        }
        normalized.setStartDate(startDate);
        normalized.setEndDate(endDate);
        normalized.setRemark(MdmValueSupport.trimToNull(targetProject.getRemark()));
        normalized.setStatus(currentProject.getStatus());
        normalized.setVersionNo(currentProject.getVersionNo());
        return normalized;
    }

    private Long normalizeParentId(Long parentId) {
        return parentId == null || parentId < 1 ? ROOT_PARENT_ID : parentId;
    }

    private boolean isOrgParentValid(Long parentId) {
        if (parentId == null || parentId.equals(ROOT_PARENT_ID)) {
            return true;
        }
        return orgMapper.selectCount(new LambdaQueryWrapper<MdmOrg>()
                .eq(MdmOrg::getOrgId, parentId)
                .eq(MdmOrg::getDelFlag, DEL_FLAG_EXIST)) > 0;
    }

    private boolean isCostCenterParentValid(Long parentId) {
        if (parentId == null || parentId.equals(ROOT_PARENT_ID)) {
            return true;
        }
        return costCenterMapper.selectCount(new LambdaQueryWrapper<MdmCostCenter>()
                .eq(MdmCostCenter::getCcId, parentId)
                .eq(MdmCostCenter::getDelFlag, DEL_FLAG_EXIST)) > 0;
    }

    private boolean hasOrgCycle(Long parentId, Long orgId) {
        if (parentId == null || parentId.equals(ROOT_PARENT_ID) || orgId == null) {
            return false;
        }
        Long current = parentId;
        int guard = 0;
        while (current != null && !current.equals(ROOT_PARENT_ID) && guard < 100) {
            if (current.equals(orgId)) {
                return true;
            }
            MdmOrg parent = orgMapper.selectById(current);
            if (parent == null) {
                return false;
            }
            current = normalizeParentId(parent.getParentId());
            guard++;
        }
        return false;
    }

    private boolean hasCostCenterCycle(Long parentId, Long ccId) {
        if (parentId == null || parentId.equals(ROOT_PARENT_ID) || ccId == null) {
            return false;
        }
        Long current = parentId;
        int guard = 0;
        while (current != null && !current.equals(ROOT_PARENT_ID) && guard < 100) {
            if (current.equals(ccId)) {
                return true;
            }
            MdmCostCenter parent = costCenterMapper.selectById(current);
            if (parent == null) {
                return false;
            }
            current = normalizeParentId(parent.getParentId());
            guard++;
        }
        return false;
    }

    private String buildOrgAncestors(Long parentId) {
        if (parentId == null || parentId.equals(ROOT_PARENT_ID)) {
            return ROOT_ANCESTORS;
        }
        MdmOrg parent = orgMapper.selectById(parentId);
        if (parent == null || !StringUtils.hasText(parent.getAncestors())) {
            return ROOT_ANCESTORS + "," + parentId;
        }
        return parent.getAncestors() + "," + parentId;
    }

    private boolean isOrgExists(Long orgId) {
        if (orgId == null || orgId < 1) {
            return false;
        }
        MdmOrg org = orgMapper.selectById(orgId);
        return org != null && DEL_FLAG_EXIST.equals(org.getDelFlag());
    }

    private boolean isProjectReferenceValid(MdmProject project) {
        return isCustomerValid(project.getCustomerId()) && isOrgExists(project.getOrgId()) && isEmployeeValid(project.getManagerEmpId());
    }

    private boolean isCustomerValid(Long customerId) {
        if (customerId == null || customerId < 1) {
            return true;
        }
        MdmCustomer customer = customerMapper.selectById(customerId);
        return customer != null && DEL_FLAG_EXIST.equals(customer.getDelFlag());
    }

    private boolean isEmployeeValid(Long employeeId) {
        if (employeeId == null || employeeId < 1) {
            return true;
        }
        MdmEmployee employee = employeeMapper.selectById(employeeId);
        return employee != null && DEL_FLAG_EXIST.equals(employee.getDelFlag());
    }

    private boolean isDateRangeValid(Date startDate, Date endDate) {
        if (startDate == null || endDate == null) {
            return true;
        }
        return !startDate.after(endDate);
    }

    private String buildOrgBusinessNo(Long orgId) {
        return BUSINESS_NO_PREFIX_ORG + orgId;
    }

    private String buildCostCenterBusinessNo(Long ccId) {
        return BUSINESS_NO_PREFIX_COST_CENTER + ccId;
    }

    private String buildProjectBusinessNo(Long projectId) {
        return BUSINESS_NO_PREFIX_PROJECT + projectId;
    }

    private String writeJson(Object data) {
        try {
            return objectMapper.writeValueAsString(data);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("审批数据序列化失败", ex);
        }
    }

    private String trimRequired(String value, String message) {
        if (!StringUtils.hasText(value)) {
            throw new IllegalArgumentException(message);
        }
        return value.trim();
    }

    private String resolveOperator() {
        String userName = securityUserResolver.getCurrentUsername();
        return StringUtils.hasText(userName) ? userName.trim() : "system";
    }
}

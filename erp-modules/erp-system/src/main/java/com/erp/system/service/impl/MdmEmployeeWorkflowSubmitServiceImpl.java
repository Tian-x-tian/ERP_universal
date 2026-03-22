package com.erp.system.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.erp.system.domain.MdmCostCenter;
import com.erp.system.domain.MdmEmployee;
import com.erp.system.domain.MdmOrg;
import com.erp.system.domain.SysDept;
import com.erp.system.domain.SysUser;
import com.erp.workflow.contract.domain.SysWorkflowInstance;
import com.erp.workflow.contract.domain.vo.WorkflowStartBody;
import com.erp.system.mapper.MdmCostCenterMapper;
import com.erp.system.mapper.MdmOrgMapper;
import com.erp.system.security.service.SecurityUserResolver;
import com.erp.system.service.IMdmEmployeeService;
import com.erp.system.service.IMdmEmployeeWorkflowSubmitService;
import com.erp.system.service.ISysDeptService;
import com.erp.system.service.ISysUserService;
import com.erp.system.service.ISysWorkflowEngineService;
import com.erp.system.service.IWorkflowBindingResolver;
import com.erp.system.support.MdmDomainTypeSupport;
import com.erp.system.support.MdmEmployeeStatusSupport;
import com.erp.system.support.MdmOptimisticLockSupport;
import com.erp.system.support.MdmValueSupport;
import com.erp.system.support.MdmWorkflowActionSupport;
import com.erp.workflow.contract.support.WorkflowBindingActionSupport;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.Date;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 员工主数据审批提交流程服务实现。
 */
@Service
public class MdmEmployeeWorkflowSubmitServiceImpl implements IMdmEmployeeWorkflowSubmitService {
    private static final String DEL_FLAG_EXIST = "0";
    private static final String WORKFLOW_STATUS_RUNNING = "0";
    private static final String BUSINESS_TYPE = "MDM_EMPLOYEE";
    private static final String BUSINESS_NO_PREFIX = "MDM:EMPLOYEE:";
    private static final String META_KEY = "__mdmEmployeeMeta";

    private final IMdmEmployeeService employeeService;
    private final ISysWorkflowEngineService workflowEngineService;
    private final IWorkflowBindingResolver workflowBindingResolver;
    private final SecurityUserResolver securityUserResolver;
    private final ISysUserService userService;
    private final MdmOrgMapper orgMapper;
    private final MdmCostCenterMapper costCenterMapper;
    private final ISysDeptService deptService;
    private final ObjectMapper objectMapper;

    public MdmEmployeeWorkflowSubmitServiceImpl(IMdmEmployeeService employeeService,
            ISysWorkflowEngineService workflowEngineService,
            IWorkflowBindingResolver workflowBindingResolver,
            SecurityUserResolver securityUserResolver,
            ISysUserService userService,
            MdmOrgMapper orgMapper,
            MdmCostCenterMapper costCenterMapper,
            ISysDeptService deptService) {
        this.employeeService = employeeService;
        this.workflowEngineService = workflowEngineService;
        this.workflowBindingResolver = workflowBindingResolver;
        this.securityUserResolver = securityUserResolver;
        this.userService = userService;
        this.orgMapper = orgMapper;
        this.costCenterMapper = costCenterMapper;
        this.deptService = deptService;
        this.objectMapper = new ObjectMapper();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean submitDraftActivation(Long employeeId, Integer versionNo, String processKey, String remark) {
        MdmEmployee employee = loadEditableEmployee(employeeId);
        MdmOptimisticLockSupport.requireVersion(versionNo, employee.getVersionNo(), "员工");
        if (!MdmEmployeeStatusSupport.isDraft(employee.getStatus())) {
            throw new IllegalStateException("仅草稿员工允许提交生效审批");
        }
        if (hasRunningWorkflow(employeeId)) {
            throw new IllegalStateException("该员工已有审批流程在处理中");
        }
        String resolvedProcessKey = workflowBindingResolver.resolveProcessKey(
                MdmDomainTypeSupport.EMPLOYEE,
                WorkflowBindingActionSupport.ONBOARD,
                processKey);
        WorkflowStartBody startBody = buildStartBody(
                resolvedProcessKey,
                remark,
                employeeId,
                MdmWorkflowActionSupport.ACTIVATE,
                employee.getVersionNo(),
                null,
                employee,
                null,
                null);
        if (!startWorkflow(startBody)) {
            throw new IllegalStateException("员工审批流程发起失败");
        }
        if (!updateEmployeeStatus(employeeId, employee.getVersionNo(), MdmEmployeeStatusSupport.SUBMITTED)) {
            abortWorkflow(startBody, "员工状态更新失败，自动中止流程");
            throw new IllegalStateException("员工状态已变化，请刷新后重试");
        }
        return true;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean submitChange(Long employeeId,
            Integer versionNo,
            MdmEmployee targetEmployee,
            String processKey,
            String remark,
            Long changeRecordId,
            String archivePayloadJson) {
        MdmEmployee currentEmployee = loadEditableEmployee(employeeId);
        MdmOptimisticLockSupport.requireVersion(versionNo, currentEmployee.getVersionNo(), "员工");
        if (MdmEmployeeStatusSupport.isSubmitted(currentEmployee.getStatus())) {
            throw new IllegalStateException("员工审批中，暂不允许提交新的变更");
        }
        if (!MdmEmployeeStatusSupport.isActive(currentEmployee.getStatus())) {
            throw new IllegalStateException("仅已生效员工允许提交变更审批");
        }
        if (hasRunningWorkflow(employeeId)) {
            throw new IllegalStateException("该员工已有审批流程在处理中");
        }
        MdmEmployee afterEmployee = normalizeTargetEmployee(currentEmployee, targetEmployee);
        String resolvedProcessKey = workflowBindingResolver.resolveProcessKey(
                MdmDomainTypeSupport.EMPLOYEE,
                WorkflowBindingActionSupport.CHANGE,
                processKey);
        WorkflowStartBody startBody = buildStartBody(
                resolvedProcessKey,
                remark,
                employeeId,
                MdmWorkflowActionSupport.UPDATE,
                currentEmployee.getVersionNo(),
                currentEmployee,
                afterEmployee,
                changeRecordId,
                archivePayloadJson);
        if (!startWorkflow(startBody)) {
            throw new IllegalStateException("员工变更审批流程发起失败");
        }
        if (!updateEmployeeStatus(employeeId, currentEmployee.getVersionNo(), MdmEmployeeStatusSupport.SUBMITTED)) {
            abortWorkflow(startBody, "员工状态更新失败，自动中止流程");
            throw new IllegalStateException("员工状态已变化，请刷新后重试");
        }
        return true;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean submitLeave(Long employeeId, Integer versionNo, String processKey, String remark) {
        MdmEmployee currentEmployee = loadEditableEmployee(employeeId);
        MdmOptimisticLockSupport.requireVersion(versionNo, currentEmployee.getVersionNo(), "员工");
        if (MdmEmployeeStatusSupport.isSubmitted(currentEmployee.getStatus())) {
            throw new IllegalStateException("员工审批中，暂不允许提交离职");
        }
        if (!MdmEmployeeStatusSupport.isActive(currentEmployee.getStatus())) {
            throw new IllegalStateException("仅在职员工允许提交离职审批");
        }
        if (hasRunningWorkflow(employeeId)) {
            throw new IllegalStateException("该员工已有审批流程在处理中");
        }
        MdmEmployee afterEmployee = new MdmEmployee();
        BeanUtils.copyProperties(currentEmployee, afterEmployee);
        afterEmployee.setStatus(MdmEmployeeStatusSupport.LEAVE);
        String resolvedProcessKey = workflowBindingResolver.resolveProcessKey(
                MdmDomainTypeSupport.EMPLOYEE,
                WorkflowBindingActionSupport.LEAVE,
                processKey);
        WorkflowStartBody startBody = buildStartBody(
                resolvedProcessKey,
                remark,
                employeeId,
                MdmWorkflowActionSupport.DISABLE,
                currentEmployee.getVersionNo(),
                currentEmployee,
                afterEmployee,
                null,
                null);
        if (!startWorkflow(startBody)) {
            throw new IllegalStateException("员工离职审批流程发起失败");
        }
        if (!updateEmployeeStatus(employeeId, currentEmployee.getVersionNo(), MdmEmployeeStatusSupport.SUBMITTED)) {
            abortWorkflow(startBody, "员工状态更新失败，自动中止流程");
            throw new IllegalStateException("员工状态已变化，请刷新后重试");
        }
        return true;
    }

    private MdmEmployee loadEditableEmployee(Long employeeId) {
        if (employeeId == null) {
            throw new IllegalArgumentException("员工ID不能为空");
        }
        MdmEmployee employee = employeeService.getOne(new LambdaQueryWrapper<MdmEmployee>()
                .eq(MdmEmployee::getEmployeeId, employeeId)
                .eq(MdmEmployee::getDelFlag, DEL_FLAG_EXIST));
        if (employee == null) {
            throw new IllegalArgumentException("员工不存在");
        }
        return employee;
    }

    private boolean hasRunningWorkflow(Long employeeId) {
        return workflowEngineService.hasRunningInstance(BUSINESS_TYPE, buildBusinessNo(employeeId));
    }

    private WorkflowStartBody buildStartBody(String processKey,
            String remark,
            Long employeeId,
            String action,
            Integer baseVersionNo,
            MdmEmployee beforeEmployee,
            MdmEmployee afterEmployee,
            Long changeRecordId,
            String archivePayloadJson) {
        if (!StringUtils.hasText(processKey)) {
            throw new IllegalArgumentException("流程标识不能为空");
        }
        Map<String, Object> formData = new LinkedHashMap<>();
        if (afterEmployee != null) {
            formData.putAll(objectMapper.convertValue(afterEmployee, new TypeReference<Map<String, Object>>() { }));
        }
        Map<String, Object> meta = new LinkedHashMap<>();
        meta.put("action", action);
        meta.put("employeeId", employeeId);
        meta.put("baseVersionNo", baseVersionNo);
        meta.put("beforeEmployee", beforeEmployee);
        meta.put("afterEmployee", afterEmployee);
        meta.put("changeRecordId", changeRecordId);
        meta.put("archivePayloadJson", MdmValueSupport.trimToNull(archivePayloadJson));
        formData.put(META_KEY, meta);

        WorkflowStartBody startBody = new WorkflowStartBody();
        startBody.setProcessKey(processKey.trim());
        startBody.setRequestedProcessKey(processKey.trim());
        startBody.setOwnerService("system");
        startBody.setBusinessNo(buildBusinessNo(employeeId));
        startBody.setBusinessType(BUSINESS_TYPE);
        startBody.setDomainType(BUSINESS_TYPE);
        startBody.setActionCode(action);
        startBody.setIdempotencyKey(buildBusinessNo(employeeId));
        startBody.setOperator(resolveOperator());
        startBody.setRemark(MdmValueSupport.trimToNull(remark));
        startBody.setFormData(writeJson(formData));
        return startBody;
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

    /**
     * 在本地状态回写失败时中止已发起的流程实例。
     *
     * @param startBody 流程启动参数
     * @param remark 中止原因
     */
    private void abortWorkflow(WorkflowStartBody startBody, String remark) {
        if (startBody == null) {
            return;
        }
        workflowEngineService.abortProcess(startBody.getBusinessType(), startBody.getBusinessNo(), remark);
    }

    private boolean updateEmployeeStatus(Long employeeId, Integer currentVersion, String targetStatus) {
        MdmEmployee updateEntity = new MdmEmployee();
        updateEntity.setEmployeeId(employeeId);
        updateEntity.setStatus(targetStatus);
        updateEntity.setUpdateBy(resolveOperator());
        updateEntity.setUpdateTime(new Date());
        return employeeService.update(updateEntity, new LambdaUpdateWrapper<MdmEmployee>()
                .eq(MdmEmployee::getEmployeeId, employeeId)
                .eq(MdmEmployee::getDelFlag, DEL_FLAG_EXIST)
                .eq(currentVersion != null, MdmEmployee::getVersionNo, currentVersion));
    }

    private MdmEmployee normalizeTargetEmployee(MdmEmployee currentEmployee, MdmEmployee targetEmployee) {
        if (currentEmployee == null || targetEmployee == null) {
            throw new IllegalArgumentException("员工变更数据不能为空");
        }
        MdmEmployee normalized = new MdmEmployee();
        BeanUtils.copyProperties(currentEmployee, normalized);
        normalized.setEmployeeId(currentEmployee.getEmployeeId());
        normalized.setTenantId(currentEmployee.getTenantId());
        normalized.setEmpCode(currentEmployee.getEmpCode());
        normalized.setEmpName(trimRequired(targetEmployee.getEmpName(), "员工姓名不能为空"));
        normalized.setMobile(MdmValueSupport.trimToNull(targetEmployee.getMobile()));
        normalized.setEmail(MdmValueSupport.trimToNull(targetEmployee.getEmail()));
        normalized.setOrgId(targetEmployee.getOrgId());
        normalized.setDeptId(targetEmployee.getDeptId());
        normalized.setPosition(MdmValueSupport.trimToNull(targetEmployee.getPosition()));
        normalized.setUserId(targetEmployee.getUserId());
        normalized.setCostCenterId(targetEmployee.getCostCenterId());
        if (!isOrgValid(normalized.getOrgId())
                || !isCostCenterValid(normalized.getCostCenterId())
                || !isDeptValid(normalized.getDeptId())
                || !isUserValid(normalized.getUserId())) {
            throw new IllegalArgumentException("员工组织、部门、账号或成本中心无效");
        }
        normalized.setRemark(MdmValueSupport.trimToNull(targetEmployee.getRemark()));
        normalized.setStatus(currentEmployee.getStatus());
        normalized.setVersionNo(currentEmployee.getVersionNo());
        return normalized;
    }

    private boolean isOrgValid(Long orgId) {
        if (orgId == null || orgId < 1) {
            return true;
        }
        MdmOrg org = orgMapper.selectById(orgId);
        return org != null && DEL_FLAG_EXIST.equals(org.getDelFlag());
    }

    private boolean isCostCenterValid(Long costCenterId) {
        if (costCenterId == null || costCenterId < 1) {
            return true;
        }
        MdmCostCenter costCenter = costCenterMapper.selectById(costCenterId);
        return costCenter != null && DEL_FLAG_EXIST.equals(costCenter.getDelFlag());
    }

    private boolean isDeptValid(Long deptId) {
        if (deptId == null || deptId < 1) {
            return true;
        }
        SysDept dept = deptService.getById(deptId);
        return dept != null;
    }

    private boolean isUserValid(Long userId) {
        if (userId == null || userId < 1) {
            return true;
        }
        SysUser user = userService.getById(userId);
        return user != null;
    }

    private String buildBusinessNo(Long employeeId) {
        return BUSINESS_NO_PREFIX + employeeId;
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



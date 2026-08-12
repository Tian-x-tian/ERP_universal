package com.erp.system.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.erp.system.domain.MdmEmployee;
import com.erp.system.domain.MdmOrg;
import com.erp.system.domain.MdmWarehouse;
import com.erp.system.domain.SysUser;
import com.erp.workflow.contract.domain.SysWorkflowInstance;
import com.erp.workflow.contract.domain.vo.WorkflowStartBody;
import com.erp.system.mapper.MdmEmployeeMapper;
import com.erp.system.mapper.MdmOrgMapper;
import com.erp.system.security.service.SecurityUserResolver;
import com.erp.system.service.IMdmWarehouseService;
import com.erp.system.service.IMdmWarehouseWorkflowSubmitService;
import com.erp.system.service.ISysUserService;
import com.erp.system.service.ISysWorkflowEngineService;
import com.erp.system.support.MdmEmployeeStatusSupport;
import com.erp.system.support.MdmOptimisticLockSupport;
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
 * 仓库主数据审批提交流程服务实现。
 */
@Service
public class MdmWarehouseWorkflowSubmitServiceImpl implements IMdmWarehouseWorkflowSubmitService {
    private static final String DEL_FLAG_EXIST = "0";
    private static final String WORKFLOW_STATUS_RUNNING = "0";
    private static final String BUSINESS_TYPE = "MDM_WAREHOUSE";
    private static final String BUSINESS_NO_PREFIX = "MDM:WAREHOUSE:";
    private static final String META_KEY = "__mdmWarehouseMeta";

    private final IMdmWarehouseService warehouseService;
    private final ISysWorkflowEngineService workflowEngineService;
    private final SecurityUserResolver securityUserResolver;
    private final ISysUserService userService;
    private final MdmOrgMapper orgMapper;
    private final MdmEmployeeMapper employeeMapper;
    private final ObjectMapper objectMapper;

    public MdmWarehouseWorkflowSubmitServiceImpl(IMdmWarehouseService warehouseService,
            ISysWorkflowEngineService workflowEngineService,
            SecurityUserResolver securityUserResolver,
            ISysUserService userService,
            MdmOrgMapper orgMapper,
            MdmEmployeeMapper employeeMapper) {
        this.warehouseService = warehouseService;
        this.workflowEngineService = workflowEngineService;
        this.securityUserResolver = securityUserResolver;
        this.userService = userService;
        this.orgMapper = orgMapper;
        this.employeeMapper = employeeMapper;
        this.objectMapper = new ObjectMapper();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean submitDraftActivation(Long warehouseId, Integer versionNo, String processKey, String remark) {
        MdmWarehouse warehouse = loadEditableWarehouse(warehouseId);
        MdmOptimisticLockSupport.requireVersion(versionNo, warehouse.getVersionNo(), "仓库");
        if (!MdmStatusSupport.isDraft(warehouse.getStatus())) {
            throw new IllegalStateException("仅草稿仓库允许提交生效审批");
        }
        if (hasRunningWorkflow(warehouseId)) {
            throw new IllegalStateException("该仓库已有审批流程在处理中");
        }
        WorkflowStartBody startBody = buildStartBody(
                processKey,
                remark,
                warehouseId,
                MdmWorkflowActionSupport.ACTIVATE,
                warehouse.getVersionNo(),
                null,
                warehouse);
        if (!startWorkflow(startBody)) {
            throw new IllegalStateException("仓库审批流程发起失败");
        }
        if (!updateWarehouseStatus(warehouseId, warehouse.getVersionNo(), MdmStatusSupport.SUBMITTED)) {
            abortWorkflow(startBody, "仓库状态更新失败，自动中止流程");
            throw new IllegalStateException("仓库状态已变化，请刷新后重试");
        }
        return true;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean submitChange(Long warehouseId, Integer versionNo, MdmWarehouse targetWarehouse, String processKey, String remark) {
        MdmWarehouse currentWarehouse = loadEditableWarehouse(warehouseId);
        MdmOptimisticLockSupport.requireVersion(versionNo, currentWarehouse.getVersionNo(), "仓库");
        if (MdmStatusSupport.isSubmitted(currentWarehouse.getStatus())) {
            throw new IllegalStateException("仓库审批中，暂不允许提交新的变更");
        }
        if (!MdmStatusSupport.isActive(currentWarehouse.getStatus())) {
            throw new IllegalStateException("仅已生效仓库允许提交变更审批");
        }
        if (hasRunningWorkflow(warehouseId)) {
            throw new IllegalStateException("该仓库已有审批流程在处理中");
        }
        MdmWarehouse afterWarehouse = normalizeTargetWarehouse(currentWarehouse, targetWarehouse);
        WorkflowStartBody startBody = buildStartBody(
                processKey,
                remark,
                warehouseId,
                MdmWorkflowActionSupport.UPDATE,
                currentWarehouse.getVersionNo(),
                currentWarehouse,
                afterWarehouse);
        if (!startWorkflow(startBody)) {
            throw new IllegalStateException("仓库变更审批流程发起失败");
        }
        if (!updateWarehouseStatus(warehouseId, currentWarehouse.getVersionNo(), MdmStatusSupport.SUBMITTED)) {
            abortWorkflow(startBody, "仓库状态更新失败，自动中止流程");
            throw new IllegalStateException("仓库状态已变化，请刷新后重试");
        }
        return true;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean submitDisable(Long warehouseId, Integer versionNo, String processKey, String remark) {
        MdmWarehouse currentWarehouse = loadEditableWarehouse(warehouseId);
        MdmOptimisticLockSupport.requireVersion(versionNo, currentWarehouse.getVersionNo(), "仓库");
        if (MdmStatusSupport.isSubmitted(currentWarehouse.getStatus())) {
            throw new IllegalStateException("仓库审批中，暂不允许提交停用");
        }
        if (!MdmStatusSupport.isActive(currentWarehouse.getStatus())) {
            throw new IllegalStateException("仅已生效仓库允许提交停用审批");
        }
        if (hasRunningWorkflow(warehouseId)) {
            throw new IllegalStateException("该仓库已有审批流程在处理中");
        }
        MdmWarehouse afterWarehouse = new MdmWarehouse();
        BeanUtils.copyProperties(currentWarehouse, afterWarehouse);
        afterWarehouse.setStatus(MdmStatusSupport.DISABLED);
        WorkflowStartBody startBody = buildStartBody(
                processKey,
                remark,
                warehouseId,
                MdmWorkflowActionSupport.DISABLE,
                currentWarehouse.getVersionNo(),
                currentWarehouse,
                afterWarehouse);
        if (!startWorkflow(startBody)) {
            throw new IllegalStateException("仓库停用审批流程发起失败");
        }
        if (!updateWarehouseStatus(warehouseId, currentWarehouse.getVersionNo(), MdmStatusSupport.SUBMITTED)) {
            abortWorkflow(startBody, "仓库状态更新失败，自动中止流程");
            throw new IllegalStateException("仓库状态已变化，请刷新后重试");
        }
        return true;
    }

    private MdmWarehouse loadEditableWarehouse(Long warehouseId) {
        if (warehouseId == null) {
            throw new IllegalArgumentException("仓库ID不能为空");
        }
        MdmWarehouse warehouse = warehouseService.getOne(new LambdaQueryWrapper<MdmWarehouse>()
                .eq(MdmWarehouse::getWarehouseId, warehouseId)
                .eq(MdmWarehouse::getDelFlag, DEL_FLAG_EXIST));
        if (warehouse == null) {
            throw new IllegalArgumentException("仓库不存在");
        }
        return warehouse;
    }

    private boolean hasRunningWorkflow(Long warehouseId) {
        return workflowEngineService.hasRunningInstance(BUSINESS_TYPE, buildBusinessNo(warehouseId));
    }

    private WorkflowStartBody buildStartBody(String processKey,
            String remark,
            Long warehouseId,
            String action,
            Integer baseVersionNo,
            MdmWarehouse beforeWarehouse,
            MdmWarehouse afterWarehouse) {
        if (!StringUtils.hasText(processKey)) {
            throw new IllegalArgumentException("流程标识不能为空");
        }
        Map<String, Object> formData = new LinkedHashMap<>();
        if (afterWarehouse != null) {
            formData.putAll(objectMapper.convertValue(afterWarehouse, new TypeReference<Map<String, Object>>() { }));
        }
        Map<String, Object> meta = new LinkedHashMap<>();
        meta.put("action", action);
        meta.put("warehouseId", warehouseId);
        meta.put("baseVersionNo", baseVersionNo);
        meta.put("beforeWarehouse", beforeWarehouse);
        meta.put("afterWarehouse", afterWarehouse);
        formData.put(META_KEY, meta);

        WorkflowStartBody startBody = new WorkflowStartBody();
        startBody.setProcessKey(processKey.trim());
        startBody.setRequestedProcessKey(processKey.trim());
        startBody.setOwnerService("system");
        startBody.setBusinessNo(buildBusinessNo(warehouseId));
        startBody.setBusinessType(BUSINESS_TYPE);
        startBody.setDomainType(BUSINESS_TYPE);
        startBody.setActionCode(action);
        startBody.setIdempotencyKey(buildBusinessNo(warehouseId));
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

    private boolean updateWarehouseStatus(Long warehouseId, Integer currentVersion, String targetStatus) {
        MdmWarehouse updateEntity = new MdmWarehouse();
        updateEntity.setWarehouseId(warehouseId);
        updateEntity.setStatus(targetStatus);
        return warehouseService.update(updateEntity, new LambdaUpdateWrapper<MdmWarehouse>()
                .eq(MdmWarehouse::getWarehouseId, warehouseId)
                .eq(MdmWarehouse::getDelFlag, DEL_FLAG_EXIST)
                .eq(currentVersion != null, MdmWarehouse::getVersionNo, currentVersion));
    }

    private MdmWarehouse normalizeTargetWarehouse(MdmWarehouse currentWarehouse, MdmWarehouse targetWarehouse) {
        if (currentWarehouse == null || targetWarehouse == null) {
            throw new IllegalArgumentException("仓库变更数据不能为空");
        }
        MdmWarehouse normalized = new MdmWarehouse();
        BeanUtils.copyProperties(currentWarehouse, normalized);
        normalized.setWarehouseId(currentWarehouse.getWarehouseId());
        normalized.setTenantId(currentWarehouse.getTenantId());
        normalized.setWhCode(currentWarehouse.getWhCode());
        normalized.setWhName(trimRequired(targetWarehouse.getWhName(), "仓库名称不能为空"));
        normalized.setWhType(MdmValueSupport.trimToNull(targetWarehouse.getWhType()));
        normalized.setOrgId(targetWarehouse.getOrgId());
        normalized.setManagerEmpId(targetWarehouse.getManagerEmpId());
        if (!isOrgValid(normalized.getOrgId()) || !isManagerValid(normalized.getManagerEmpId())) {
            throw new IllegalArgumentException("仓库组织或负责人无效");
        }
        normalized.setAddress(MdmValueSupport.trimToNull(targetWarehouse.getAddress()));
        normalized.setAllowNegativeStock(MdmValueSupport.normalizeYN(targetWarehouse.getAllowNegativeStock(),
                currentWarehouse.getAllowNegativeStock()));
        normalized.setRemark(MdmValueSupport.trimToNull(targetWarehouse.getRemark()));
        normalized.setStatus(currentWarehouse.getStatus());
        normalized.setVersionNo(currentWarehouse.getVersionNo());
        return normalized;
    }

    private boolean isOrgValid(Long orgId) {
        if (orgId == null || orgId < 1) {
            return true;
        }
        MdmOrg org = orgMapper.selectById(orgId);
        return org != null && DEL_FLAG_EXIST.equals(org.getDelFlag());
    }

    private boolean isManagerValid(Long managerEmpId) {
        if (managerEmpId == null || managerEmpId < 1) {
            return true;
        }
        MdmEmployee employee = employeeMapper.selectById(managerEmpId);
        return employee != null
                && DEL_FLAG_EXIST.equals(employee.getDelFlag())
                && MdmEmployeeStatusSupport.isActive(employee.getStatus());
    }

    private String buildBusinessNo(Long warehouseId) {
        return BUSINESS_NO_PREFIX + warehouseId;
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



package com.erp.system.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.erp.system.domain.MdmEmployee;
import com.erp.system.domain.SysWorkflowInstance;
import com.erp.system.mapper.MdmEmployeeMapper;
import com.erp.system.service.IMdmAuditTrailService;
import com.erp.system.service.IWorkflowBusinessCallback;
import com.erp.system.support.MdmChangeTypeSupport;
import com.erp.system.support.MdmDomainTypeSupport;
import com.erp.system.support.MdmEmployeeStatusSupport;
import com.erp.system.support.MdmValueSupport;
import com.erp.system.support.MdmWorkflowActionSupport;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.Date;
import java.util.Map;

/**
 * 员工主数据工作流终态回调实现。
 */
@Service
public class MdmEmployeeWorkflowCallbackServiceImpl implements IWorkflowBusinessCallback {
    private static final String DEL_FLAG_EXIST = "0";
    private static final String BUSINESS_TYPE = "MDM_EMPLOYEE";
    private static final String META_KEY = "__mdmEmployeeMeta";

    private final MdmEmployeeMapper employeeMapper;
    private final IMdmAuditTrailService auditTrailService;
    private final ObjectMapper objectMapper;

    public MdmEmployeeWorkflowCallbackServiceImpl(MdmEmployeeMapper employeeMapper,
            IMdmAuditTrailService auditTrailService) {
        this.employeeMapper = employeeMapper;
        this.auditTrailService = auditTrailService;
        this.objectMapper = new ObjectMapper();
    }

    @Override
    public boolean supports(String businessType) {
        return BUSINESS_TYPE.equalsIgnoreCase(StringUtils.trimWhitespace(businessType));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void onWorkflowCompleted(SysWorkflowInstance instance) {
        Map<String, Object> meta = readMeta(instance);
        String action = readString(meta.get("action"));
        Long employeeId = readLong(meta.get("employeeId"));
        Integer baseVersionNo = readInteger(meta.get("baseVersionNo"));
        if (employeeId == null || !StringUtils.hasText(action)) {
            return;
        }
        if (MdmWorkflowActionSupport.ACTIVATE.equalsIgnoreCase(action)) {
            activateEmployee(employeeId, baseVersionNo, instance);
            return;
        }
        if (MdmWorkflowActionSupport.UPDATE.equalsIgnoreCase(action)) {
            applyApprovedChange(employeeId, baseVersionNo, meta, instance);
            return;
        }
        if (MdmWorkflowActionSupport.DISABLE.equalsIgnoreCase(action)) {
            leaveEmployee(employeeId, baseVersionNo, instance);
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

    private void rollbackSubmittedDraft(SysWorkflowInstance instance) {
        Map<String, Object> meta = readMeta(instance);
        String action = readString(meta.get("action"));
        Long employeeId = readLong(meta.get("employeeId"));
        if (!MdmWorkflowActionSupport.ACTIVATE.equalsIgnoreCase(action) || employeeId == null) {
            return;
        }
        employeeMapper.update(new MdmEmployee(), new LambdaUpdateWrapper<MdmEmployee>()
                .eq(MdmEmployee::getEmployeeId, employeeId)
                .eq(MdmEmployee::getDelFlag, DEL_FLAG_EXIST)
                .eq(MdmEmployee::getStatus, MdmEmployeeStatusSupport.SUBMITTED)
                .set(MdmEmployee::getStatus, MdmEmployeeStatusSupport.DRAFT)
                .set(MdmEmployee::getUpdateBy, resolveOperator(instance))
                .set(MdmEmployee::getUpdateTime, new Date()));
    }

    private void activateEmployee(Long employeeId, Integer baseVersionNo, SysWorkflowInstance instance) {
        MdmEmployee before = loadEmployee(employeeId);
        if (before == null) {
            throw new IllegalStateException("员工不存在，无法完成审批回写");
        }
        if (!MdmEmployeeStatusSupport.isSubmitted(before.getStatus())) {
            throw new IllegalStateException("员工状态已变化，无法完成审批回写");
        }
        if (baseVersionNo != null && before.getVersionNo() != null && !baseVersionNo.equals(before.getVersionNo())) {
            throw new IllegalStateException("员工版本已变化，无法完成审批回写");
        }
        employeeMapper.update(new MdmEmployee(), new LambdaUpdateWrapper<MdmEmployee>()
                .eq(MdmEmployee::getEmployeeId, employeeId)
                .eq(MdmEmployee::getDelFlag, DEL_FLAG_EXIST)
                .eq(MdmEmployee::getStatus, MdmEmployeeStatusSupport.SUBMITTED)
                .eq(baseVersionNo != null, MdmEmployee::getVersionNo, baseVersionNo)
                .set(MdmEmployee::getStatus, MdmEmployeeStatusSupport.ACTIVE)
                .set(before.getEffectiveTime() == null, MdmEmployee::getEffectiveTime, new Date())
                .set(MdmEmployee::getUpdateBy, resolveOperator(instance))
                .set(MdmEmployee::getUpdateTime, new Date()));
        MdmEmployee after = loadEmployee(employeeId);
        auditTrailService.record(MdmDomainTypeSupport.EMPLOYEE,
                employeeId,
                MdmChangeTypeSupport.STATUS,
                after == null ? before.getVersionNo() : after.getVersionNo(),
                after == null ? MdmEmployeeStatusSupport.ACTIVE : after.getStatus(),
                before,
                after);
    }

    private void applyApprovedChange(Long employeeId,
            Integer baseVersionNo,
            Map<String, Object> meta,
            SysWorkflowInstance instance) {
        MdmEmployee before = loadEmployee(employeeId);
        if (before == null) {
            throw new IllegalStateException("员工不存在，无法完成变更回写");
        }
        if (baseVersionNo != null && before.getVersionNo() != null && !baseVersionNo.equals(before.getVersionNo())) {
            throw new IllegalStateException("员工版本已变化，请重新发起审批");
        }
        MdmEmployee afterEmployee = objectMapper.convertValue(meta.get("afterEmployee"), MdmEmployee.class);
        if (afterEmployee == null) {
            throw new IllegalStateException("审批回写缺少员工变更数据");
        }
        MdmEmployee updateEntity = new MdmEmployee();
        BeanUtils.copyProperties(afterEmployee, updateEntity);
        updateEntity.setEmployeeId(employeeId);
        updateEntity.setTenantId(before.getTenantId());
        updateEntity.setEmpCode(before.getEmpCode());
        updateEntity.setStatus(before.getStatus());
        updateEntity.setVersionNo(MdmValueSupport.resolveNextVersionNo(before.getVersionNo()));
        updateEntity.setUpdateBy(resolveOperator(instance));
        updateEntity.setUpdateTime(new Date());
        updateEntity.setCreateBy(null);
        updateEntity.setCreateTime(null);
        updateEntity.setDelFlag(null);
        boolean updated = employeeMapper.update(updateEntity, new LambdaUpdateWrapper<MdmEmployee>()
                .eq(MdmEmployee::getEmployeeId, employeeId)
                .eq(MdmEmployee::getDelFlag, DEL_FLAG_EXIST)
                .eq(baseVersionNo != null, MdmEmployee::getVersionNo, baseVersionNo)) > 0;
        if (!updated) {
            throw new IllegalStateException("员工版本已变化，请重新发起审批");
        }
        MdmEmployee after = loadEmployee(employeeId);
        auditTrailService.record(MdmDomainTypeSupport.EMPLOYEE,
                employeeId,
                MdmChangeTypeSupport.UPDATE,
                after == null ? updateEntity.getVersionNo() : after.getVersionNo(),
                after == null ? before.getStatus() : after.getStatus(),
                before,
                after);
    }

    private void leaveEmployee(Long employeeId, Integer baseVersionNo, SysWorkflowInstance instance) {
        MdmEmployee before = loadEmployee(employeeId);
        if (before == null) {
            throw new IllegalStateException("员工不存在，无法完成离职回写");
        }
        if (baseVersionNo != null && before.getVersionNo() != null && !baseVersionNo.equals(before.getVersionNo())) {
            throw new IllegalStateException("员工版本已变化，请重新发起审批");
        }
        MdmEmployee updateEntity = new MdmEmployee();
        updateEntity.setEmployeeId(employeeId);
        updateEntity.setStatus(MdmEmployeeStatusSupport.LEAVE);
        updateEntity.setVersionNo(MdmValueSupport.resolveNextVersionNo(before.getVersionNo()));
        updateEntity.setUpdateBy(resolveOperator(instance));
        updateEntity.setUpdateTime(new Date());
        boolean updated = employeeMapper.update(updateEntity, new LambdaUpdateWrapper<MdmEmployee>()
                .eq(MdmEmployee::getEmployeeId, employeeId)
                .eq(MdmEmployee::getDelFlag, DEL_FLAG_EXIST)
                .eq(baseVersionNo != null, MdmEmployee::getVersionNo, baseVersionNo)) > 0;
        if (!updated) {
            throw new IllegalStateException("员工版本已变化，请重新发起审批");
        }
        MdmEmployee after = loadEmployee(employeeId);
        auditTrailService.record(MdmDomainTypeSupport.EMPLOYEE,
                employeeId,
                MdmChangeTypeSupport.STATUS,
                after == null ? updateEntity.getVersionNo() : after.getVersionNo(),
                after == null ? MdmEmployeeStatusSupport.LEAVE : after.getStatus(),
                before,
                after);
    }

    private MdmEmployee loadEmployee(Long employeeId) {
        return employeeMapper.selectOne(new LambdaQueryWrapper<MdmEmployee>()
                .eq(MdmEmployee::getEmployeeId, employeeId)
                .eq(MdmEmployee::getDelFlag, DEL_FLAG_EXIST));
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> readMeta(SysWorkflowInstance instance) {
        if (instance == null || !StringUtils.hasText(instance.getFormData())) {
            return Map.of();
        }
        try {
            Map<String, Object> formData = objectMapper.readValue(instance.getFormData(), Map.class);
            Object meta = formData.get(META_KEY);
            return meta instanceof Map ? (Map<String, Object>) meta : Map.of();
        } catch (Exception ex) {
            throw new IllegalStateException("解析员工审批元数据失败", ex);
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

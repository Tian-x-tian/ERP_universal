package com.erp.system.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.erp.system.domain.MdmWarehouse;
import com.erp.workflow.contract.domain.SysWorkflowInstance;
import com.erp.system.mapper.MdmWarehouseMapper;
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
 * 仓库主数据工作流终态回调实现。
 */
@Service
public class MdmWarehouseWorkflowCallbackServiceImpl implements IWorkflowBusinessCallback {
    private static final String DEL_FLAG_EXIST = "0";
    private static final String BUSINESS_TYPE = "MDM_WAREHOUSE";
    private static final String META_KEY = "__mdmWarehouseMeta";

    private final MdmWarehouseMapper warehouseMapper;
    private final IMdmAuditTrailService auditTrailService;
    private final ObjectMapper objectMapper;

    public MdmWarehouseWorkflowCallbackServiceImpl(MdmWarehouseMapper warehouseMapper,
            IMdmAuditTrailService auditTrailService) {
        this.warehouseMapper = warehouseMapper;
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
        Long warehouseId = readLong(meta.get("warehouseId"));
        Integer baseVersionNo = readInteger(meta.get("baseVersionNo"));
        if (warehouseId == null || !StringUtils.hasText(action)) {
            return;
        }
        if (MdmWorkflowActionSupport.ACTIVATE.equalsIgnoreCase(action)) {
            activateWarehouse(warehouseId, baseVersionNo, instance);
            return;
        }
        if (MdmWorkflowActionSupport.UPDATE.equalsIgnoreCase(action)) {
            applyApprovedChange(warehouseId, baseVersionNo, meta, instance);
            return;
        }
        if (MdmWorkflowActionSupport.DISABLE.equalsIgnoreCase(action)) {
            disableWarehouse(warehouseId, baseVersionNo, instance);
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
        Long warehouseId = readLong(meta.get("warehouseId"));
        if (warehouseId == null) {
            return;
        }
        String rollbackStatus = MdmWorkflowActionSupport.ACTIVATE.equalsIgnoreCase(action)
                ? MdmStatusSupport.DRAFT
                : MdmStatusSupport.ACTIVE;
        warehouseMapper.update(new MdmWarehouse(), new LambdaUpdateWrapper<MdmWarehouse>()
                .eq(MdmWarehouse::getWarehouseId, warehouseId)
                .eq(MdmWarehouse::getDelFlag, DEL_FLAG_EXIST)
                .eq(MdmWarehouse::getStatus, MdmStatusSupport.SUBMITTED)
                .set(MdmWarehouse::getStatus, rollbackStatus)
                .set(MdmWarehouse::getUpdateBy, resolveOperator(instance))
                .set(MdmWarehouse::getUpdateTime, new Date()));
    }

    private void activateWarehouse(Long warehouseId, Integer baseVersionNo, SysWorkflowInstance instance) {
        MdmWarehouse before = loadWarehouse(warehouseId);
        if (before == null) {
            throw new IllegalStateException("仓库不存在，无法完成审批回写");
        }
        if (!MdmStatusSupport.isSubmitted(before.getStatus())) {
            throw new IllegalStateException("仓库状态已变化，无法完成审批回写");
        }
        if (baseVersionNo != null && before.getVersionNo() != null && !baseVersionNo.equals(before.getVersionNo())) {
            throw new IllegalStateException("仓库版本已变化，无法完成审批回写");
        }
        boolean updated = warehouseMapper.update(new MdmWarehouse(), new LambdaUpdateWrapper<MdmWarehouse>()
                .eq(MdmWarehouse::getWarehouseId, warehouseId)
                .eq(MdmWarehouse::getDelFlag, DEL_FLAG_EXIST)
                .eq(MdmWarehouse::getStatus, MdmStatusSupport.SUBMITTED)
                .eq(baseVersionNo != null, MdmWarehouse::getVersionNo, baseVersionNo)
                .set(MdmWarehouse::getStatus, MdmStatusSupport.ACTIVE)
                .set(before.getEffectiveTime() == null, MdmWarehouse::getEffectiveTime, new Date())
                .set(MdmWarehouse::getUpdateBy, resolveOperator(instance))
                .set(MdmWarehouse::getUpdateTime, new Date())) > 0;
        if (!updated) {
            throw new IllegalStateException("仓库状态已变化，无法完成审批回写");
        }
        MdmWarehouse after = loadWarehouse(warehouseId);
        auditTrailService.record(MdmDomainTypeSupport.WAREHOUSE,
                warehouseId,
                MdmChangeTypeSupport.STATUS,
                after == null ? before.getVersionNo() : after.getVersionNo(),
                after == null ? MdmStatusSupport.ACTIVE : after.getStatus(),
                before,
                after);
    }

    private void applyApprovedChange(Long warehouseId,
            Integer baseVersionNo,
            Map<String, Object> meta,
            SysWorkflowInstance instance) {
        MdmWarehouse before = loadWarehouse(warehouseId);
        if (before == null) {
            throw new IllegalStateException("仓库不存在，无法完成变更回写");
        }
        if (!MdmStatusSupport.isSubmitted(before.getStatus())) {
            throw new IllegalStateException("仓库状态已变化，请重新发起审批");
        }
        if (baseVersionNo != null && before.getVersionNo() != null && !baseVersionNo.equals(before.getVersionNo())) {
            throw new IllegalStateException("仓库版本已变化，请重新发起审批");
        }
        MdmWarehouse afterWarehouse = objectMapper.convertValue(meta.get("afterWarehouse"), MdmWarehouse.class);
        if (afterWarehouse == null) {
            throw new IllegalStateException("审批回写缺少仓库变更数据");
        }
        MdmWarehouse updateEntity = new MdmWarehouse();
        BeanUtils.copyProperties(afterWarehouse, updateEntity);
        updateEntity.setWarehouseId(warehouseId);
        updateEntity.setTenantId(before.getTenantId());
        updateEntity.setWhCode(before.getWhCode());
        updateEntity.setStatus(MdmStatusSupport.ACTIVE);
        updateEntity.setVersionNo(MdmValueSupport.resolveNextVersionNo(before.getVersionNo()));
        updateEntity.setUpdateBy(resolveOperator(instance));
        updateEntity.setUpdateTime(new Date());
        updateEntity.setCreateBy(null);
        updateEntity.setCreateTime(null);
        updateEntity.setDelFlag(null);
        boolean updated = warehouseMapper.update(updateEntity, new LambdaUpdateWrapper<MdmWarehouse>()
                .eq(MdmWarehouse::getWarehouseId, warehouseId)
                .eq(MdmWarehouse::getDelFlag, DEL_FLAG_EXIST)
                .eq(MdmWarehouse::getStatus, MdmStatusSupport.SUBMITTED)
                .eq(baseVersionNo != null, MdmWarehouse::getVersionNo, baseVersionNo)) > 0;
        if (!updated) {
            throw new IllegalStateException("仓库版本已变化，请重新发起审批");
        }
        MdmWarehouse after = loadWarehouse(warehouseId);
        auditTrailService.record(MdmDomainTypeSupport.WAREHOUSE,
                warehouseId,
                MdmChangeTypeSupport.UPDATE,
                after == null ? updateEntity.getVersionNo() : after.getVersionNo(),
                after == null ? before.getStatus() : after.getStatus(),
                before,
                after);
    }

    private void disableWarehouse(Long warehouseId, Integer baseVersionNo, SysWorkflowInstance instance) {
        MdmWarehouse before = loadWarehouse(warehouseId);
        if (before == null) {
            throw new IllegalStateException("仓库不存在，无法完成停用回写");
        }
        if (!MdmStatusSupport.isSubmitted(before.getStatus())) {
            throw new IllegalStateException("仓库状态已变化，请重新发起审批");
        }
        if (baseVersionNo != null && before.getVersionNo() != null && !baseVersionNo.equals(before.getVersionNo())) {
            throw new IllegalStateException("仓库版本已变化，请重新发起审批");
        }
        MdmWarehouse updateEntity = new MdmWarehouse();
        updateEntity.setWarehouseId(warehouseId);
        updateEntity.setStatus(MdmStatusSupport.DISABLED);
        updateEntity.setVersionNo(MdmValueSupport.resolveNextVersionNo(before.getVersionNo()));
        updateEntity.setUpdateBy(resolveOperator(instance));
        updateEntity.setUpdateTime(new Date());
        boolean updated = warehouseMapper.update(updateEntity, new LambdaUpdateWrapper<MdmWarehouse>()
                .eq(MdmWarehouse::getWarehouseId, warehouseId)
                .eq(MdmWarehouse::getDelFlag, DEL_FLAG_EXIST)
                .eq(MdmWarehouse::getStatus, MdmStatusSupport.SUBMITTED)
                .eq(baseVersionNo != null, MdmWarehouse::getVersionNo, baseVersionNo)) > 0;
        if (!updated) {
            throw new IllegalStateException("仓库版本已变化，请重新发起审批");
        }
        MdmWarehouse after = loadWarehouse(warehouseId);
        auditTrailService.record(MdmDomainTypeSupport.WAREHOUSE,
                warehouseId,
                MdmChangeTypeSupport.STATUS,
                after == null ? updateEntity.getVersionNo() : after.getVersionNo(),
                after == null ? MdmStatusSupport.DISABLED : after.getStatus(),
                before,
                after);
    }

    private MdmWarehouse loadWarehouse(Long warehouseId) {
        return warehouseMapper.selectOne(new LambdaQueryWrapper<MdmWarehouse>()
                .eq(MdmWarehouse::getWarehouseId, warehouseId)
                .eq(MdmWarehouse::getDelFlag, DEL_FLAG_EXIST));
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
            throw new IllegalStateException("解析仓库审批元数据失败", ex);
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


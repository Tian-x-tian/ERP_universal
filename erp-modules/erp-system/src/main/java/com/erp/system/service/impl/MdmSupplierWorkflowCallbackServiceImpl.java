package com.erp.system.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.erp.system.domain.MdmSupplier;
import com.erp.system.domain.SysWorkflowInstance;
import com.erp.system.mapper.MdmSupplierMapper;
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
 * 供应商主数据工作流终态回调实现。
 */
@Service
public class MdmSupplierWorkflowCallbackServiceImpl implements IWorkflowBusinessCallback {
    private static final String DEL_FLAG_EXIST = "0";
    private static final String BUSINESS_TYPE = "MDM_SUPPLIER";
    private static final String META_KEY = "__mdmSupplierMeta";

    private final MdmSupplierMapper supplierMapper;
    private final IMdmAuditTrailService auditTrailService;
    private final ObjectMapper objectMapper;

    public MdmSupplierWorkflowCallbackServiceImpl(MdmSupplierMapper supplierMapper,
                                                  IMdmAuditTrailService auditTrailService) {
        this.supplierMapper = supplierMapper;
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
        Long supplierId = readLong(meta.get("supplierId"));
        Integer baseVersionNo = readInteger(meta.get("baseVersionNo"));
        if (supplierId == null || !StringUtils.hasText(action)) {
            return;
        }
        if (MdmWorkflowActionSupport.ACTIVATE.equalsIgnoreCase(action)) {
            activateSupplier(supplierId, baseVersionNo, instance);
            return;
        }
        if (MdmWorkflowActionSupport.UPDATE.equalsIgnoreCase(action)) {
            applyApprovedChange(supplierId, baseVersionNo, meta, instance);
            return;
        }
        if (MdmWorkflowActionSupport.DISABLE.equalsIgnoreCase(action)) {
            disableSupplier(supplierId, baseVersionNo, instance);
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
        Long supplierId = readLong(meta.get("supplierId"));
        if (!MdmWorkflowActionSupport.ACTIVATE.equalsIgnoreCase(action) || supplierId == null) {
            return;
        }
        supplierMapper.update(new MdmSupplier(), new LambdaUpdateWrapper<MdmSupplier>()
                .eq(MdmSupplier::getSupplierId, supplierId)
                .eq(MdmSupplier::getDelFlag, DEL_FLAG_EXIST)
                .eq(MdmSupplier::getStatus, MdmStatusSupport.SUBMITTED)
                .set(MdmSupplier::getStatus, MdmStatusSupport.DRAFT)
                .set(MdmSupplier::getUpdateBy, resolveOperator(instance))
                .set(MdmSupplier::getUpdateTime, new Date()));
    }

    private void activateSupplier(Long supplierId, Integer baseVersionNo, SysWorkflowInstance instance) {
        MdmSupplier before = loadSupplier(supplierId);
        if (before == null) {
            throw new IllegalStateException("供应商不存在，无法完成审批回写");
        }
        if (!MdmStatusSupport.isSubmitted(before.getStatus())) {
            throw new IllegalStateException("供应商状态已变化，无法完成审批回写");
        }
        if (baseVersionNo != null && before.getVersionNo() != null && !baseVersionNo.equals(before.getVersionNo())) {
            throw new IllegalStateException("供应商版本已变化，无法完成审批回写");
        }
        supplierMapper.update(new MdmSupplier(), new LambdaUpdateWrapper<MdmSupplier>()
                .eq(MdmSupplier::getSupplierId, supplierId)
                .eq(MdmSupplier::getDelFlag, DEL_FLAG_EXIST)
                .eq(MdmSupplier::getStatus, MdmStatusSupport.SUBMITTED)
                .eq(baseVersionNo != null, MdmSupplier::getVersionNo, baseVersionNo)
                .set(MdmSupplier::getStatus, MdmStatusSupport.ACTIVE)
                .set(before.getEffectiveTime() == null, MdmSupplier::getEffectiveTime, new Date())
                .set(MdmSupplier::getUpdateBy, resolveOperator(instance))
                .set(MdmSupplier::getUpdateTime, new Date()));
        MdmSupplier after = loadSupplier(supplierId);
        auditTrailService.record(MdmDomainTypeSupport.SUPPLIER,
                supplierId,
                MdmChangeTypeSupport.STATUS,
                after == null ? before.getVersionNo() : after.getVersionNo(),
                after == null ? MdmStatusSupport.ACTIVE : after.getStatus(),
                before,
                after);
    }

    private void applyApprovedChange(Long supplierId,
                                     Integer baseVersionNo,
                                     Map<String, Object> meta,
                                     SysWorkflowInstance instance) {
        MdmSupplier before = loadSupplier(supplierId);
        if (before == null) {
            throw new IllegalStateException("供应商不存在，无法完成变更回写");
        }
        if (baseVersionNo != null && before.getVersionNo() != null && !baseVersionNo.equals(before.getVersionNo())) {
            throw new IllegalStateException("供应商版本已变化，请重新发起审批");
        }
        MdmSupplier afterSupplier = objectMapper.convertValue(meta.get("afterSupplier"), MdmSupplier.class);
        if (afterSupplier == null) {
            throw new IllegalStateException("审批回写缺少供应商变更数据");
        }
        MdmSupplier updateEntity = new MdmSupplier();
        BeanUtils.copyProperties(afterSupplier, updateEntity);
        updateEntity.setSupplierId(supplierId);
        updateEntity.setTenantId(before.getTenantId());
        updateEntity.setSupplierCode(before.getSupplierCode());
        updateEntity.setStatus(before.getStatus());
        updateEntity.setVersionNo(MdmValueSupport.resolveNextVersionNo(before.getVersionNo()));
        updateEntity.setUpdateBy(resolveOperator(instance));
        updateEntity.setUpdateTime(new Date());
        updateEntity.setCreateBy(null);
        updateEntity.setCreateTime(null);
        updateEntity.setDelFlag(null);
        boolean updated = supplierMapper.update(updateEntity, new LambdaUpdateWrapper<MdmSupplier>()
                .eq(MdmSupplier::getSupplierId, supplierId)
                .eq(MdmSupplier::getDelFlag, DEL_FLAG_EXIST)
                .eq(baseVersionNo != null, MdmSupplier::getVersionNo, baseVersionNo)) > 0;
        if (!updated) {
            throw new IllegalStateException("供应商版本已变化，请重新发起审批");
        }
        MdmSupplier after = loadSupplier(supplierId);
        auditTrailService.record(MdmDomainTypeSupport.SUPPLIER,
                supplierId,
                MdmChangeTypeSupport.UPDATE,
                after == null ? updateEntity.getVersionNo() : after.getVersionNo(),
                after == null ? before.getStatus() : after.getStatus(),
                before,
                after);
    }

    private void disableSupplier(Long supplierId, Integer baseVersionNo, SysWorkflowInstance instance) {
        MdmSupplier before = loadSupplier(supplierId);
        if (before == null) {
            throw new IllegalStateException("供应商不存在，无法完成停用回写");
        }
        if (baseVersionNo != null && before.getVersionNo() != null && !baseVersionNo.equals(before.getVersionNo())) {
            throw new IllegalStateException("供应商版本已变化，请重新发起审批");
        }
        MdmSupplier updateEntity = new MdmSupplier();
        updateEntity.setSupplierId(supplierId);
        updateEntity.setStatus(MdmStatusSupport.DISABLED);
        updateEntity.setVersionNo(MdmValueSupport.resolveNextVersionNo(before.getVersionNo()));
        updateEntity.setUpdateBy(resolveOperator(instance));
        updateEntity.setUpdateTime(new Date());
        boolean updated = supplierMapper.update(updateEntity, new LambdaUpdateWrapper<MdmSupplier>()
                .eq(MdmSupplier::getSupplierId, supplierId)
                .eq(MdmSupplier::getDelFlag, DEL_FLAG_EXIST)
                .eq(baseVersionNo != null, MdmSupplier::getVersionNo, baseVersionNo)) > 0;
        if (!updated) {
            throw new IllegalStateException("供应商版本已变化，请重新发起审批");
        }
        MdmSupplier after = loadSupplier(supplierId);
        auditTrailService.record(MdmDomainTypeSupport.SUPPLIER,
                supplierId,
                MdmChangeTypeSupport.STATUS,
                after == null ? updateEntity.getVersionNo() : after.getVersionNo(),
                after == null ? MdmStatusSupport.DISABLED : after.getStatus(),
                before,
                after);
    }

    private MdmSupplier loadSupplier(Long supplierId) {
        return supplierMapper.selectOne(new LambdaQueryWrapper<MdmSupplier>()
                .eq(MdmSupplier::getSupplierId, supplierId)
                .eq(MdmSupplier::getDelFlag, DEL_FLAG_EXIST));
    }

    private Map<String, Object> readMeta(SysWorkflowInstance instance) {
        if (instance == null || !StringUtils.hasText(instance.getFormData())) {
            return Map.of();
        }
        try {
            Map<String, Object> formData = objectMapper.readValue(instance.getFormData(), new TypeReference<Map<String, Object>>() { });
            Object meta = formData.get(META_KEY);
            if (!(meta instanceof Map)) {
                return Map.of();
            }
            return (Map<String, Object>) meta;
        } catch (Exception ex) {
            throw new IllegalStateException("解析供应商审批回写数据失败", ex);
        }
    }

    private String readString(Object value) {
        return value == null ? null : String.valueOf(value).trim();
    }

    private Long readLong(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Number) {
            return ((Number) value).longValue();
        }
        try {
            return Long.parseLong(String.valueOf(value).trim());
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private Integer readInteger(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Number) {
            return ((Number) value).intValue();
        }
        try {
            return Integer.parseInt(String.valueOf(value).trim());
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

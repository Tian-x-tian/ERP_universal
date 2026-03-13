package com.erp.system.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.erp.system.domain.MdmItem;
import com.erp.system.domain.SysWorkflowInstance;
import com.erp.system.mapper.MdmItemMapper;
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
 * 物料主数据工作流终态回调实现。
 */
@Service
public class MdmItemWorkflowCallbackServiceImpl implements IWorkflowBusinessCallback {
    private static final String DEL_FLAG_EXIST = "0";
    private static final String BUSINESS_TYPE = "MDM_ITEM";
    private static final String META_KEY = "__mdmItemMeta";

    private final MdmItemMapper itemMapper;
    private final IMdmAuditTrailService auditTrailService;
    private final ObjectMapper objectMapper;

    public MdmItemWorkflowCallbackServiceImpl(MdmItemMapper itemMapper,
                                              IMdmAuditTrailService auditTrailService) {
        this.itemMapper = itemMapper;
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
        Long itemId = readLong(meta.get("itemId"));
        Integer baseVersionNo = readInteger(meta.get("baseVersionNo"));
        if (itemId == null || !StringUtils.hasText(action)) {
            return;
        }
        if (MdmWorkflowActionSupport.ACTIVATE.equalsIgnoreCase(action)) {
            activateItem(itemId, baseVersionNo, instance);
            return;
        }
        if (MdmWorkflowActionSupport.UPDATE.equalsIgnoreCase(action)) {
            applyApprovedChange(itemId, baseVersionNo, meta, instance);
            return;
        }
        if (MdmWorkflowActionSupport.DISABLE.equalsIgnoreCase(action)) {
            disableItem(itemId, baseVersionNo, instance);
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
        Long itemId = readLong(meta.get("itemId"));
        if (!MdmWorkflowActionSupport.ACTIVATE.equalsIgnoreCase(action) || itemId == null) {
            return;
        }
        itemMapper.update(new MdmItem(), new LambdaUpdateWrapper<MdmItem>()
                .eq(MdmItem::getItemId, itemId)
                .eq(MdmItem::getDelFlag, DEL_FLAG_EXIST)
                .eq(MdmItem::getStatus, MdmStatusSupport.SUBMITTED)
                .set(MdmItem::getStatus, MdmStatusSupport.DRAFT)
                .set(MdmItem::getUpdateBy, resolveOperator(instance))
                .set(MdmItem::getUpdateTime, new Date()));
    }

    private void activateItem(Long itemId, Integer baseVersionNo, SysWorkflowInstance instance) {
        MdmItem before = loadItem(itemId);
        if (before == null) {
            throw new IllegalStateException("物料不存在，无法完成审批回写");
        }
        if (!MdmStatusSupport.isSubmitted(before.getStatus())) {
            throw new IllegalStateException("物料状态已变化，无法完成审批回写");
        }
        if (baseVersionNo != null && before.getVersionNo() != null && !baseVersionNo.equals(before.getVersionNo())) {
            throw new IllegalStateException("物料版本已变化，无法完成审批回写");
        }
        itemMapper.update(new MdmItem(), new LambdaUpdateWrapper<MdmItem>()
                .eq(MdmItem::getItemId, itemId)
                .eq(MdmItem::getDelFlag, DEL_FLAG_EXIST)
                .eq(MdmItem::getStatus, MdmStatusSupport.SUBMITTED)
                .eq(baseVersionNo != null, MdmItem::getVersionNo, baseVersionNo)
                .set(MdmItem::getStatus, MdmStatusSupport.ACTIVE)
                .set(before.getEffectiveTime() == null, MdmItem::getEffectiveTime, new Date())
                .set(MdmItem::getUpdateBy, resolveOperator(instance))
                .set(MdmItem::getUpdateTime, new Date()));
        MdmItem after = loadItem(itemId);
        auditTrailService.record(MdmDomainTypeSupport.ITEM,
                itemId,
                MdmChangeTypeSupport.STATUS,
                after == null ? before.getVersionNo() : after.getVersionNo(),
                after == null ? MdmStatusSupport.ACTIVE : after.getStatus(),
                before,
                after);
    }

    private void applyApprovedChange(Long itemId,
                                     Integer baseVersionNo,
                                     Map<String, Object> meta,
                                     SysWorkflowInstance instance) {
        MdmItem before = loadItem(itemId);
        if (before == null) {
            throw new IllegalStateException("物料不存在，无法完成变更回写");
        }
        if (baseVersionNo != null && before.getVersionNo() != null && !baseVersionNo.equals(before.getVersionNo())) {
            throw new IllegalStateException("物料版本已变化，请重新发起审批");
        }
        MdmItem afterItem = objectMapper.convertValue(meta.get("afterItem"), MdmItem.class);
        if (afterItem == null) {
            throw new IllegalStateException("审批回写缺少物料变更数据");
        }
        MdmItem updateEntity = new MdmItem();
        BeanUtils.copyProperties(afterItem, updateEntity);
        updateEntity.setItemId(itemId);
        updateEntity.setTenantId(before.getTenantId());
        updateEntity.setItemCode(before.getItemCode());
        updateEntity.setStatus(before.getStatus());
        updateEntity.setVersionNo(MdmValueSupport.resolveNextVersionNo(before.getVersionNo()));
        updateEntity.setUpdateBy(resolveOperator(instance));
        updateEntity.setUpdateTime(new Date());
        updateEntity.setCreateBy(null);
        updateEntity.setCreateTime(null);
        updateEntity.setDelFlag(null);
        boolean updated = itemMapper.update(updateEntity, new LambdaUpdateWrapper<MdmItem>()
                .eq(MdmItem::getItemId, itemId)
                .eq(MdmItem::getDelFlag, DEL_FLAG_EXIST)
                .eq(baseVersionNo != null, MdmItem::getVersionNo, baseVersionNo)) > 0;
        if (!updated) {
            throw new IllegalStateException("物料版本已变化，请重新发起审批");
        }
        MdmItem after = loadItem(itemId);
        auditTrailService.record(MdmDomainTypeSupport.ITEM,
                itemId,
                MdmChangeTypeSupport.UPDATE,
                after == null ? updateEntity.getVersionNo() : after.getVersionNo(),
                after == null ? before.getStatus() : after.getStatus(),
                before,
                after);
    }

    private void disableItem(Long itemId, Integer baseVersionNo, SysWorkflowInstance instance) {
        MdmItem before = loadItem(itemId);
        if (before == null) {
            throw new IllegalStateException("物料不存在，无法完成停用回写");
        }
        if (baseVersionNo != null && before.getVersionNo() != null && !baseVersionNo.equals(before.getVersionNo())) {
            throw new IllegalStateException("物料版本已变化，请重新发起审批");
        }
        MdmItem updateEntity = new MdmItem();
        updateEntity.setItemId(itemId);
        updateEntity.setStatus(MdmStatusSupport.DISABLED);
        updateEntity.setVersionNo(MdmValueSupport.resolveNextVersionNo(before.getVersionNo()));
        updateEntity.setUpdateBy(resolveOperator(instance));
        updateEntity.setUpdateTime(new Date());
        boolean updated = itemMapper.update(updateEntity, new LambdaUpdateWrapper<MdmItem>()
                .eq(MdmItem::getItemId, itemId)
                .eq(MdmItem::getDelFlag, DEL_FLAG_EXIST)
                .eq(baseVersionNo != null, MdmItem::getVersionNo, baseVersionNo)) > 0;
        if (!updated) {
            throw new IllegalStateException("物料版本已变化，请重新发起审批");
        }
        MdmItem after = loadItem(itemId);
        auditTrailService.record(MdmDomainTypeSupport.ITEM,
                itemId,
                MdmChangeTypeSupport.STATUS,
                after == null ? updateEntity.getVersionNo() : after.getVersionNo(),
                after == null ? MdmStatusSupport.DISABLED : after.getStatus(),
                before,
                after);
    }

    private MdmItem loadItem(Long itemId) {
        return itemMapper.selectOne(new LambdaQueryWrapper<MdmItem>()
                .eq(MdmItem::getItemId, itemId)
                .eq(MdmItem::getDelFlag, DEL_FLAG_EXIST));
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
            throw new IllegalStateException("解析物料审批回写数据失败", ex);
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

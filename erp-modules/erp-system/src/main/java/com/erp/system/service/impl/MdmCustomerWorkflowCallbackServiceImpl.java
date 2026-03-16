package com.erp.system.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.erp.system.domain.MdmCustomer;
import com.erp.system.domain.SysWorkflowInstance;
import com.erp.system.mapper.MdmCustomerMapper;
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
 * 客户主数据工作流终态回调实现。
 */
@Service
public class MdmCustomerWorkflowCallbackServiceImpl implements IWorkflowBusinessCallback {
    private static final String DEL_FLAG_EXIST = "0";
    private static final String BUSINESS_TYPE = "MDM_CUSTOMER";
    private static final String META_KEY = "__mdmCustomerMeta";

    private final MdmCustomerMapper customerMapper;
    private final IMdmAuditTrailService auditTrailService;
    private final ObjectMapper objectMapper;

    public MdmCustomerWorkflowCallbackServiceImpl(MdmCustomerMapper customerMapper,
                                                  IMdmAuditTrailService auditTrailService) {
        this.customerMapper = customerMapper;
        this.auditTrailService = auditTrailService;
        this.objectMapper = new ObjectMapper();
    }

    /**
     * 当前回调是否支持指定业务类型。
     *
     * @param businessType 业务类型
     * @return true 表示支持
     */
    @Override
    public boolean supports(String businessType) {
        return BUSINESS_TYPE.equalsIgnoreCase(StringUtils.trimWhitespace(businessType));
    }

    /**
     * 处理流程审批完成事件。
     *
     * @param instance 流程实例
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void onWorkflowCompleted(SysWorkflowInstance instance) {
        Map<String, Object> meta = readMeta(instance);
        String action = readString(meta.get("action"));
        Long customerId = readLong(meta.get("customerId"));
        Integer baseVersionNo = readInteger(meta.get("baseVersionNo"));
        if (customerId == null || !StringUtils.hasText(action)) {
            return;
        }
        if (MdmWorkflowActionSupport.ACTIVATE.equalsIgnoreCase(action)) {
            activateCustomer(customerId, baseVersionNo, instance);
            return;
        }
        if (MdmWorkflowActionSupport.UPDATE.equalsIgnoreCase(action)) {
            applyApprovedChange(customerId, baseVersionNo, meta, instance);
            return;
        }
        if (MdmWorkflowActionSupport.DISABLE.equalsIgnoreCase(action)) {
            disableCustomer(customerId, baseVersionNo, instance);
        }
    }

    /**
     * 处理流程驳回事件。
     *
     * @param instance 流程实例
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void onWorkflowRejected(SysWorkflowInstance instance) {
        rollbackSubmittedDraft(instance);
    }

    /**
     * 处理流程撤回事件。
     *
     * @param instance 流程实例
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void onWorkflowWithdrawn(SysWorkflowInstance instance) {
        rollbackSubmittedDraft(instance);
    }

    /**
     * 将审批中的草稿回滚为草稿状态。
     *
     * @param instance 流程实例
     */
    private void rollbackSubmittedDraft(SysWorkflowInstance instance) {
        Map<String, Object> meta = readMeta(instance);
        String action = readString(meta.get("action"));
        Long customerId = readLong(meta.get("customerId"));
        if (customerId == null) {
            return;
        }
        String rollbackStatus = MdmWorkflowActionSupport.ACTIVATE.equalsIgnoreCase(action)
                ? MdmStatusSupport.DRAFT
                : MdmStatusSupport.ACTIVE;
        customerMapper.update(new MdmCustomer(), new LambdaUpdateWrapper<MdmCustomer>()
                .eq(MdmCustomer::getCustomerId, customerId)
                .eq(MdmCustomer::getDelFlag, DEL_FLAG_EXIST)
                .eq(MdmCustomer::getStatus, MdmStatusSupport.SUBMITTED)
                .set(MdmCustomer::getStatus, rollbackStatus)
                .set(MdmCustomer::getUpdateBy, resolveOperator(instance))
                .set(MdmCustomer::getUpdateTime, new Date()));
    }

    /**
     * 审批通过后生效草稿客户。
     *
     * @param customerId     客户ID
     * @param baseVersionNo  基础版本号
     * @param instance       流程实例
     */
    private void activateCustomer(Long customerId, Integer baseVersionNo, SysWorkflowInstance instance) {
        MdmCustomer before = loadCustomer(customerId);
        if (before == null) {
            throw new IllegalStateException("客户不存在，无法完成审批回写");
        }
        if (!MdmStatusSupport.isSubmitted(before.getStatus())) {
            throw new IllegalStateException("客户状态已变化，无法完成审批回写");
        }
        if (baseVersionNo != null && before.getVersionNo() != null && !baseVersionNo.equals(before.getVersionNo())) {
            throw new IllegalStateException("客户版本已变化，无法完成审批回写");
        }
        boolean updated = customerMapper.update(new MdmCustomer(), new LambdaUpdateWrapper<MdmCustomer>()
                .eq(MdmCustomer::getCustomerId, customerId)
                .eq(MdmCustomer::getDelFlag, DEL_FLAG_EXIST)
                .eq(MdmCustomer::getStatus, MdmStatusSupport.SUBMITTED)
                .eq(baseVersionNo != null, MdmCustomer::getVersionNo, baseVersionNo)
                .set(MdmCustomer::getStatus, MdmStatusSupport.ACTIVE)
                .set(before.getEffectiveTime() == null, MdmCustomer::getEffectiveTime, new Date())
                .set(MdmCustomer::getUpdateBy, resolveOperator(instance))
                .set(MdmCustomer::getUpdateTime, new Date())) > 0;
        if (!updated) {
            throw new IllegalStateException("客户状态已变化，无法完成审批回写");
        }
        MdmCustomer after = loadCustomer(customerId);
        auditTrailService.record(MdmDomainTypeSupport.CUSTOMER,
                customerId,
                MdmChangeTypeSupport.STATUS,
                after == null ? before.getVersionNo() : after.getVersionNo(),
                after == null ? MdmStatusSupport.ACTIVE : after.getStatus(),
                before,
                after);
    }

    /**
     * 审批通过后应用客户变更。
     *
     * @param customerId    客户ID
     * @param baseVersionNo 基础版本号
     * @param meta          流程元数据
     * @param instance      流程实例
     */
    private void applyApprovedChange(Long customerId,
                                     Integer baseVersionNo,
                                     Map<String, Object> meta,
                                     SysWorkflowInstance instance) {
        MdmCustomer before = loadCustomer(customerId);
        if (before == null) {
            throw new IllegalStateException("客户不存在，无法完成变更回写");
        }
        if (!MdmStatusSupport.isSubmitted(before.getStatus())) {
            throw new IllegalStateException("客户状态已变化，请重新发起审批");
        }
        if (baseVersionNo != null && before.getVersionNo() != null && !baseVersionNo.equals(before.getVersionNo())) {
            throw new IllegalStateException("客户版本已变化，请重新发起审批");
        }
        MdmCustomer afterCustomer = objectMapper.convertValue(meta.get("afterCustomer"), MdmCustomer.class);
        if (afterCustomer == null) {
            throw new IllegalStateException("审批回写缺少客户变更数据");
        }
        MdmCustomer updateEntity = new MdmCustomer();
        BeanUtils.copyProperties(afterCustomer, updateEntity);
        updateEntity.setCustomerId(customerId);
        updateEntity.setTenantId(before.getTenantId());
        updateEntity.setCustomerCode(before.getCustomerCode());
        updateEntity.setStatus(MdmStatusSupport.ACTIVE);
        updateEntity.setVersionNo(MdmValueSupport.resolveNextVersionNo(before.getVersionNo()));
        updateEntity.setUpdateBy(resolveOperator(instance));
        updateEntity.setUpdateTime(new Date());
        updateEntity.setCreateBy(null);
        updateEntity.setCreateTime(null);
        updateEntity.setDelFlag(null);
        boolean updated = customerMapper.update(updateEntity, new LambdaUpdateWrapper<MdmCustomer>()
                .eq(MdmCustomer::getCustomerId, customerId)
                .eq(MdmCustomer::getDelFlag, DEL_FLAG_EXIST)
                .eq(MdmCustomer::getStatus, MdmStatusSupport.SUBMITTED)
                .eq(baseVersionNo != null, MdmCustomer::getVersionNo, baseVersionNo)) > 0;
        if (!updated) {
            throw new IllegalStateException("客户版本已变化，请重新发起审批");
        }
        MdmCustomer after = loadCustomer(customerId);
        auditTrailService.record(MdmDomainTypeSupport.CUSTOMER,
                customerId,
                MdmChangeTypeSupport.UPDATE,
                after == null ? updateEntity.getVersionNo() : after.getVersionNo(),
                after == null ? before.getStatus() : after.getStatus(),
                before,
                after);
    }

    /**
     * 审批通过后停用客户。
     *
     * @param customerId    客户ID
     * @param baseVersionNo 基础版本号
     * @param instance      流程实例
     */
    private void disableCustomer(Long customerId, Integer baseVersionNo, SysWorkflowInstance instance) {
        MdmCustomer before = loadCustomer(customerId);
        if (before == null) {
            throw new IllegalStateException("客户不存在，无法完成停用回写");
        }
        if (!MdmStatusSupport.isSubmitted(before.getStatus())) {
            throw new IllegalStateException("客户状态已变化，请重新发起审批");
        }
        if (baseVersionNo != null && before.getVersionNo() != null && !baseVersionNo.equals(before.getVersionNo())) {
            throw new IllegalStateException("客户版本已变化，请重新发起审批");
        }
        MdmCustomer updateEntity = new MdmCustomer();
        updateEntity.setCustomerId(customerId);
        updateEntity.setStatus(MdmStatusSupport.DISABLED);
        updateEntity.setVersionNo(MdmValueSupport.resolveNextVersionNo(before.getVersionNo()));
        updateEntity.setUpdateBy(resolveOperator(instance));
        updateEntity.setUpdateTime(new Date());
        boolean updated = customerMapper.update(updateEntity, new LambdaUpdateWrapper<MdmCustomer>()
                .eq(MdmCustomer::getCustomerId, customerId)
                .eq(MdmCustomer::getDelFlag, DEL_FLAG_EXIST)
                .eq(MdmCustomer::getStatus, MdmStatusSupport.SUBMITTED)
                .eq(baseVersionNo != null, MdmCustomer::getVersionNo, baseVersionNo)) > 0;
        if (!updated) {
            throw new IllegalStateException("客户版本已变化，请重新发起审批");
        }
        MdmCustomer after = loadCustomer(customerId);
        auditTrailService.record(MdmDomainTypeSupport.CUSTOMER,
                customerId,
                MdmChangeTypeSupport.STATUS,
                after == null ? updateEntity.getVersionNo() : after.getVersionNo(),
                after == null ? MdmStatusSupport.DISABLED : after.getStatus(),
                before,
                after);
    }

    /**
     * 加载客户当前快照。
     *
     * @param customerId 客户ID
     * @return 客户对象
     */
    private MdmCustomer loadCustomer(Long customerId) {
        return customerMapper.selectOne(new LambdaQueryWrapper<MdmCustomer>()
                .eq(MdmCustomer::getCustomerId, customerId)
                .eq(MdmCustomer::getDelFlag, DEL_FLAG_EXIST));
    }

    /**
     * 解析工作流元数据。
     *
     * @param instance 流程实例
     * @return 元数据
     */
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
            throw new IllegalStateException("解析客户审批回写数据失败", ex);
        }
    }

    /**
     * 读取字符串值。
     *
     * @param value 原始值
     * @return 字符串
     */
    private String readString(Object value) {
        return value == null ? null : String.valueOf(value).trim();
    }

    /**
     * 读取长整型值。
     *
     * @param value 原始值
     * @return 长整型值
     */
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

    /**
     * 读取整型值。
     *
     * @param value 原始值
     * @return 整型值
     */
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

    /**
     * 解析操作人账号。
     *
     * @param instance 流程实例
     * @return 操作人账号
     */
    private String resolveOperator(SysWorkflowInstance instance) {
        if (instance != null && StringUtils.hasText(instance.getLastActionUserName())) {
            return instance.getLastActionUserName().trim();
        }
        return "system";
    }
}

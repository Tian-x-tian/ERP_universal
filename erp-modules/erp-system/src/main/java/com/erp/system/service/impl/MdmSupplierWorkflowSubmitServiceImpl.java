package com.erp.system.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.erp.system.domain.MdmSupplier;
import com.erp.system.domain.SysUser;
import com.erp.system.domain.SysWorkflowInstance;
import com.erp.system.domain.vo.WorkflowStartBody;
import com.erp.system.mapper.SysWorkflowInstanceMapper;
import com.erp.system.security.service.SecurityUserResolver;
import com.erp.system.service.IMdmSupplierService;
import com.erp.system.service.IMdmSupplierWorkflowSubmitService;
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
 * 供应商主数据审批提交流程服务实现。
 */
@Service
public class MdmSupplierWorkflowSubmitServiceImpl implements IMdmSupplierWorkflowSubmitService {
    private static final String DEL_FLAG_EXIST = "0";
    private static final String WORKFLOW_STATUS_RUNNING = "0";
    private static final String BUSINESS_TYPE = "MDM_SUPPLIER";
    private static final String BUSINESS_NO_PREFIX = "MDM:SUPPLIER:";
    private static final String META_KEY = "__mdmSupplierMeta";

    private final IMdmSupplierService supplierService;
    private final ISysWorkflowEngineService workflowEngineService;
    private final SecurityUserResolver securityUserResolver;
    private final ISysUserService userService;
    private final SysWorkflowInstanceMapper workflowInstanceMapper;
    private final ObjectMapper objectMapper;

    public MdmSupplierWorkflowSubmitServiceImpl(IMdmSupplierService supplierService,
                                                ISysWorkflowEngineService workflowEngineService,
                                                SecurityUserResolver securityUserResolver,
                                                ISysUserService userService,
                                                SysWorkflowInstanceMapper workflowInstanceMapper) {
        this.supplierService = supplierService;
        this.workflowEngineService = workflowEngineService;
        this.securityUserResolver = securityUserResolver;
        this.userService = userService;
        this.workflowInstanceMapper = workflowInstanceMapper;
        this.objectMapper = new ObjectMapper();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean submitDraftActivation(Long supplierId, String processKey, String remark) {
        MdmSupplier supplier = loadEditableSupplier(supplierId);
        if (!MdmStatusSupport.isDraft(supplier.getStatus())) {
            throw new IllegalStateException("仅草稿供应商允许提交生效审批");
        }
        if (hasRunningWorkflow(supplierId)) {
            throw new IllegalStateException("该供应商已有审批流程在处理中");
        }
        WorkflowStartBody startBody = buildStartBody(
                processKey,
                remark,
                supplierId,
                MdmWorkflowActionSupport.ACTIVATE,
                supplier.getVersionNo(),
                null,
                supplier);
        if (!startWorkflow(startBody)) {
            throw new IllegalStateException("供应商审批流程发起失败");
        }
        if (!updateSupplierStatus(supplierId, supplier.getVersionNo(), MdmStatusSupport.SUBMITTED)) {
            throw new IllegalStateException("供应商状态已变化，请刷新后重试");
        }
        return true;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean submitChange(Long supplierId, MdmSupplier targetSupplier, String processKey, String remark) {
        MdmSupplier currentSupplier = loadEditableSupplier(supplierId);
        if (MdmStatusSupport.isSubmitted(currentSupplier.getStatus())) {
            throw new IllegalStateException("供应商审批中，暂不允许提交新的变更");
        }
        if (!MdmStatusSupport.isActive(currentSupplier.getStatus())) {
            throw new IllegalStateException("仅已生效供应商允许提交变更审批");
        }
        if (hasRunningWorkflow(supplierId)) {
            throw new IllegalStateException("该供应商已有审批流程在处理中");
        }
        MdmSupplier afterSupplier = normalizeTargetSupplier(currentSupplier, targetSupplier);
        WorkflowStartBody startBody = buildStartBody(
                processKey,
                remark,
                supplierId,
                MdmWorkflowActionSupport.UPDATE,
                currentSupplier.getVersionNo(),
                currentSupplier,
                afterSupplier);
        if (!startWorkflow(startBody)) {
            throw new IllegalStateException("供应商变更审批流程发起失败");
        }
        return true;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean submitDisable(Long supplierId, String processKey, String remark) {
        MdmSupplier currentSupplier = loadEditableSupplier(supplierId);
        if (MdmStatusSupport.isSubmitted(currentSupplier.getStatus())) {
            throw new IllegalStateException("供应商审批中，暂不允许提交停用");
        }
        if (!MdmStatusSupport.isActive(currentSupplier.getStatus())) {
            throw new IllegalStateException("仅已生效供应商允许提交停用审批");
        }
        if (hasRunningWorkflow(supplierId)) {
            throw new IllegalStateException("该供应商已有审批流程在处理中");
        }
        MdmSupplier afterSupplier = new MdmSupplier();
        BeanUtils.copyProperties(currentSupplier, afterSupplier);
        afterSupplier.setStatus(MdmStatusSupport.DISABLED);
        WorkflowStartBody startBody = buildStartBody(
                processKey,
                remark,
                supplierId,
                MdmWorkflowActionSupport.DISABLE,
                currentSupplier.getVersionNo(),
                currentSupplier,
                afterSupplier);
        if (!startWorkflow(startBody)) {
            throw new IllegalStateException("供应商停用审批流程发起失败");
        }
        return true;
    }

    private MdmSupplier loadEditableSupplier(Long supplierId) {
        if (supplierId == null) {
            throw new IllegalArgumentException("供应商ID不能为空");
        }
        MdmSupplier supplier = supplierService.getOne(new LambdaQueryWrapper<MdmSupplier>()
                .eq(MdmSupplier::getSupplierId, supplierId)
                .eq(MdmSupplier::getDelFlag, DEL_FLAG_EXIST));
        if (supplier == null) {
            throw new IllegalArgumentException("供应商不存在");
        }
        return supplier;
    }

    private boolean hasRunningWorkflow(Long supplierId) {
        Long count = workflowInstanceMapper.selectCount(new LambdaQueryWrapper<SysWorkflowInstance>()
                .eq(SysWorkflowInstance::getBusinessType, BUSINESS_TYPE)
                .eq(SysWorkflowInstance::getBusinessNo, buildBusinessNo(supplierId))
                .eq(SysWorkflowInstance::getStatus, WORKFLOW_STATUS_RUNNING));
        return count != null && count > 0;
    }

    private WorkflowStartBody buildStartBody(String processKey,
                                             String remark,
                                             Long supplierId,
                                             String action,
                                             Integer baseVersionNo,
                                             MdmSupplier beforeSupplier,
                                             MdmSupplier afterSupplier) {
        if (!StringUtils.hasText(processKey)) {
            throw new IllegalArgumentException("流程标识不能为空");
        }
        Map<String, Object> formData = new LinkedHashMap<>();
        if (afterSupplier != null) {
            formData.putAll(objectMapper.convertValue(afterSupplier, new TypeReference<Map<String, Object>>() { }));
        }
        Map<String, Object> meta = new LinkedHashMap<>();
        meta.put("action", action);
        meta.put("supplierId", supplierId);
        meta.put("baseVersionNo", baseVersionNo);
        meta.put("beforeSupplier", beforeSupplier);
        meta.put("afterSupplier", afterSupplier);
        formData.put(META_KEY, meta);

        WorkflowStartBody startBody = new WorkflowStartBody();
        startBody.setProcessKey(processKey.trim());
        startBody.setBusinessNo(buildBusinessNo(supplierId));
        startBody.setBusinessType(BUSINESS_TYPE);
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

    private boolean updateSupplierStatus(Long supplierId, Integer currentVersion, String targetStatus) {
        MdmSupplier updateEntity = new MdmSupplier();
        updateEntity.setSupplierId(supplierId);
        updateEntity.setStatus(targetStatus);
        updateEntity.setUpdateBy(resolveOperator());
        updateEntity.setUpdateTime(new Date());
        return supplierService.update(updateEntity, new LambdaUpdateWrapper<MdmSupplier>()
                .eq(MdmSupplier::getSupplierId, supplierId)
                .eq(MdmSupplier::getDelFlag, DEL_FLAG_EXIST)
                .eq(currentVersion != null, MdmSupplier::getVersionNo, currentVersion));
    }

    private MdmSupplier normalizeTargetSupplier(MdmSupplier currentSupplier, MdmSupplier targetSupplier) {
        if (currentSupplier == null || targetSupplier == null) {
            throw new IllegalArgumentException("供应商变更数据不能为空");
        }
        MdmSupplier normalized = new MdmSupplier();
        BeanUtils.copyProperties(currentSupplier, normalized);
        normalized.setSupplierId(currentSupplier.getSupplierId());
        normalized.setTenantId(currentSupplier.getTenantId());
        normalized.setSupplierCode(currentSupplier.getSupplierCode());
        normalized.setSupplierName(trimRequired(targetSupplier.getSupplierName(), "供应商名称不能为空"));
        normalized.setShortName(MdmValueSupport.trimToNull(targetSupplier.getShortName()));
        normalized.setSupplyCategory(MdmValueSupport.trimToNull(targetSupplier.getSupplyCategory()));
        normalized.setTaxNo(MdmValueSupport.trimToNull(targetSupplier.getTaxNo()));
        if (StringUtils.hasText(normalized.getTaxNo()) && !MdmValueSupport.isValidTaxNo(normalized.getTaxNo())) {
            throw new IllegalArgumentException("税号格式不正确");
        }
        normalized.setDefaultCurrency(MdmValueSupport.trimToNull(targetSupplier.getDefaultCurrency()));
        normalized.setDefaultTaxRate(targetSupplier.getDefaultTaxRate());
        normalized.setLeadTimeDays(targetSupplier.getLeadTimeDays());
        normalized.setQualityLevel(MdmValueSupport.trimToNull(targetSupplier.getQualityLevel()));
        normalized.setBankAccountInfo(MdmValueSupport.trimToNull(targetSupplier.getBankAccountInfo()));
        normalized.setContactName(MdmValueSupport.trimToNull(targetSupplier.getContactName()));
        normalized.setContactPhone(MdmValueSupport.trimToNull(targetSupplier.getContactPhone()));
        normalized.setContactEmail(MdmValueSupport.trimToNull(targetSupplier.getContactEmail()));
        normalized.setAddress(MdmValueSupport.trimToNull(targetSupplier.getAddress()));
        normalized.setRemark(MdmValueSupport.trimToNull(targetSupplier.getRemark()));
        normalized.setStatus(currentSupplier.getStatus());
        normalized.setVersionNo(currentSupplier.getVersionNo());
        return normalized;
    }

    private String buildBusinessNo(Long supplierId) {
        return BUSINESS_NO_PREFIX + supplierId;
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

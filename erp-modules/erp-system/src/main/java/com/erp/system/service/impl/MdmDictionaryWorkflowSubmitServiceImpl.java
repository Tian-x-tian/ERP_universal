package com.erp.system.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.erp.system.domain.MdmCurrency;
import com.erp.system.domain.MdmSettleMethod;
import com.erp.system.domain.MdmTaxRate;
import com.erp.system.domain.MdmUom;
import com.erp.system.domain.SysUser;
import com.erp.system.domain.SysWorkflowInstance;
import com.erp.system.domain.vo.WorkflowStartBody;
import com.erp.system.mapper.SysWorkflowInstanceMapper;
import com.erp.system.security.service.SecurityUserResolver;
import com.erp.system.service.IMdmCurrencyService;
import com.erp.system.service.IMdmDictionaryWorkflowSubmitService;
import com.erp.system.service.IMdmSettleMethodService;
import com.erp.system.service.IMdmTaxRateService;
import com.erp.system.service.IMdmUomService;
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
import java.util.Locale;
import java.util.Map;

/**
 * 字典主数据审批提交流程服务实现。
 */
@Service
public class MdmDictionaryWorkflowSubmitServiceImpl implements IMdmDictionaryWorkflowSubmitService {
    private static final String DEL_FLAG_EXIST = "0";
    private static final String WORKFLOW_STATUS_RUNNING = "0";
    private static final String BUSINESS_TYPE_SETTLE = "MDM_SETTLE_METHOD";
    private static final String BUSINESS_TYPE_TAX = "MDM_TAX_RATE";
    private static final String BUSINESS_TYPE_CURRENCY = "MDM_CURRENCY";
    private static final String BUSINESS_TYPE_UOM = "MDM_UOM";
    private static final String PREFIX_SETTLE = "MDM:SETTLE:";
    private static final String PREFIX_TAX = "MDM:TAX:";
    private static final String PREFIX_CURRENCY = "MDM:CURRENCY:";
    private static final String PREFIX_UOM = "MDM:UOM:";
    private static final String META_KEY_SETTLE = "__mdmSettleMeta";
    private static final String META_KEY_TAX = "__mdmTaxMeta";
    private static final String META_KEY_CURRENCY = "__mdmCurrencyMeta";
    private static final String META_KEY_UOM = "__mdmUomMeta";

    private final IMdmSettleMethodService settleMethodService;
    private final IMdmTaxRateService taxRateService;
    private final IMdmCurrencyService currencyService;
    private final IMdmUomService uomService;
    private final ISysWorkflowEngineService workflowEngineService;
    private final SecurityUserResolver securityUserResolver;
    private final ISysUserService userService;
    private final SysWorkflowInstanceMapper workflowInstanceMapper;
    private final ObjectMapper objectMapper;

    public MdmDictionaryWorkflowSubmitServiceImpl(IMdmSettleMethodService settleMethodService,
            IMdmTaxRateService taxRateService,
            IMdmCurrencyService currencyService,
            IMdmUomService uomService,
            ISysWorkflowEngineService workflowEngineService,
            SecurityUserResolver securityUserResolver,
            ISysUserService userService,
            SysWorkflowInstanceMapper workflowInstanceMapper) {
        this.settleMethodService = settleMethodService;
        this.taxRateService = taxRateService;
        this.currencyService = currencyService;
        this.uomService = uomService;
        this.workflowEngineService = workflowEngineService;
        this.securityUserResolver = securityUserResolver;
        this.userService = userService;
        this.workflowInstanceMapper = workflowInstanceMapper;
        this.objectMapper = new ObjectMapper();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean submitSettleDraftActivation(Long settleMethodId, String processKey, String remark) {
        MdmSettleMethod settleMethod = loadSettleMethod(settleMethodId);
        ensureDraft(settleMethod.getStatus(), "仅草稿结算方式允许提交生效审批");
        ensureNoRunningWorkflow(BUSINESS_TYPE_SETTLE, PREFIX_SETTLE + settleMethodId, "该结算方式已有审批流程在处理中");
        if (!startWorkflow(buildStartBody(processKey, remark, PREFIX_SETTLE + settleMethodId, BUSINESS_TYPE_SETTLE, META_KEY_SETTLE, buildMeta("action", MdmWorkflowActionSupport.ACTIVATE, "settleMethodId", settleMethodId, "baseVersionNo", settleMethod.getVersionNo(), "beforeSettleMethod", null, "afterSettleMethod", settleMethod), settleMethod))) {
            throw new IllegalStateException("结算方式审批流程发起失败");
        }
        return updateSettleStatus(settleMethodId, settleMethod.getVersionNo(), MdmStatusSupport.SUBMITTED);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean submitSettleChange(Long settleMethodId, MdmSettleMethod targetSettleMethod, String processKey, String remark) {
        MdmSettleMethod current = loadSettleMethod(settleMethodId);
        ensureActiveForFlow(current.getStatus(), "结算方式审批中，暂不允许提交新的变更", "仅已生效结算方式允许提交变更审批");
        ensureNoRunningWorkflow(BUSINESS_TYPE_SETTLE, PREFIX_SETTLE + settleMethodId, "该结算方式已有审批流程在处理中");
        MdmSettleMethod after = normalizeSettle(current, targetSettleMethod);
        if (!startWorkflow(buildStartBody(processKey, remark, PREFIX_SETTLE + settleMethodId, BUSINESS_TYPE_SETTLE, META_KEY_SETTLE, buildMeta("action", MdmWorkflowActionSupport.UPDATE, "settleMethodId", settleMethodId, "baseVersionNo", current.getVersionNo(), "beforeSettleMethod", current, "afterSettleMethod", after), after))) {
            throw new IllegalStateException("结算方式变更审批流程发起失败");
        }
        if (!updateSettleStatus(settleMethodId, current.getVersionNo(), MdmStatusSupport.SUBMITTED)) {
            throw new IllegalStateException("结算方式状态已变化，请刷新后重试");
        }
        return true;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean submitSettleDisable(Long settleMethodId, String processKey, String remark) {
        MdmSettleMethod current = loadSettleMethod(settleMethodId);
        ensureActiveForFlow(current.getStatus(), "结算方式审批中，暂不允许提交停用", "仅已生效结算方式允许提交停用审批");
        ensureNoRunningWorkflow(BUSINESS_TYPE_SETTLE, PREFIX_SETTLE + settleMethodId, "该结算方式已有审批流程在处理中");
        MdmSettleMethod after = new MdmSettleMethod();
        BeanUtils.copyProperties(current, after);
        after.setStatus(MdmStatusSupport.DISABLED);
        if (!startWorkflow(buildStartBody(processKey, remark, PREFIX_SETTLE + settleMethodId, BUSINESS_TYPE_SETTLE, META_KEY_SETTLE, buildMeta("action", MdmWorkflowActionSupport.DISABLE, "settleMethodId", settleMethodId, "baseVersionNo", current.getVersionNo(), "beforeSettleMethod", current, "afterSettleMethod", after), after))) {
            throw new IllegalStateException("结算方式停用审批流程发起失败");
        }
        if (!updateSettleStatus(settleMethodId, current.getVersionNo(), MdmStatusSupport.SUBMITTED)) {
            throw new IllegalStateException("结算方式状态已变化，请刷新后重试");
        }
        return true;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean submitTaxDraftActivation(Long taxRateId, String processKey, String remark) {
        MdmTaxRate taxRate = loadTaxRate(taxRateId);
        ensureDraft(taxRate.getStatus(), "仅草稿税率允许提交生效审批");
        ensureNoRunningWorkflow(BUSINESS_TYPE_TAX, PREFIX_TAX + taxRateId, "该税率已有审批流程在处理中");
        if (!startWorkflow(buildStartBody(processKey, remark, PREFIX_TAX + taxRateId, BUSINESS_TYPE_TAX, META_KEY_TAX, buildMeta("action", MdmWorkflowActionSupport.ACTIVATE, "taxRateId", taxRateId, "baseVersionNo", taxRate.getVersionNo(), "beforeTaxRate", null, "afterTaxRate", taxRate), taxRate))) {
            throw new IllegalStateException("税率审批流程发起失败");
        }
        return updateTaxStatus(taxRateId, taxRate.getVersionNo(), MdmStatusSupport.SUBMITTED);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean submitTaxChange(Long taxRateId, MdmTaxRate targetTaxRate, String processKey, String remark) {
        MdmTaxRate current = loadTaxRate(taxRateId);
        ensureActiveForFlow(current.getStatus(), "税率审批中，暂不允许提交新的变更", "仅已生效税率允许提交变更审批");
        ensureNoRunningWorkflow(BUSINESS_TYPE_TAX, PREFIX_TAX + taxRateId, "该税率已有审批流程在处理中");
        MdmTaxRate after = normalizeTax(current, targetTaxRate);
        if (!startWorkflow(buildStartBody(processKey, remark, PREFIX_TAX + taxRateId, BUSINESS_TYPE_TAX, META_KEY_TAX, buildMeta("action", MdmWorkflowActionSupport.UPDATE, "taxRateId", taxRateId, "baseVersionNo", current.getVersionNo(), "beforeTaxRate", current, "afterTaxRate", after), after))) {
            throw new IllegalStateException("税率变更审批流程发起失败");
        }
        if (!updateTaxStatus(taxRateId, current.getVersionNo(), MdmStatusSupport.SUBMITTED)) {
            throw new IllegalStateException("税率状态已变化，请刷新后重试");
        }
        return true;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean submitTaxDisable(Long taxRateId, String processKey, String remark) {
        MdmTaxRate current = loadTaxRate(taxRateId);
        ensureActiveForFlow(current.getStatus(), "税率审批中，暂不允许提交停用", "仅已生效税率允许提交停用审批");
        ensureNoRunningWorkflow(BUSINESS_TYPE_TAX, PREFIX_TAX + taxRateId, "该税率已有审批流程在处理中");
        MdmTaxRate after = new MdmTaxRate();
        BeanUtils.copyProperties(current, after);
        after.setStatus(MdmStatusSupport.DISABLED);
        if (!startWorkflow(buildStartBody(processKey, remark, PREFIX_TAX + taxRateId, BUSINESS_TYPE_TAX, META_KEY_TAX, buildMeta("action", MdmWorkflowActionSupport.DISABLE, "taxRateId", taxRateId, "baseVersionNo", current.getVersionNo(), "beforeTaxRate", current, "afterTaxRate", after), after))) {
            throw new IllegalStateException("税率停用审批流程发起失败");
        }
        if (!updateTaxStatus(taxRateId, current.getVersionNo(), MdmStatusSupport.SUBMITTED)) {
            throw new IllegalStateException("税率状态已变化，请刷新后重试");
        }
        return true;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean submitCurrencyDraftActivation(Long currencyId, String processKey, String remark) {
        MdmCurrency currency = loadCurrency(currencyId);
        ensureDraft(currency.getStatus(), "仅草稿币种允许提交生效审批");
        ensureNoRunningWorkflow(BUSINESS_TYPE_CURRENCY, PREFIX_CURRENCY + currencyId, "该币种已有审批流程在处理中");
        if (!startWorkflow(buildStartBody(processKey, remark, PREFIX_CURRENCY + currencyId, BUSINESS_TYPE_CURRENCY, META_KEY_CURRENCY, buildMeta("action", MdmWorkflowActionSupport.ACTIVATE, "currencyId", currencyId, "baseVersionNo", currency.getVersionNo(), "beforeCurrency", null, "afterCurrency", currency), currency))) {
            throw new IllegalStateException("币种审批流程发起失败");
        }
        return updateCurrencyStatus(currencyId, currency.getVersionNo(), MdmStatusSupport.SUBMITTED);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean submitCurrencyChange(Long currencyId, MdmCurrency targetCurrency, String processKey, String remark) {
        MdmCurrency current = loadCurrency(currencyId);
        ensureActiveForFlow(current.getStatus(), "币种审批中，暂不允许提交新的变更", "仅已生效币种允许提交变更审批");
        ensureNoRunningWorkflow(BUSINESS_TYPE_CURRENCY, PREFIX_CURRENCY + currencyId, "该币种已有审批流程在处理中");
        MdmCurrency after = normalizeCurrency(current, targetCurrency);
        if (!startWorkflow(buildStartBody(processKey, remark, PREFIX_CURRENCY + currencyId, BUSINESS_TYPE_CURRENCY, META_KEY_CURRENCY, buildMeta("action", MdmWorkflowActionSupport.UPDATE, "currencyId", currencyId, "baseVersionNo", current.getVersionNo(), "beforeCurrency", current, "afterCurrency", after), after))) {
            throw new IllegalStateException("币种变更审批流程发起失败");
        }
        if (!updateCurrencyStatus(currencyId, current.getVersionNo(), MdmStatusSupport.SUBMITTED)) {
            throw new IllegalStateException("币种状态已变化，请刷新后重试");
        }
        return true;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean submitCurrencyDisable(Long currencyId, String processKey, String remark) {
        MdmCurrency current = loadCurrency(currencyId);
        ensureActiveForFlow(current.getStatus(), "币种审批中，暂不允许提交停用", "仅已生效币种允许提交停用审批");
        ensureNoRunningWorkflow(BUSINESS_TYPE_CURRENCY, PREFIX_CURRENCY + currencyId, "该币种已有审批流程在处理中");
        MdmCurrency after = new MdmCurrency();
        BeanUtils.copyProperties(current, after);
        after.setStatus(MdmStatusSupport.DISABLED);
        if (!startWorkflow(buildStartBody(processKey, remark, PREFIX_CURRENCY + currencyId, BUSINESS_TYPE_CURRENCY, META_KEY_CURRENCY, buildMeta("action", MdmWorkflowActionSupport.DISABLE, "currencyId", currencyId, "baseVersionNo", current.getVersionNo(), "beforeCurrency", current, "afterCurrency", after), after))) {
            throw new IllegalStateException("币种停用审批流程发起失败");
        }
        if (!updateCurrencyStatus(currencyId, current.getVersionNo(), MdmStatusSupport.SUBMITTED)) {
            throw new IllegalStateException("币种状态已变化，请刷新后重试");
        }
        return true;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean submitUomDraftActivation(Long uomId, String processKey, String remark) {
        MdmUom uom = loadUom(uomId);
        ensureDraft(uom.getStatus(), "仅草稿计量单位允许提交生效审批");
        ensureNoRunningWorkflow(BUSINESS_TYPE_UOM, PREFIX_UOM + uomId, "该计量单位已有审批流程在处理中");
        if (!startWorkflow(buildStartBody(processKey, remark, PREFIX_UOM + uomId, BUSINESS_TYPE_UOM, META_KEY_UOM, buildMeta("action", MdmWorkflowActionSupport.ACTIVATE, "uomId", uomId, "baseVersionNo", uom.getVersionNo(), "beforeUom", null, "afterUom", uom), uom))) {
            throw new IllegalStateException("计量单位审批流程发起失败");
        }
        return updateUomStatus(uomId, uom.getVersionNo(), MdmStatusSupport.SUBMITTED);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean submitUomChange(Long uomId, MdmUom targetUom, String processKey, String remark) {
        MdmUom current = loadUom(uomId);
        ensureActiveForFlow(current.getStatus(), "计量单位审批中，暂不允许提交新的变更", "仅已生效计量单位允许提交变更审批");
        ensureNoRunningWorkflow(BUSINESS_TYPE_UOM, PREFIX_UOM + uomId, "该计量单位已有审批流程在处理中");
        MdmUom after = normalizeUom(current, targetUom);
        if (!startWorkflow(buildStartBody(processKey, remark, PREFIX_UOM + uomId, BUSINESS_TYPE_UOM, META_KEY_UOM, buildMeta("action", MdmWorkflowActionSupport.UPDATE, "uomId", uomId, "baseVersionNo", current.getVersionNo(), "beforeUom", current, "afterUom", after), after))) {
            throw new IllegalStateException("计量单位变更审批流程发起失败");
        }
        if (!updateUomStatus(uomId, current.getVersionNo(), MdmStatusSupport.SUBMITTED)) {
            throw new IllegalStateException("计量单位状态已变化，请刷新后重试");
        }
        return true;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean submitUomDisable(Long uomId, String processKey, String remark) {
        MdmUom current = loadUom(uomId);
        ensureActiveForFlow(current.getStatus(), "计量单位审批中，暂不允许提交停用", "仅已生效计量单位允许提交停用审批");
        ensureNoRunningWorkflow(BUSINESS_TYPE_UOM, PREFIX_UOM + uomId, "该计量单位已有审批流程在处理中");
        MdmUom after = new MdmUom();
        BeanUtils.copyProperties(current, after);
        after.setStatus(MdmStatusSupport.DISABLED);
        if (!startWorkflow(buildStartBody(processKey, remark, PREFIX_UOM + uomId, BUSINESS_TYPE_UOM, META_KEY_UOM, buildMeta("action", MdmWorkflowActionSupport.DISABLE, "uomId", uomId, "baseVersionNo", current.getVersionNo(), "beforeUom", current, "afterUom", after), after))) {
            throw new IllegalStateException("计量单位停用审批流程发起失败");
        }
        if (!updateUomStatus(uomId, current.getVersionNo(), MdmStatusSupport.SUBMITTED)) {
            throw new IllegalStateException("计量单位状态已变化，请刷新后重试");
        }
        return true;
    }

    private MdmSettleMethod loadSettleMethod(Long settleMethodId) {
        if (settleMethodId == null) {
            throw new IllegalArgumentException("结算方式ID不能为空");
        }
        MdmSettleMethod entity = settleMethodService.getOne(new LambdaQueryWrapper<MdmSettleMethod>().eq(MdmSettleMethod::getSettleMethodId, settleMethodId).eq(MdmSettleMethod::getDelFlag, DEL_FLAG_EXIST));
        if (entity == null) {
            throw new IllegalArgumentException("结算方式不存在");
        }
        return entity;
    }

    private MdmTaxRate loadTaxRate(Long taxRateId) {
        if (taxRateId == null) {
            throw new IllegalArgumentException("税率ID不能为空");
        }
        MdmTaxRate entity = taxRateService.getOne(new LambdaQueryWrapper<MdmTaxRate>().eq(MdmTaxRate::getTaxRateId, taxRateId).eq(MdmTaxRate::getDelFlag, DEL_FLAG_EXIST));
        if (entity == null) {
            throw new IllegalArgumentException("税率不存在");
        }
        return entity;
    }

    private MdmCurrency loadCurrency(Long currencyId) {
        if (currencyId == null) {
            throw new IllegalArgumentException("币种ID不能为空");
        }
        MdmCurrency entity = currencyService.getOne(new LambdaQueryWrapper<MdmCurrency>().eq(MdmCurrency::getCurrencyId, currencyId).eq(MdmCurrency::getDelFlag, DEL_FLAG_EXIST));
        if (entity == null) {
            throw new IllegalArgumentException("币种不存在");
        }
        return entity;
    }

    private MdmUom loadUom(Long uomId) {
        if (uomId == null) {
            throw new IllegalArgumentException("计量单位ID不能为空");
        }
        MdmUom entity = uomService.getOne(new LambdaQueryWrapper<MdmUom>().eq(MdmUom::getUomId, uomId).eq(MdmUom::getDelFlag, DEL_FLAG_EXIST));
        if (entity == null) {
            throw new IllegalArgumentException("计量单位不存在");
        }
        return entity;
    }

    private void ensureDraft(String status, String message) {
        if (!MdmStatusSupport.isDraft(status)) {
            throw new IllegalStateException(message);
        }
    }

    private void ensureActiveForFlow(String status, String submittedMessage, String activeMessage) {
        if (MdmStatusSupport.isSubmitted(status)) {
            throw new IllegalStateException(submittedMessage);
        }
        if (!MdmStatusSupport.isActive(status)) {
            throw new IllegalStateException(activeMessage);
        }
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

    private WorkflowStartBody buildStartBody(String processKey, String remark, String businessNo, String businessType, String metaKey, Map<String, Object> meta, Object afterValue) {
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

    private boolean updateSettleStatus(Long settleMethodId, Integer currentVersion, String targetStatus) {
        MdmSettleMethod updateEntity = new MdmSettleMethod();
        updateEntity.setSettleMethodId(settleMethodId);
        updateEntity.setStatus(targetStatus);
        updateEntity.setUpdateBy(resolveOperator());
        updateEntity.setUpdateTime(new Date());
        return settleMethodService.update(updateEntity, new LambdaUpdateWrapper<MdmSettleMethod>().eq(MdmSettleMethod::getSettleMethodId, settleMethodId).eq(MdmSettleMethod::getDelFlag, DEL_FLAG_EXIST).eq(currentVersion != null, MdmSettleMethod::getVersionNo, currentVersion));
    }

    private boolean updateTaxStatus(Long taxRateId, Integer currentVersion, String targetStatus) {
        MdmTaxRate updateEntity = new MdmTaxRate();
        updateEntity.setTaxRateId(taxRateId);
        updateEntity.setStatus(targetStatus);
        updateEntity.setUpdateBy(resolveOperator());
        updateEntity.setUpdateTime(new Date());
        return taxRateService.update(updateEntity, new LambdaUpdateWrapper<MdmTaxRate>().eq(MdmTaxRate::getTaxRateId, taxRateId).eq(MdmTaxRate::getDelFlag, DEL_FLAG_EXIST).eq(currentVersion != null, MdmTaxRate::getVersionNo, currentVersion));
    }

    private boolean updateCurrencyStatus(Long currencyId, Integer currentVersion, String targetStatus) {
        MdmCurrency updateEntity = new MdmCurrency();
        updateEntity.setCurrencyId(currencyId);
        updateEntity.setStatus(targetStatus);
        updateEntity.setUpdateBy(resolveOperator());
        updateEntity.setUpdateTime(new Date());
        return currencyService.update(updateEntity, new LambdaUpdateWrapper<MdmCurrency>().eq(MdmCurrency::getCurrencyId, currencyId).eq(MdmCurrency::getDelFlag, DEL_FLAG_EXIST).eq(currentVersion != null, MdmCurrency::getVersionNo, currentVersion));
    }

    private boolean updateUomStatus(Long uomId, Integer currentVersion, String targetStatus) {
        MdmUom updateEntity = new MdmUom();
        updateEntity.setUomId(uomId);
        updateEntity.setStatus(targetStatus);
        updateEntity.setUpdateBy(resolveOperator());
        updateEntity.setUpdateTime(new Date());
        return uomService.update(updateEntity, new LambdaUpdateWrapper<MdmUom>().eq(MdmUom::getUomId, uomId).eq(MdmUom::getDelFlag, DEL_FLAG_EXIST).eq(currentVersion != null, MdmUom::getVersionNo, currentVersion));
    }

    private Map<String, Object> buildMeta(Object... args) {
        Map<String, Object> meta = new LinkedHashMap<>();
        for (int i = 0; i < args.length; i += 2) {
            meta.put(String.valueOf(args[i]), args[i + 1]);
        }
        return meta;
    }

    private MdmSettleMethod normalizeSettle(MdmSettleMethod current, MdmSettleMethod target) {
        if (current == null || target == null) {
            throw new IllegalArgumentException("结算方式变更数据不能为空");
        }
        MdmSettleMethod normalized = new MdmSettleMethod();
        BeanUtils.copyProperties(current, normalized);
        normalized.setSettleMethodId(current.getSettleMethodId());
        normalized.setTenantId(current.getTenantId());
        normalized.setSettleCode(current.getSettleCode());
        normalized.setSettleName(trimRequired(target.getSettleName(), "结算方式名称不能为空"));
        normalized.setRemark(MdmValueSupport.trimToNull(target.getRemark()));
        normalized.setStatus(current.getStatus());
        normalized.setVersionNo(current.getVersionNo());
        return normalized;
    }

    private MdmTaxRate normalizeTax(MdmTaxRate current, MdmTaxRate target) {
        if (current == null || target == null) {
            throw new IllegalArgumentException("税率变更数据不能为空");
        }
        MdmTaxRate normalized = new MdmTaxRate();
        BeanUtils.copyProperties(current, normalized);
        normalized.setTaxRateId(current.getTaxRateId());
        normalized.setTenantId(current.getTenantId());
        normalized.setTaxCode(current.getTaxCode());
        normalized.setTaxName(trimRequired(target.getTaxName(), "税率名称不能为空"));
        normalized.setTaxRate(target.getTaxRate());
        normalized.setEffectiveFrom(target.getEffectiveFrom());
        normalized.setEffectiveTo(target.getEffectiveTo());
        normalized.setRemark(MdmValueSupport.trimToNull(target.getRemark()));
        normalized.setStatus(current.getStatus());
        normalized.setVersionNo(current.getVersionNo());
        return normalized;
    }

    private MdmCurrency normalizeCurrency(MdmCurrency current, MdmCurrency target) {
        if (current == null || target == null) {
            throw new IllegalArgumentException("币种变更数据不能为空");
        }
        MdmCurrency normalized = new MdmCurrency();
        BeanUtils.copyProperties(current, normalized);
        normalized.setCurrencyId(current.getCurrencyId());
        normalized.setTenantId(current.getTenantId());
        normalized.setCurrencyCode(current.getCurrencyCode());
        normalized.setCurrencyName(trimRequired(target.getCurrencyName(), "币种名称不能为空"));
        normalized.setSymbol(MdmValueSupport.trimToNull(target.getSymbol()));
        normalized.setPrecisionScale(target.getPrecisionScale());
        normalized.setEffectiveFrom(target.getEffectiveFrom());
        normalized.setEffectiveTo(target.getEffectiveTo());
        normalized.setRemark(MdmValueSupport.trimToNull(target.getRemark()));
        normalized.setStatus(current.getStatus());
        normalized.setVersionNo(current.getVersionNo());
        return normalized;
    }

    private MdmUom normalizeUom(MdmUom current, MdmUom target) {
        if (current == null || target == null) {
            throw new IllegalArgumentException("计量单位变更数据不能为空");
        }
        MdmUom normalized = new MdmUom();
        BeanUtils.copyProperties(current, normalized);
        normalized.setUomId(current.getUomId());
        normalized.setTenantId(current.getTenantId());
        normalized.setUomCode(current.getUomCode());
        normalized.setUomName(trimRequired(target.getUomName(), "计量单位名称不能为空"));
        normalized.setBaseUomCode(normalizeCode(target.getBaseUomCode()));
        normalized.setConvertRate(target.getConvertRate());
        normalized.setRemark(MdmValueSupport.trimToNull(target.getRemark()));
        normalized.setStatus(current.getStatus());
        normalized.setVersionNo(current.getVersionNo());
        return normalized;
    }

    private String normalizeCode(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        return value.trim().toUpperCase(Locale.ROOT);
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

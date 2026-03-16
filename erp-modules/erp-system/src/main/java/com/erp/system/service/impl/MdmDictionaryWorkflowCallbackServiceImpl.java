package com.erp.system.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.erp.system.domain.MdmCurrency;
import com.erp.system.domain.MdmSettleMethod;
import com.erp.system.domain.MdmTaxRate;
import com.erp.system.domain.MdmUom;
import com.erp.system.domain.SysWorkflowInstance;
import com.erp.system.mapper.MdmCurrencyMapper;
import com.erp.system.mapper.MdmSettleMethodMapper;
import com.erp.system.mapper.MdmTaxRateMapper;
import com.erp.system.mapper.MdmUomMapper;
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
 * 字典主数据工作流终态回调实现。
 */
@Service
public class MdmDictionaryWorkflowCallbackServiceImpl implements IWorkflowBusinessCallback {
    private static final String DEL_FLAG_EXIST = "0";
    private static final String BUSINESS_TYPE_SETTLE = "MDM_SETTLE_METHOD";
    private static final String BUSINESS_TYPE_TAX = "MDM_TAX_RATE";
    private static final String BUSINESS_TYPE_CURRENCY = "MDM_CURRENCY";
    private static final String BUSINESS_TYPE_UOM = "MDM_UOM";
    private static final String META_KEY_SETTLE = "__mdmSettleMeta";
    private static final String META_KEY_TAX = "__mdmTaxMeta";
    private static final String META_KEY_CURRENCY = "__mdmCurrencyMeta";
    private static final String META_KEY_UOM = "__mdmUomMeta";

    private final MdmSettleMethodMapper settleMethodMapper;
    private final MdmTaxRateMapper taxRateMapper;
    private final MdmCurrencyMapper currencyMapper;
    private final MdmUomMapper uomMapper;
    private final IMdmAuditTrailService auditTrailService;
    private final ObjectMapper objectMapper;

    public MdmDictionaryWorkflowCallbackServiceImpl(MdmSettleMethodMapper settleMethodMapper,
            MdmTaxRateMapper taxRateMapper,
            MdmCurrencyMapper currencyMapper,
            MdmUomMapper uomMapper,
            IMdmAuditTrailService auditTrailService) {
        this.settleMethodMapper = settleMethodMapper;
        this.taxRateMapper = taxRateMapper;
        this.currencyMapper = currencyMapper;
        this.uomMapper = uomMapper;
        this.auditTrailService = auditTrailService;
        this.objectMapper = new ObjectMapper();
    }

    @Override
    public boolean supports(String businessType) {
        String normalized = StringUtils.trimWhitespace(businessType);
        return BUSINESS_TYPE_SETTLE.equalsIgnoreCase(normalized)
                || BUSINESS_TYPE_TAX.equalsIgnoreCase(normalized)
                || BUSINESS_TYPE_CURRENCY.equalsIgnoreCase(normalized)
                || BUSINESS_TYPE_UOM.equalsIgnoreCase(normalized);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void onWorkflowCompleted(SysWorkflowInstance instance) {
        String businessType = StringUtils.trimWhitespace(instance == null ? null : instance.getBusinessType());
        if (BUSINESS_TYPE_SETTLE.equalsIgnoreCase(businessType)) {
            handleSettleCompleted(instance);
            return;
        }
        if (BUSINESS_TYPE_TAX.equalsIgnoreCase(businessType)) {
            handleTaxCompleted(instance);
            return;
        }
        if (BUSINESS_TYPE_CURRENCY.equalsIgnoreCase(businessType)) {
            handleCurrencyCompleted(instance);
            return;
        }
        if (BUSINESS_TYPE_UOM.equalsIgnoreCase(businessType)) {
            handleUomCompleted(instance);
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

    private void handleSettleCompleted(SysWorkflowInstance instance) {
        Map<String, Object> meta = readMeta(instance, META_KEY_SETTLE);
        String action = readString(meta.get("action"));
        Long id = readLong(meta.get("settleMethodId"));
        Integer baseVersionNo = readInteger(meta.get("baseVersionNo"));
        if (id == null || !StringUtils.hasText(action)) {
            return;
        }
        if (MdmWorkflowActionSupport.ACTIVATE.equalsIgnoreCase(action)) {
            activateSettle(id, baseVersionNo, instance);
        } else if (MdmWorkflowActionSupport.UPDATE.equalsIgnoreCase(action)) {
            applySettleChange(id, baseVersionNo, meta, instance);
        } else if (MdmWorkflowActionSupport.DISABLE.equalsIgnoreCase(action)) {
            disableSettle(id, baseVersionNo, instance);
        }
    }

    private void handleTaxCompleted(SysWorkflowInstance instance) {
        Map<String, Object> meta = readMeta(instance, META_KEY_TAX);
        String action = readString(meta.get("action"));
        Long id = readLong(meta.get("taxRateId"));
        Integer baseVersionNo = readInteger(meta.get("baseVersionNo"));
        if (id == null || !StringUtils.hasText(action)) {
            return;
        }
        if (MdmWorkflowActionSupport.ACTIVATE.equalsIgnoreCase(action)) {
            activateTax(id, baseVersionNo, instance);
        } else if (MdmWorkflowActionSupport.UPDATE.equalsIgnoreCase(action)) {
            applyTaxChange(id, baseVersionNo, meta, instance);
        } else if (MdmWorkflowActionSupport.DISABLE.equalsIgnoreCase(action)) {
            disableTax(id, baseVersionNo, instance);
        }
    }

    private void handleCurrencyCompleted(SysWorkflowInstance instance) {
        Map<String, Object> meta = readMeta(instance, META_KEY_CURRENCY);
        String action = readString(meta.get("action"));
        Long id = readLong(meta.get("currencyId"));
        Integer baseVersionNo = readInteger(meta.get("baseVersionNo"));
        if (id == null || !StringUtils.hasText(action)) {
            return;
        }
        if (MdmWorkflowActionSupport.ACTIVATE.equalsIgnoreCase(action)) {
            activateCurrency(id, baseVersionNo, instance);
        } else if (MdmWorkflowActionSupport.UPDATE.equalsIgnoreCase(action)) {
            applyCurrencyChange(id, baseVersionNo, meta, instance);
        } else if (MdmWorkflowActionSupport.DISABLE.equalsIgnoreCase(action)) {
            disableCurrency(id, baseVersionNo, instance);
        }
    }

    private void handleUomCompleted(SysWorkflowInstance instance) {
        Map<String, Object> meta = readMeta(instance, META_KEY_UOM);
        String action = readString(meta.get("action"));
        Long id = readLong(meta.get("uomId"));
        Integer baseVersionNo = readInteger(meta.get("baseVersionNo"));
        if (id == null || !StringUtils.hasText(action)) {
            return;
        }
        if (MdmWorkflowActionSupport.ACTIVATE.equalsIgnoreCase(action)) {
            activateUom(id, baseVersionNo, instance);
        } else if (MdmWorkflowActionSupport.UPDATE.equalsIgnoreCase(action)) {
            applyUomChange(id, baseVersionNo, meta, instance);
        } else if (MdmWorkflowActionSupport.DISABLE.equalsIgnoreCase(action)) {
            disableUom(id, baseVersionNo, instance);
        }
    }

    private void rollbackSubmittedDraft(SysWorkflowInstance instance) {
        String businessType = StringUtils.trimWhitespace(instance == null ? null : instance.getBusinessType());
        if (BUSINESS_TYPE_SETTLE.equalsIgnoreCase(businessType)) {
            rollbackSettle(instance);
        } else if (BUSINESS_TYPE_TAX.equalsIgnoreCase(businessType)) {
            rollbackTax(instance);
        } else if (BUSINESS_TYPE_CURRENCY.equalsIgnoreCase(businessType)) {
            rollbackCurrency(instance);
        } else if (BUSINESS_TYPE_UOM.equalsIgnoreCase(businessType)) {
            rollbackUom(instance);
        }
    }

    private void rollbackSettle(SysWorkflowInstance instance) {
        Map<String, Object> meta = readMeta(instance, META_KEY_SETTLE);
        Long id = readLong(meta.get("settleMethodId"));
        if (id == null) {
            return;
        }
        String rollbackStatus = MdmWorkflowActionSupport.ACTIVATE.equalsIgnoreCase(readString(meta.get("action")))
                ? MdmStatusSupport.DRAFT
                : MdmStatusSupport.ACTIVE;
        settleMethodMapper.update(new MdmSettleMethod(), new LambdaUpdateWrapper<MdmSettleMethod>()
                .eq(MdmSettleMethod::getSettleMethodId, id)
                .eq(MdmSettleMethod::getDelFlag, DEL_FLAG_EXIST)
                .eq(MdmSettleMethod::getStatus, MdmStatusSupport.SUBMITTED)
                .set(MdmSettleMethod::getStatus, rollbackStatus)
                .set(MdmSettleMethod::getUpdateBy, resolveOperator(instance))
                .set(MdmSettleMethod::getUpdateTime, new Date()));
    }

    private void rollbackTax(SysWorkflowInstance instance) {
        Map<String, Object> meta = readMeta(instance, META_KEY_TAX);
        Long id = readLong(meta.get("taxRateId"));
        if (id == null) {
            return;
        }
        String rollbackStatus = MdmWorkflowActionSupport.ACTIVATE.equalsIgnoreCase(readString(meta.get("action")))
                ? MdmStatusSupport.DRAFT
                : MdmStatusSupport.ACTIVE;
        taxRateMapper.update(new MdmTaxRate(), new LambdaUpdateWrapper<MdmTaxRate>()
                .eq(MdmTaxRate::getTaxRateId, id)
                .eq(MdmTaxRate::getDelFlag, DEL_FLAG_EXIST)
                .eq(MdmTaxRate::getStatus, MdmStatusSupport.SUBMITTED)
                .set(MdmTaxRate::getStatus, rollbackStatus)
                .set(MdmTaxRate::getUpdateBy, resolveOperator(instance))
                .set(MdmTaxRate::getUpdateTime, new Date()));
    }

    private void rollbackCurrency(SysWorkflowInstance instance) {
        Map<String, Object> meta = readMeta(instance, META_KEY_CURRENCY);
        Long id = readLong(meta.get("currencyId"));
        if (id == null) {
            return;
        }
        String rollbackStatus = MdmWorkflowActionSupport.ACTIVATE.equalsIgnoreCase(readString(meta.get("action")))
                ? MdmStatusSupport.DRAFT
                : MdmStatusSupport.ACTIVE;
        currencyMapper.update(new MdmCurrency(), new LambdaUpdateWrapper<MdmCurrency>()
                .eq(MdmCurrency::getCurrencyId, id)
                .eq(MdmCurrency::getDelFlag, DEL_FLAG_EXIST)
                .eq(MdmCurrency::getStatus, MdmStatusSupport.SUBMITTED)
                .set(MdmCurrency::getStatus, rollbackStatus)
                .set(MdmCurrency::getUpdateBy, resolveOperator(instance))
                .set(MdmCurrency::getUpdateTime, new Date()));
    }

    private void rollbackUom(SysWorkflowInstance instance) {
        Map<String, Object> meta = readMeta(instance, META_KEY_UOM);
        Long id = readLong(meta.get("uomId"));
        if (id == null) {
            return;
        }
        String rollbackStatus = MdmWorkflowActionSupport.ACTIVATE.equalsIgnoreCase(readString(meta.get("action")))
                ? MdmStatusSupport.DRAFT
                : MdmStatusSupport.ACTIVE;
        uomMapper.update(new MdmUom(), new LambdaUpdateWrapper<MdmUom>()
                .eq(MdmUom::getUomId, id)
                .eq(MdmUom::getDelFlag, DEL_FLAG_EXIST)
                .eq(MdmUom::getStatus, MdmStatusSupport.SUBMITTED)
                .set(MdmUom::getStatus, rollbackStatus)
                .set(MdmUom::getUpdateBy, resolveOperator(instance))
                .set(MdmUom::getUpdateTime, new Date()));
    }

    private void activateSettle(Long id, Integer baseVersionNo, SysWorkflowInstance instance) { activateSimple(loadSettle(id), id, baseVersionNo, instance, MdmDomainTypeSupport.SETTLE_METHOD); }
    private void activateTax(Long id, Integer baseVersionNo, SysWorkflowInstance instance) { activateSimple(loadTax(id), id, baseVersionNo, instance, MdmDomainTypeSupport.TAX_RATE); }
    private void activateCurrency(Long id, Integer baseVersionNo, SysWorkflowInstance instance) { activateSimple(loadCurrency(id), id, baseVersionNo, instance, MdmDomainTypeSupport.CURRENCY); }
    private void activateUom(Long id, Integer baseVersionNo, SysWorkflowInstance instance) { activateSimple(loadUom(id), id, baseVersionNo, instance, MdmDomainTypeSupport.UOM); }

    private void activateSimple(Object before, Long id, Integer baseVersionNo, SysWorkflowInstance instance, String domainType) {
        if (before == null) {
            throw new IllegalStateException("字典不存在，无法完成审批回写");
        }
        if (before instanceof MdmSettleMethod) {
            boolean updated = settleMethodMapper.update(new MdmSettleMethod(), new LambdaUpdateWrapper<MdmSettleMethod>()
                    .eq(MdmSettleMethod::getSettleMethodId, id)
                    .eq(MdmSettleMethod::getDelFlag, DEL_FLAG_EXIST)
                    .eq(MdmSettleMethod::getStatus, MdmStatusSupport.SUBMITTED)
                    .eq(baseVersionNo != null, MdmSettleMethod::getVersionNo, baseVersionNo)
                    .set(MdmSettleMethod::getStatus, MdmStatusSupport.ACTIVE)
                    .set(MdmSettleMethod::getUpdateBy, resolveOperator(instance))
                    .set(MdmSettleMethod::getUpdateTime, new Date())) > 0;
            if (!updated) {
                throw new IllegalStateException("结算方式状态已变化，无法完成审批回写");
            }
            auditTrailService.record(domainType, id, MdmChangeTypeSupport.STATUS, loadSettle(id).getVersionNo(), MdmStatusSupport.ACTIVE, before, loadSettle(id));
        } else if (before instanceof MdmTaxRate) {
            boolean updated = taxRateMapper.update(new MdmTaxRate(), new LambdaUpdateWrapper<MdmTaxRate>()
                    .eq(MdmTaxRate::getTaxRateId, id)
                    .eq(MdmTaxRate::getDelFlag, DEL_FLAG_EXIST)
                    .eq(MdmTaxRate::getStatus, MdmStatusSupport.SUBMITTED)
                    .eq(baseVersionNo != null, MdmTaxRate::getVersionNo, baseVersionNo)
                    .set(MdmTaxRate::getStatus, MdmStatusSupport.ACTIVE)
                    .set(MdmTaxRate::getUpdateBy, resolveOperator(instance))
                    .set(MdmTaxRate::getUpdateTime, new Date())) > 0;
            if (!updated) {
                throw new IllegalStateException("税率状态已变化，无法完成审批回写");
            }
            auditTrailService.record(domainType, id, MdmChangeTypeSupport.STATUS, loadTax(id).getVersionNo(), MdmStatusSupport.ACTIVE, before, loadTax(id));
        } else if (before instanceof MdmCurrency) {
            boolean updated = currencyMapper.update(new MdmCurrency(), new LambdaUpdateWrapper<MdmCurrency>()
                    .eq(MdmCurrency::getCurrencyId, id)
                    .eq(MdmCurrency::getDelFlag, DEL_FLAG_EXIST)
                    .eq(MdmCurrency::getStatus, MdmStatusSupport.SUBMITTED)
                    .eq(baseVersionNo != null, MdmCurrency::getVersionNo, baseVersionNo)
                    .set(MdmCurrency::getStatus, MdmStatusSupport.ACTIVE)
                    .set(MdmCurrency::getUpdateBy, resolveOperator(instance))
                    .set(MdmCurrency::getUpdateTime, new Date())) > 0;
            if (!updated) {
                throw new IllegalStateException("币种状态已变化，无法完成审批回写");
            }
            auditTrailService.record(domainType, id, MdmChangeTypeSupport.STATUS, loadCurrency(id).getVersionNo(), MdmStatusSupport.ACTIVE, before, loadCurrency(id));
        } else if (before instanceof MdmUom) {
            boolean updated = uomMapper.update(new MdmUom(), new LambdaUpdateWrapper<MdmUom>()
                    .eq(MdmUom::getUomId, id)
                    .eq(MdmUom::getDelFlag, DEL_FLAG_EXIST)
                    .eq(MdmUom::getStatus, MdmStatusSupport.SUBMITTED)
                    .eq(baseVersionNo != null, MdmUom::getVersionNo, baseVersionNo)
                    .set(MdmUom::getStatus, MdmStatusSupport.ACTIVE)
                    .set(MdmUom::getUpdateBy, resolveOperator(instance))
                    .set(MdmUom::getUpdateTime, new Date())) > 0;
            if (!updated) {
                throw new IllegalStateException("计量单位状态已变化，无法完成审批回写");
            }
            auditTrailService.record(domainType, id, MdmChangeTypeSupport.STATUS, loadUom(id).getVersionNo(), MdmStatusSupport.ACTIVE, before, loadUom(id));
        }
    }

    private void applySettleChange(Long id, Integer baseVersionNo, Map<String, Object> meta, SysWorkflowInstance instance) { applySettleLike(id, baseVersionNo, meta, instance, "afterSettleMethod", loadSettle(id), MdmDomainTypeSupport.SETTLE_METHOD); }
    private void applyTaxChange(Long id, Integer baseVersionNo, Map<String, Object> meta, SysWorkflowInstance instance) { applySettleLike(id, baseVersionNo, meta, instance, "afterTaxRate", loadTax(id), MdmDomainTypeSupport.TAX_RATE); }
    private void applyCurrencyChange(Long id, Integer baseVersionNo, Map<String, Object> meta, SysWorkflowInstance instance) { applySettleLike(id, baseVersionNo, meta, instance, "afterCurrency", loadCurrency(id), MdmDomainTypeSupport.CURRENCY); }
    private void applyUomChange(Long id, Integer baseVersionNo, Map<String, Object> meta, SysWorkflowInstance instance) { applySettleLike(id, baseVersionNo, meta, instance, "afterUom", loadUom(id), MdmDomainTypeSupport.UOM); }

    private void applySettleLike(Long id, Integer baseVersionNo, Map<String, Object> meta, SysWorkflowInstance instance, String key, Object before, String domainType) {
        if (before instanceof MdmSettleMethod) {
            if (!MdmStatusSupport.isSubmitted(((MdmSettleMethod) before).getStatus())) {
                throw new IllegalStateException("结算方式状态已变化，请重新发起审批");
            }
            MdmSettleMethod after = objectMapper.convertValue(meta.get(key), MdmSettleMethod.class);
            MdmSettleMethod updateEntity = new MdmSettleMethod();
            BeanUtils.copyProperties(after, updateEntity);
            updateEntity.setSettleMethodId(id);
            updateEntity.setTenantId(((MdmSettleMethod) before).getTenantId());
            updateEntity.setSettleCode(((MdmSettleMethod) before).getSettleCode());
            updateEntity.setStatus(MdmStatusSupport.ACTIVE);
            updateEntity.setVersionNo(MdmValueSupport.resolveNextVersionNo(((MdmSettleMethod) before).getVersionNo()));
            updateEntity.setUpdateBy(resolveOperator(instance));
            updateEntity.setUpdateTime(new Date());
            updateEntity.setCreateBy(null);
            updateEntity.setCreateTime(null);
            updateEntity.setDelFlag(null);
            boolean updated = settleMethodMapper.update(updateEntity, new LambdaUpdateWrapper<MdmSettleMethod>()
                    .eq(MdmSettleMethod::getSettleMethodId, id)
                    .eq(MdmSettleMethod::getDelFlag, DEL_FLAG_EXIST)
                    .eq(MdmSettleMethod::getStatus, MdmStatusSupport.SUBMITTED)
                    .eq(baseVersionNo != null, MdmSettleMethod::getVersionNo, baseVersionNo)) > 0;
            if (!updated) {
                throw new IllegalStateException("结算方式版本已变化，请重新发起审批");
            }
            auditTrailService.record(domainType, id, MdmChangeTypeSupport.UPDATE, loadSettle(id).getVersionNo(), loadSettle(id).getStatus(), before, loadSettle(id));
        } else if (before instanceof MdmTaxRate) {
            if (!MdmStatusSupport.isSubmitted(((MdmTaxRate) before).getStatus())) {
                throw new IllegalStateException("税率状态已变化，请重新发起审批");
            }
            MdmTaxRate after = objectMapper.convertValue(meta.get(key), MdmTaxRate.class);
            MdmTaxRate updateEntity = new MdmTaxRate();
            BeanUtils.copyProperties(after, updateEntity);
            updateEntity.setTaxRateId(id);
            updateEntity.setTenantId(((MdmTaxRate) before).getTenantId());
            updateEntity.setTaxCode(((MdmTaxRate) before).getTaxCode());
            updateEntity.setStatus(MdmStatusSupport.ACTIVE);
            updateEntity.setVersionNo(MdmValueSupport.resolveNextVersionNo(((MdmTaxRate) before).getVersionNo()));
            updateEntity.setUpdateBy(resolveOperator(instance));
            updateEntity.setUpdateTime(new Date());
            updateEntity.setCreateBy(null);
            updateEntity.setCreateTime(null);
            updateEntity.setDelFlag(null);
            boolean updated = taxRateMapper.update(updateEntity, new LambdaUpdateWrapper<MdmTaxRate>()
                    .eq(MdmTaxRate::getTaxRateId, id)
                    .eq(MdmTaxRate::getDelFlag, DEL_FLAG_EXIST)
                    .eq(MdmTaxRate::getStatus, MdmStatusSupport.SUBMITTED)
                    .eq(baseVersionNo != null, MdmTaxRate::getVersionNo, baseVersionNo)) > 0;
            if (!updated) {
                throw new IllegalStateException("税率版本已变化，请重新发起审批");
            }
            auditTrailService.record(domainType, id, MdmChangeTypeSupport.UPDATE, loadTax(id).getVersionNo(), loadTax(id).getStatus(), before, loadTax(id));
        } else if (before instanceof MdmCurrency) {
            if (!MdmStatusSupport.isSubmitted(((MdmCurrency) before).getStatus())) {
                throw new IllegalStateException("币种状态已变化，请重新发起审批");
            }
            MdmCurrency after = objectMapper.convertValue(meta.get(key), MdmCurrency.class);
            MdmCurrency updateEntity = new MdmCurrency();
            BeanUtils.copyProperties(after, updateEntity);
            updateEntity.setCurrencyId(id);
            updateEntity.setTenantId(((MdmCurrency) before).getTenantId());
            updateEntity.setCurrencyCode(((MdmCurrency) before).getCurrencyCode());
            updateEntity.setStatus(MdmStatusSupport.ACTIVE);
            updateEntity.setVersionNo(MdmValueSupport.resolveNextVersionNo(((MdmCurrency) before).getVersionNo()));
            updateEntity.setUpdateBy(resolveOperator(instance));
            updateEntity.setUpdateTime(new Date());
            updateEntity.setCreateBy(null);
            updateEntity.setCreateTime(null);
            updateEntity.setDelFlag(null);
            boolean updated = currencyMapper.update(updateEntity, new LambdaUpdateWrapper<MdmCurrency>()
                    .eq(MdmCurrency::getCurrencyId, id)
                    .eq(MdmCurrency::getDelFlag, DEL_FLAG_EXIST)
                    .eq(MdmCurrency::getStatus, MdmStatusSupport.SUBMITTED)
                    .eq(baseVersionNo != null, MdmCurrency::getVersionNo, baseVersionNo)) > 0;
            if (!updated) {
                throw new IllegalStateException("币种版本已变化，请重新发起审批");
            }
            auditTrailService.record(domainType, id, MdmChangeTypeSupport.UPDATE, loadCurrency(id).getVersionNo(), loadCurrency(id).getStatus(), before, loadCurrency(id));
        } else if (before instanceof MdmUom) {
            if (!MdmStatusSupport.isSubmitted(((MdmUom) before).getStatus())) {
                throw new IllegalStateException("计量单位状态已变化，请重新发起审批");
            }
            MdmUom after = objectMapper.convertValue(meta.get(key), MdmUom.class);
            MdmUom updateEntity = new MdmUom();
            BeanUtils.copyProperties(after, updateEntity);
            updateEntity.setUomId(id);
            updateEntity.setTenantId(((MdmUom) before).getTenantId());
            updateEntity.setUomCode(((MdmUom) before).getUomCode());
            updateEntity.setStatus(MdmStatusSupport.ACTIVE);
            updateEntity.setVersionNo(MdmValueSupport.resolveNextVersionNo(((MdmUom) before).getVersionNo()));
            updateEntity.setUpdateBy(resolveOperator(instance));
            updateEntity.setUpdateTime(new Date());
            updateEntity.setCreateBy(null);
            updateEntity.setCreateTime(null);
            updateEntity.setDelFlag(null);
            boolean updated = uomMapper.update(updateEntity, new LambdaUpdateWrapper<MdmUom>()
                    .eq(MdmUom::getUomId, id)
                    .eq(MdmUom::getDelFlag, DEL_FLAG_EXIST)
                    .eq(MdmUom::getStatus, MdmStatusSupport.SUBMITTED)
                    .eq(baseVersionNo != null, MdmUom::getVersionNo, baseVersionNo)) > 0;
            if (!updated) {
                throw new IllegalStateException("计量单位版本已变化，请重新发起审批");
            }
            auditTrailService.record(domainType, id, MdmChangeTypeSupport.UPDATE, loadUom(id).getVersionNo(), loadUom(id).getStatus(), before, loadUom(id));
        }
    }

    private void disableSettle(Long id, Integer baseVersionNo, SysWorkflowInstance instance) { disableSimple(loadSettle(id), id, baseVersionNo, instance, MdmDomainTypeSupport.SETTLE_METHOD); }
    private void disableTax(Long id, Integer baseVersionNo, SysWorkflowInstance instance) { disableSimple(loadTax(id), id, baseVersionNo, instance, MdmDomainTypeSupport.TAX_RATE); }
    private void disableCurrency(Long id, Integer baseVersionNo, SysWorkflowInstance instance) { disableSimple(loadCurrency(id), id, baseVersionNo, instance, MdmDomainTypeSupport.CURRENCY); }
    private void disableUom(Long id, Integer baseVersionNo, SysWorkflowInstance instance) { disableSimple(loadUom(id), id, baseVersionNo, instance, MdmDomainTypeSupport.UOM); }

    private void disableSimple(Object before, Long id, Integer baseVersionNo, SysWorkflowInstance instance, String domainType) {
        if (before instanceof MdmSettleMethod) {
            if (!MdmStatusSupport.isSubmitted(((MdmSettleMethod) before).getStatus())) {
                throw new IllegalStateException("结算方式状态已变化，请重新发起审批");
            }
            MdmSettleMethod updateEntity = new MdmSettleMethod();
            updateEntity.setSettleMethodId(id);
            updateEntity.setStatus(MdmStatusSupport.DISABLED);
            updateEntity.setVersionNo(MdmValueSupport.resolveNextVersionNo(((MdmSettleMethod) before).getVersionNo()));
            updateEntity.setUpdateBy(resolveOperator(instance));
            updateEntity.setUpdateTime(new Date());
            boolean updated = settleMethodMapper.update(updateEntity, new LambdaUpdateWrapper<MdmSettleMethod>()
                    .eq(MdmSettleMethod::getSettleMethodId, id)
                    .eq(MdmSettleMethod::getDelFlag, DEL_FLAG_EXIST)
                    .eq(MdmSettleMethod::getStatus, MdmStatusSupport.SUBMITTED)
                    .eq(baseVersionNo != null, MdmSettleMethod::getVersionNo, baseVersionNo)) > 0;
            if (!updated) {
                throw new IllegalStateException("结算方式版本已变化，请重新发起审批");
            }
            auditTrailService.record(domainType, id, MdmChangeTypeSupport.STATUS, loadSettle(id).getVersionNo(), MdmStatusSupport.DISABLED, before, loadSettle(id));
        } else if (before instanceof MdmTaxRate) {
            if (!MdmStatusSupport.isSubmitted(((MdmTaxRate) before).getStatus())) {
                throw new IllegalStateException("税率状态已变化，请重新发起审批");
            }
            MdmTaxRate updateEntity = new MdmTaxRate();
            updateEntity.setTaxRateId(id);
            updateEntity.setStatus(MdmStatusSupport.DISABLED);
            updateEntity.setVersionNo(MdmValueSupport.resolveNextVersionNo(((MdmTaxRate) before).getVersionNo()));
            updateEntity.setUpdateBy(resolveOperator(instance));
            updateEntity.setUpdateTime(new Date());
            boolean updated = taxRateMapper.update(updateEntity, new LambdaUpdateWrapper<MdmTaxRate>()
                    .eq(MdmTaxRate::getTaxRateId, id)
                    .eq(MdmTaxRate::getDelFlag, DEL_FLAG_EXIST)
                    .eq(MdmTaxRate::getStatus, MdmStatusSupport.SUBMITTED)
                    .eq(baseVersionNo != null, MdmTaxRate::getVersionNo, baseVersionNo)) > 0;
            if (!updated) {
                throw new IllegalStateException("税率版本已变化，请重新发起审批");
            }
            auditTrailService.record(domainType, id, MdmChangeTypeSupport.STATUS, loadTax(id).getVersionNo(), MdmStatusSupport.DISABLED, before, loadTax(id));
        } else if (before instanceof MdmCurrency) {
            if (!MdmStatusSupport.isSubmitted(((MdmCurrency) before).getStatus())) {
                throw new IllegalStateException("币种状态已变化，请重新发起审批");
            }
            MdmCurrency updateEntity = new MdmCurrency();
            updateEntity.setCurrencyId(id);
            updateEntity.setStatus(MdmStatusSupport.DISABLED);
            updateEntity.setVersionNo(MdmValueSupport.resolveNextVersionNo(((MdmCurrency) before).getVersionNo()));
            updateEntity.setUpdateBy(resolveOperator(instance));
            updateEntity.setUpdateTime(new Date());
            boolean updated = currencyMapper.update(updateEntity, new LambdaUpdateWrapper<MdmCurrency>()
                    .eq(MdmCurrency::getCurrencyId, id)
                    .eq(MdmCurrency::getDelFlag, DEL_FLAG_EXIST)
                    .eq(MdmCurrency::getStatus, MdmStatusSupport.SUBMITTED)
                    .eq(baseVersionNo != null, MdmCurrency::getVersionNo, baseVersionNo)) > 0;
            if (!updated) {
                throw new IllegalStateException("币种版本已变化，请重新发起审批");
            }
            auditTrailService.record(domainType, id, MdmChangeTypeSupport.STATUS, loadCurrency(id).getVersionNo(), MdmStatusSupport.DISABLED, before, loadCurrency(id));
        } else if (before instanceof MdmUom) {
            if (!MdmStatusSupport.isSubmitted(((MdmUom) before).getStatus())) {
                throw new IllegalStateException("计量单位状态已变化，请重新发起审批");
            }
            MdmUom updateEntity = new MdmUom();
            updateEntity.setUomId(id);
            updateEntity.setStatus(MdmStatusSupport.DISABLED);
            updateEntity.setVersionNo(MdmValueSupport.resolveNextVersionNo(((MdmUom) before).getVersionNo()));
            updateEntity.setUpdateBy(resolveOperator(instance));
            updateEntity.setUpdateTime(new Date());
            boolean updated = uomMapper.update(updateEntity, new LambdaUpdateWrapper<MdmUom>()
                    .eq(MdmUom::getUomId, id)
                    .eq(MdmUom::getDelFlag, DEL_FLAG_EXIST)
                    .eq(MdmUom::getStatus, MdmStatusSupport.SUBMITTED)
                    .eq(baseVersionNo != null, MdmUom::getVersionNo, baseVersionNo)) > 0;
            if (!updated) {
                throw new IllegalStateException("计量单位版本已变化，请重新发起审批");
            }
            auditTrailService.record(domainType, id, MdmChangeTypeSupport.STATUS, loadUom(id).getVersionNo(), MdmStatusSupport.DISABLED, before, loadUom(id));
        }
    }

    private MdmSettleMethod loadSettle(Long id) { return settleMethodMapper.selectOne(new LambdaQueryWrapper<MdmSettleMethod>().eq(MdmSettleMethod::getSettleMethodId, id).eq(MdmSettleMethod::getDelFlag, DEL_FLAG_EXIST)); }
    private MdmTaxRate loadTax(Long id) { return taxRateMapper.selectOne(new LambdaQueryWrapper<MdmTaxRate>().eq(MdmTaxRate::getTaxRateId, id).eq(MdmTaxRate::getDelFlag, DEL_FLAG_EXIST)); }
    private MdmCurrency loadCurrency(Long id) { return currencyMapper.selectOne(new LambdaQueryWrapper<MdmCurrency>().eq(MdmCurrency::getCurrencyId, id).eq(MdmCurrency::getDelFlag, DEL_FLAG_EXIST)); }
    private MdmUom loadUom(Long id) { return uomMapper.selectOne(new LambdaQueryWrapper<MdmUom>().eq(MdmUom::getUomId, id).eq(MdmUom::getDelFlag, DEL_FLAG_EXIST)); }

    @SuppressWarnings("unchecked")
    private Map<String, Object> readMeta(SysWorkflowInstance instance, String metaKey) {
        if (instance == null || !StringUtils.hasText(instance.getFormData())) {
            return Map.of();
        }
        try {
            Map<String, Object> formData = objectMapper.readValue(instance.getFormData(), Map.class);
            Object meta = formData.get(metaKey);
            return meta instanceof Map ? (Map<String, Object>) meta : Map.of();
        } catch (Exception ex) {
            throw new IllegalStateException("解析字典审批元数据失败", ex);
        }
    }

    private String readString(Object value) { return value == null ? null : StringUtils.trimWhitespace(String.valueOf(value)); }
    private Long readLong(Object value) { try { return value == null ? null : Long.valueOf(String.valueOf(value)); } catch (Exception ex) { return null; } }
    private Integer readInteger(Object value) { try { return value == null ? null : Integer.valueOf(String.valueOf(value)); } catch (Exception ex) { return null; } }
    private String resolveOperator(SysWorkflowInstance instance) { return instance != null && StringUtils.hasText(instance.getLastActionUserName()) ? instance.getLastActionUserName().trim() : "system"; }
}

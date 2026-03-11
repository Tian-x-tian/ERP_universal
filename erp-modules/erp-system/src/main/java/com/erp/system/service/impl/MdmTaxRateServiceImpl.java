package com.erp.system.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.erp.system.domain.MdmTaxRate;
import com.erp.system.mapper.MdmTaxRateMapper;
import com.erp.system.security.service.SecurityUserResolver;
import com.erp.system.service.IMdmAuditTrailService;
import com.erp.system.service.IMdmTaxRateService;
import com.erp.system.support.MdmChangeTypeSupport;
import com.erp.system.support.MdmDomainTypeSupport;
import com.erp.system.support.MdmStatusSupport;
import com.erp.system.support.MdmValueSupport;
import com.erp.system.support.TenantWriteGuard;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;

/**
 * MDM 税率字典服务实现。
 */
@Service
public class MdmTaxRateServiceImpl extends ServiceImpl<MdmTaxRateMapper, MdmTaxRate> implements IMdmTaxRateService {
    private static final String DEL_FLAG_EXIST = "0";
    private static final String DEL_FLAG_DELETED = "2";
    private static final String DEFAULT_OPERATOR = "system";

    private final IMdmAuditTrailService auditTrailService;
    private final SecurityUserResolver securityUserResolver;

    public MdmTaxRateServiceImpl(IMdmAuditTrailService auditTrailService, SecurityUserResolver securityUserResolver) {
        this.auditTrailService = auditTrailService;
        this.securityUserResolver = securityUserResolver;
    }

    /**
     * 查询税率列表。
     *
     * @param taxCode 税率编码
     * @param taxName 税率名称
     * @param status  状态
     * @return 税率列表
     */
    @Override
    public List<MdmTaxRate> selectList(String taxCode, String taxName, String status) {
        LambdaQueryWrapper<MdmTaxRate> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(MdmTaxRate::getDelFlag, DEL_FLAG_EXIST);
        if (StringUtils.hasText(taxCode)) {
            queryWrapper.like(MdmTaxRate::getTaxCode, taxCode.trim());
        }
        if (StringUtils.hasText(taxName)) {
            queryWrapper.like(MdmTaxRate::getTaxName, taxName.trim());
        }
        if (StringUtils.hasText(status)) {
            queryWrapper.eq(MdmTaxRate::getStatus, MdmStatusSupport.normalizeStatus(status));
        }
        queryWrapper.orderByDesc(MdmTaxRate::getUpdateTime).orderByDesc(MdmTaxRate::getCreateTime);
        return list(queryWrapper);
    }

    /**
     * 新增税率。
     *
     * @param taxRate 税率对象
     * @return true 表示新增成功
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean create(MdmTaxRate taxRate) {
        if (taxRate == null || !StringUtils.hasText(taxRate.getTaxCode()) || !StringUtils.hasText(taxRate.getTaxName())
                || taxRate.getTaxRate() == null) {
            return false;
        }
        if (!isValidTaxRate(taxRate.getTaxRate()) || !isValidEffectiveRange(taxRate.getEffectiveFrom(), taxRate.getEffectiveTo())) {
            return false;
        }
        String tenantId = TenantWriteGuard.currentTenantId();
        if (!StringUtils.hasText(tenantId)) {
            return false;
        }
        String taxCode = taxRate.getTaxCode().trim();
        if (existsCode(taxCode, null)) {
            return false;
        }
        Date now = new Date();
        String operator = resolveOperator();
        taxRate.setTenantId(tenantId);
        taxRate.setTaxCode(taxCode);
        taxRate.setTaxName(taxRate.getTaxName().trim());
        taxRate.setStatus(MdmStatusSupport.normalizeStatus(taxRate.getStatus()));
        taxRate.setVersionNo(1);
        taxRate.setDelFlag(DEL_FLAG_EXIST);
        taxRate.setCreateBy(operator);
        taxRate.setUpdateBy(operator);
        taxRate.setCreateTime(now);
        taxRate.setUpdateTime(now);
        boolean saved = save(taxRate);
        if (saved) {
            auditTrailService.record(MdmDomainTypeSupport.TAX_RATE,
                    taxRate.getTaxRateId(),
                    MdmChangeTypeSupport.CREATE,
                    taxRate.getVersionNo(),
                    taxRate.getStatus(),
                    null,
                    taxRate);
        }
        return saved;
    }

    /**
     * 修改税率。
     *
     * @param taxRate 税率对象
     * @return true 表示修改成功
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean modify(MdmTaxRate taxRate) {
        if (taxRate == null || taxRate.getTaxRateId() == null) {
            return false;
        }
        MdmTaxRate existed = getOne(new LambdaQueryWrapper<MdmTaxRate>()
                .eq(MdmTaxRate::getTaxRateId, taxRate.getTaxRateId())
                .eq(MdmTaxRate::getDelFlag, DEL_FLAG_EXIST));
        if (existed == null) {
            return false;
        }
        if (taxRate.getTaxRate() != null && !isValidTaxRate(taxRate.getTaxRate())) {
            return false;
        }
        Date effectiveFrom = taxRate.getEffectiveFrom() != null ? taxRate.getEffectiveFrom() : existed.getEffectiveFrom();
        Date effectiveTo = taxRate.getEffectiveTo() != null ? taxRate.getEffectiveTo() : existed.getEffectiveTo();
        if (!isValidEffectiveRange(effectiveFrom, effectiveTo)) {
            return false;
        }
        MdmTaxRate before = new MdmTaxRate();
        BeanUtils.copyProperties(existed, before);

        if (StringUtils.hasText(taxRate.getTaxCode())) {
            String taxCode = taxRate.getTaxCode().trim();
            if (existsCode(taxCode, taxRate.getTaxRateId())) {
                return false;
            }
            taxRate.setTaxCode(taxCode);
        }
        if (StringUtils.hasText(taxRate.getTaxName())) {
            taxRate.setTaxName(taxRate.getTaxName().trim());
        }
        taxRate.setStatus(MdmStatusSupport.normalizeStatusForUpdate(taxRate.getStatus(), existed.getStatus()));
        taxRate.setVersionNo(MdmValueSupport.resolveNextVersionNo(existed.getVersionNo()));
        taxRate.setUpdateBy(resolveOperator());
        taxRate.setUpdateTime(new Date());
        boolean updated = updateById(taxRate);
        if (updated) {
            MdmTaxRate after = getById(taxRate.getTaxRateId());
            auditTrailService.record(MdmDomainTypeSupport.TAX_RATE,
                    taxRate.getTaxRateId(),
                    MdmChangeTypeSupport.UPDATE,
                    after == null ? taxRate.getVersionNo() : after.getVersionNo(),
                    after == null ? taxRate.getStatus() : after.getStatus(),
                    before,
                    after);
        }
        return updated;
    }

    /**
     * 停用税率。
     *
     * @param taxRateId 税率ID
     * @return true 表示停用成功
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean disable(Long taxRateId) {
        if (taxRateId == null) {
            return false;
        }
        MdmTaxRate existed = getOne(new LambdaQueryWrapper<MdmTaxRate>()
                .eq(MdmTaxRate::getTaxRateId, taxRateId)
                .eq(MdmTaxRate::getDelFlag, DEL_FLAG_EXIST));
        if (existed == null) {
            return false;
        }
        if (MdmStatusSupport.DISABLED.equals(existed.getStatus())) {
            return true;
        }
        MdmTaxRate updateEntity = new MdmTaxRate();
        updateEntity.setTaxRateId(taxRateId);
        updateEntity.setStatus(MdmStatusSupport.DISABLED);
        updateEntity.setVersionNo(MdmValueSupport.resolveNextVersionNo(existed.getVersionNo()));
        updateEntity.setUpdateBy(resolveOperator());
        updateEntity.setUpdateTime(new Date());
        boolean updated = updateById(updateEntity);
        if (updated) {
            MdmTaxRate after = getById(taxRateId);
            auditTrailService.record(MdmDomainTypeSupport.TAX_RATE,
                    taxRateId,
                    MdmChangeTypeSupport.STATUS,
                    after == null ? updateEntity.getVersionNo() : after.getVersionNo(),
                    after == null ? updateEntity.getStatus() : after.getStatus(),
                    existed,
                    after);
        }
        return updated;
    }

    /**
     * 删除税率（逻辑删除）。
     *
     * @param taxRateId 税率ID
     * @return true 表示删除成功
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean remove(Long taxRateId) {
        if (taxRateId == null) {
            return false;
        }
        MdmTaxRate existed = getOne(new LambdaQueryWrapper<MdmTaxRate>()
                .eq(MdmTaxRate::getTaxRateId, taxRateId)
                .eq(MdmTaxRate::getDelFlag, DEL_FLAG_EXIST));
        if (existed == null || MdmStatusSupport.isActive(existed.getStatus())) {
            return false;
        }
        MdmTaxRate updateEntity = new MdmTaxRate();
        updateEntity.setTaxRateId(taxRateId);
        updateEntity.setDelFlag(DEL_FLAG_DELETED);
        updateEntity.setVersionNo(MdmValueSupport.resolveNextVersionNo(existed.getVersionNo()));
        updateEntity.setUpdateBy(resolveOperator());
        updateEntity.setUpdateTime(new Date());
        boolean updated = updateById(updateEntity);
        if (updated) {
            auditTrailService.record(MdmDomainTypeSupport.TAX_RATE,
                    taxRateId,
                    MdmChangeTypeSupport.DELETE,
                    updateEntity.getVersionNo(),
                    existed.getStatus(),
                    existed,
                    null);
        }
        return updated;
    }

    /**
     * 判断税率编码是否重复。
     *
     * @param code 编码
     * @param excludeId 排除主键
     * @return true 表示重复
     */
    private boolean existsCode(String code, Long excludeId) {
        LambdaQueryWrapper<MdmTaxRate> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(MdmTaxRate::getTaxCode, code);
        queryWrapper.eq(MdmTaxRate::getDelFlag, DEL_FLAG_EXIST);
        if (excludeId != null) {
            queryWrapper.ne(MdmTaxRate::getTaxRateId, excludeId);
        }
        return count(queryWrapper) > 0;
    }

    /**
     * 校验税率值范围。
     *
     * @param taxRate 税率值
     * @return true 表示合法
     */
    private boolean isValidTaxRate(BigDecimal taxRate) {
        if (taxRate == null) {
            return false;
        }
        return taxRate.compareTo(BigDecimal.ZERO) >= 0 && taxRate.compareTo(BigDecimal.ONE) <= 0;
    }

    /**
     * 校验生效区间。
     *
     * @param effectiveFrom 生效开始
     * @param effectiveTo 生效结束
     * @return true 表示合法
     */
    private boolean isValidEffectiveRange(Date effectiveFrom, Date effectiveTo) {
        if (effectiveFrom == null || effectiveTo == null) {
            return true;
        }
        return !effectiveFrom.after(effectiveTo);
    }

    /**
     * 解析操作人账号。
     *
     * @return 操作人账号
     */
    private String resolveOperator() {
        String username = securityUserResolver.getCurrentUsername();
        return StringUtils.hasText(username) ? username.trim() : DEFAULT_OPERATOR;
    }
}

package com.erp.system.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.erp.system.domain.MdmCustomer;
import com.erp.system.domain.MdmCurrency;
import com.erp.system.domain.MdmSupplier;
import com.erp.system.mapper.MdmCustomerMapper;
import com.erp.system.mapper.MdmCurrencyMapper;
import com.erp.system.mapper.MdmSupplierMapper;
import com.erp.system.security.service.SecurityUserResolver;
import com.erp.system.service.IMdmAuditTrailService;
import com.erp.system.service.IMdmCurrencyService;
import com.erp.system.service.IMdmReferenceCheckService;
import com.erp.system.support.MdmChangeTypeSupport;
import com.erp.system.support.MdmDomainTypeSupport;
import com.erp.system.support.MdmStatusSupport;
import com.erp.system.support.MdmValueSupport;
import com.erp.system.support.TenantWriteGuard;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * MDM 币种字典服务实现。
 */
@Service
public class MdmCurrencyServiceImpl extends ServiceImpl<MdmCurrencyMapper, MdmCurrency> implements IMdmCurrencyService {
    private static final String DEL_FLAG_EXIST = "0";
    private static final String DEL_FLAG_DELETED = "2";
    private static final String DEFAULT_OPERATOR = "system";

    private final IMdmAuditTrailService auditTrailService;
    private final SecurityUserResolver securityUserResolver;
    private final MdmCustomerMapper customerMapper;
    private final MdmSupplierMapper supplierMapper;
    private final IMdmReferenceCheckService referenceCheckService;

    public MdmCurrencyServiceImpl(IMdmAuditTrailService auditTrailService,
            SecurityUserResolver securityUserResolver,
            MdmCustomerMapper customerMapper,
            MdmSupplierMapper supplierMapper,
            IMdmReferenceCheckService referenceCheckService) {
        this.auditTrailService = auditTrailService;
        this.securityUserResolver = securityUserResolver;
        this.customerMapper = customerMapper;
        this.supplierMapper = supplierMapper;
        this.referenceCheckService = referenceCheckService;
    }

    /**
     * 查询币种列表。
     *
     * @param currencyCode 币种编码
     * @param currencyName 币种名称
     * @param status       状态
     * @return 币种列表
     */
    @Override
    public List<MdmCurrency> selectList(String currencyCode, String currencyName, String status) {
        LambdaQueryWrapper<MdmCurrency> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(MdmCurrency::getDelFlag, DEL_FLAG_EXIST);
        if (StringUtils.hasText(currencyCode)) {
            queryWrapper.like(MdmCurrency::getCurrencyCode, currencyCode.trim());
        }
        if (StringUtils.hasText(currencyName)) {
            queryWrapper.like(MdmCurrency::getCurrencyName, currencyName.trim());
        }
        if (StringUtils.hasText(status)) {
            queryWrapper.eq(MdmCurrency::getStatus, MdmStatusSupport.normalizeStatus(status));
        }
        queryWrapper.orderByDesc(MdmCurrency::getUpdateTime).orderByDesc(MdmCurrency::getCreateTime);
        return list(queryWrapper);
    }

    /**
     * 新增币种。
     *
     * @param currency 币种对象
     * @return true 表示新增成功
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean create(MdmCurrency currency) {
        if (currency == null || !StringUtils.hasText(currency.getCurrencyCode())
                || !StringUtils.hasText(currency.getCurrencyName())) {
            return false;
        }
        if (!isValidEffectiveRange(currency.getEffectiveFrom(), currency.getEffectiveTo())) {
            return false;
        }
        String tenantId = TenantWriteGuard.currentTenantId();
        if (!StringUtils.hasText(tenantId)) {
            return false;
        }
        String currencyCode = normalizeCode(currency.getCurrencyCode());
        if (existsCode(currencyCode, null)) {
            return false;
        }
        Date now = new Date();
        String operator = resolveOperator();
        currency.setTenantId(tenantId);
        currency.setCurrencyCode(currencyCode);
        currency.setCurrencyName(currency.getCurrencyName().trim());
        currency.setSymbol(MdmValueSupport.trimToNull(currency.getSymbol()));
        currency.setPrecisionScale(currency.getPrecisionScale() == null ? 2 : currency.getPrecisionScale());
        currency.setStatus(MdmStatusSupport.DRAFT);
        currency.setVersionNo(1);
        currency.setDelFlag(DEL_FLAG_EXIST);
        boolean saved = save(currency);
        if (saved) {
            auditTrailService.record(MdmDomainTypeSupport.CURRENCY,
                    currency.getCurrencyId(),
                    MdmChangeTypeSupport.CREATE,
                    currency.getVersionNo(),
                    currency.getStatus(),
                    null,
                    currency);
        }
        return saved;
    }

    /**
     * 修改币种。
     *
     * @param currency 币种对象
     * @return true 表示修改成功
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean modify(MdmCurrency currency) {
        if (currency == null || currency.getCurrencyId() == null) {
            return false;
        }
        MdmCurrency existed = getOne(new LambdaQueryWrapper<MdmCurrency>()
                .eq(MdmCurrency::getCurrencyId, currency.getCurrencyId())
                .eq(MdmCurrency::getDelFlag, DEL_FLAG_EXIST));
        if (existed == null) {
            return false;
        }
        if (MdmStatusSupport.isSubmitted(existed.getStatus())) {
            throw new IllegalStateException("币种审批中，暂不允许直接修改");
        }
        if (!MdmStatusSupport.isDraft(existed.getStatus())) {
            throw new IllegalStateException("已生效币种请通过审批流程提交变更");
        }
        Date effectiveFrom = currency.getEffectiveFrom() != null ? currency.getEffectiveFrom()
                : existed.getEffectiveFrom();
        Date effectiveTo = currency.getEffectiveTo() != null ? currency.getEffectiveTo() : existed.getEffectiveTo();
        if (!isValidEffectiveRange(effectiveFrom, effectiveTo)) {
            return false;
        }
        MdmCurrency before = new MdmCurrency();
        BeanUtils.copyProperties(existed, before);

        if (StringUtils.hasText(currency.getCurrencyCode())) {
            String currencyCode = normalizeCode(currency.getCurrencyCode());
            if (existsCode(currencyCode, currency.getCurrencyId())) {
                return false;
            }
            currency.setCurrencyCode(currencyCode);
        }
        if (StringUtils.hasText(currency.getCurrencyName())) {
            currency.setCurrencyName(currency.getCurrencyName().trim());
        }
        if (StringUtils.hasText(currency.getSymbol())) {
            currency.setSymbol(currency.getSymbol().trim());
        }
        currency.setStatus(MdmStatusSupport.normalizeStatusForUpdate(currency.getStatus(), existed.getStatus()));
        currency.setVersionNo(MdmValueSupport.resolveNextVersionNo(existed.getVersionNo()));
        boolean updated = updateCurrencyByVersion(currency, existed.getVersionNo());
        if (updated) {
            MdmCurrency after = getById(currency.getCurrencyId());
            auditTrailService.record(MdmDomainTypeSupport.CURRENCY,
                    currency.getCurrencyId(),
                    MdmChangeTypeSupport.UPDATE,
                    after == null ? currency.getVersionNo() : after.getVersionNo(),
                    after == null ? currency.getStatus() : after.getStatus(),
                    before,
                    after);
        }
        return updated;
    }

    /**
     * 停用币种。
     *
     * @param currencyId 币种ID
     * @return true 表示停用成功
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean disable(Long currencyId) {
        if (currencyId == null) {
            return false;
        }

        referenceCheckService.check(MdmDomainTypeSupport.CURRENCY, currencyId);

        MdmCurrency existed = getOne(new LambdaQueryWrapper<MdmCurrency>()
                .eq(MdmCurrency::getCurrencyId, currencyId)
                .eq(MdmCurrency::getDelFlag, DEL_FLAG_EXIST));
        if (existed == null) {
            return false;
        }
        if (MdmStatusSupport.isSubmitted(existed.getStatus())) {
            throw new IllegalStateException("币种审批中，暂不允许直接停用");
        }
        if (MdmStatusSupport.isActive(existed.getStatus())) {
            throw new IllegalStateException("已生效币种请通过审批流程提交停用");
        }
        if (MdmStatusSupport.DISABLED.equals(existed.getStatus())) {
            return true;
        }
        MdmCurrency updateEntity = new MdmCurrency();
        updateEntity.setCurrencyId(currencyId);
        updateEntity.setStatus(MdmStatusSupport.DISABLED);
        updateEntity.setVersionNo(MdmValueSupport.resolveNextVersionNo(existed.getVersionNo()));
        boolean updated = updateCurrencyByVersion(updateEntity, existed.getVersionNo());
        if (updated) {
            MdmCurrency after = getById(currencyId);
            auditTrailService.record(MdmDomainTypeSupport.CURRENCY,
                    currencyId,
                    MdmChangeTypeSupport.STATUS,
                    after == null ? updateEntity.getVersionNo() : after.getVersionNo(),
                    after == null ? updateEntity.getStatus() : after.getStatus(),
                    existed,
                    after);
        }
        return updated;
    }

    /**
     * 删除币种（逻辑删除）。
     *
     * @param currencyId 币种ID
     * @return true 表示删除成功
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean remove(Long currencyId) {
        if (currencyId == null) {
            return false;
        }

        referenceCheckService.check(MdmDomainTypeSupport.CURRENCY, currencyId);

        if (isReferenced(currencyId)) {
            throw new IllegalStateException("币种已被客户或供应商引用，不能删除");
        }
        MdmCurrency existed = getOne(new LambdaQueryWrapper<MdmCurrency>()
                .eq(MdmCurrency::getCurrencyId, currencyId)
                .eq(MdmCurrency::getDelFlag, DEL_FLAG_EXIST));
        if (existed == null || !MdmStatusSupport.isDraft(existed.getStatus())) {
            return false;
        }
        if (MdmStatusSupport.isSubmitted(existed.getStatus())) {
            return false;
        }
        MdmCurrency updateEntity = new MdmCurrency();
        updateEntity.setCurrencyId(currencyId);
        updateEntity.setDelFlag(DEL_FLAG_DELETED);
        updateEntity.setVersionNo(MdmValueSupport.resolveNextVersionNo(existed.getVersionNo()));
        boolean updated = updateCurrencyByVersion(updateEntity, existed.getVersionNo());
        if (updated) {
            auditTrailService.record(MdmDomainTypeSupport.CURRENCY,
                    currencyId,
                    MdmChangeTypeSupport.DELETE,
                    updateEntity.getVersionNo(),
                    existed.getStatus(),
                    existed,
                    null);
        }
        return updated;
    }

    /**
     * 判断编码是否重复。
     *
     * @param code      编码
     * @param excludeId 排除主键
     * @return true 表示重复
     */
    private boolean existsCode(String code, Long excludeId) {
        LambdaQueryWrapper<MdmCurrency> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(MdmCurrency::getCurrencyCode, code);
        queryWrapper.eq(MdmCurrency::getDelFlag, DEL_FLAG_EXIST);
        if (excludeId != null) {
            queryWrapper.ne(MdmCurrency::getCurrencyId, excludeId);
        }
        return count(queryWrapper) > 0;
    }

    /**
     * 按版本号执行乐观锁更新。
     *
     * @param currency         更新对象
     * @param currentVersionNo 当前版本号
     * @return true 表示更新成功
     */
    private boolean updateCurrencyByVersion(MdmCurrency currency, Integer currentVersionNo) {
        if (currency == null || currency.getCurrencyId() == null) {
            return false;
        }
        LambdaUpdateWrapper<MdmCurrency> updateWrapper = new LambdaUpdateWrapper<MdmCurrency>()
                .eq(MdmCurrency::getCurrencyId, currency.getCurrencyId())
                .eq(MdmCurrency::getDelFlag, DEL_FLAG_EXIST);
        if (currentVersionNo != null) {
            updateWrapper.eq(MdmCurrency::getVersionNo, currentVersionNo);
        }
        boolean updated = update(currency, updateWrapper);
        if (!updated) {
            throw new IllegalStateException("币种数据已被其他人更新，请刷新后重试");
        }
        return updated;
    }

    /**
     * 判断币种是否已被客户或供应商引用。
     *
     * @param currencyId 币种ID
     * @return true 表示已引用
     */
    private boolean isReferenced(Long currencyId) {
        if (currencyId == null) {
            return false;
        }
        MdmCurrency existed = getById(currencyId);
        if (existed == null || !StringUtils.hasText(existed.getCurrencyCode())) {
            return false;
        }
        String currencyCode = existed.getCurrencyCode().trim();
        Long customerCount = customerMapper.selectCount(new LambdaQueryWrapper<MdmCustomer>()
                .eq(MdmCustomer::getDefaultCurrency, currencyCode)
                .eq(MdmCustomer::getDelFlag, DEL_FLAG_EXIST));
        if (customerCount != null && customerCount > 0) {
            return true;
        }
        Long supplierCount = supplierMapper.selectCount(new LambdaQueryWrapper<MdmSupplier>()
                .eq(MdmSupplier::getDefaultCurrency, currencyCode)
                .eq(MdmSupplier::getDelFlag, DEL_FLAG_EXIST));
        return supplierCount != null && supplierCount > 0;
    }

    /**
     * 校验生效区间。
     *
     * @param effectiveFrom 生效开始
     * @param effectiveTo   生效结束
     * @return true 表示合法
     */
    private boolean isValidEffectiveRange(Date effectiveFrom, Date effectiveTo) {
        if (effectiveFrom == null || effectiveTo == null) {
            return true;
        }
        return !effectiveFrom.after(effectiveTo);
    }

    /**
     * 规范币种编码。
     *
     * @param source 原始编码
     * @return 规范化编码
     */
    private String normalizeCode(String source) {
        return source == null ? null : source.trim().toUpperCase(Locale.ROOT);
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

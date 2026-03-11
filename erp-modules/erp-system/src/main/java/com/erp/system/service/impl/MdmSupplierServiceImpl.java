package com.erp.system.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.erp.system.domain.MdmSupplier;
import com.erp.system.mapper.MdmSupplierMapper;
import com.erp.system.security.service.SecurityUserResolver;
import com.erp.system.service.IMdmAuditTrailService;
import com.erp.system.service.IMdmSupplierService;
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

/**
 * MDM 供应商主数据服务实现。
 */
@Service
public class MdmSupplierServiceImpl extends ServiceImpl<MdmSupplierMapper, MdmSupplier> implements IMdmSupplierService {
    private static final String DEL_FLAG_EXIST = "0";
    private static final String DEL_FLAG_DELETED = "2";
    private static final String DEFAULT_OPERATOR = "system";

    private final IMdmAuditTrailService auditTrailService;
    private final SecurityUserResolver securityUserResolver;

    public MdmSupplierServiceImpl(IMdmAuditTrailService auditTrailService, SecurityUserResolver securityUserResolver) {
        this.auditTrailService = auditTrailService;
        this.securityUserResolver = securityUserResolver;
    }

    /**
     * 查询供应商列表。
     *
     * @param supplierCode 供应商编码
     * @param supplierName 供应商名称
     * @param status       状态
     * @return 供应商列表
     */
    @Override
    public List<MdmSupplier> selectSupplierList(String supplierCode, String supplierName, String status) {
        LambdaQueryWrapper<MdmSupplier> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(MdmSupplier::getDelFlag, DEL_FLAG_EXIST);
        if (StringUtils.hasText(supplierCode)) {
            queryWrapper.like(MdmSupplier::getSupplierCode, supplierCode.trim());
        }
        if (StringUtils.hasText(supplierName)) {
            queryWrapper.like(MdmSupplier::getSupplierName, supplierName.trim());
        }
        if (StringUtils.hasText(status)) {
            queryWrapper.eq(MdmSupplier::getStatus, MdmStatusSupport.normalizeStatus(status));
        }
        queryWrapper.orderByDesc(MdmSupplier::getUpdateTime).orderByDesc(MdmSupplier::getCreateTime);
        return list(queryWrapper);
    }

    /**
     * 新增供应商。
     *
     * @param supplier 供应商对象
     * @return true 表示成功
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean createSupplier(MdmSupplier supplier) {
        if (supplier == null || !StringUtils.hasText(supplier.getSupplierCode())
                || !StringUtils.hasText(supplier.getSupplierName())) {
            return false;
        }
        String tenantId = TenantWriteGuard.currentTenantId();
        if (!StringUtils.hasText(tenantId)) {
            return false;
        }
        String supplierCode = supplier.getSupplierCode().trim();
        if (existsSupplierCode(supplierCode, null) || !MdmValueSupport.isValidTaxNo(supplier.getTaxNo())) {
            return false;
        }
        String operator = resolveOperator();
        Date now = new Date();
        supplier.setTenantId(tenantId);
        supplier.setSupplierCode(supplierCode);
        supplier.setSupplierName(supplier.getSupplierName().trim());
        supplier.setShortName(MdmValueSupport.trimToNull(supplier.getShortName()));
        supplier.setSupplyCategory(MdmValueSupport.trimToNull(supplier.getSupplyCategory()));
        supplier.setTaxNo(MdmValueSupport.trimToNull(supplier.getTaxNo()));
        supplier.setDefaultCurrency(MdmValueSupport.trimToNull(supplier.getDefaultCurrency()));
        supplier.setQualityLevel(MdmValueSupport.trimToNull(supplier.getQualityLevel()));
        supplier.setBankAccountInfo(MdmValueSupport.trimToNull(supplier.getBankAccountInfo()));
        supplier.setContactName(MdmValueSupport.trimToNull(supplier.getContactName()));
        supplier.setContactPhone(MdmValueSupport.trimToNull(supplier.getContactPhone()));
        supplier.setContactEmail(MdmValueSupport.trimToNull(supplier.getContactEmail()));
        supplier.setAddress(MdmValueSupport.trimToNull(supplier.getAddress()));
        supplier.setStatus(MdmStatusSupport.normalizeStatus(supplier.getStatus()));
        supplier.setVersionNo(1);
        supplier.setDelFlag(DEL_FLAG_EXIST);
        supplier.setCreateBy(operator);
        supplier.setUpdateBy(operator);
        supplier.setCreateTime(now);
        supplier.setUpdateTime(now);
        if (MdmStatusSupport.isActive(supplier.getStatus())) {
            supplier.setEffectiveTime(now);
        }
        boolean saved = save(supplier);
        if (saved) {
            auditTrailService.record(MdmDomainTypeSupport.SUPPLIER,
                    supplier.getSupplierId(),
                    MdmChangeTypeSupport.CREATE,
                    supplier.getVersionNo(),
                    supplier.getStatus(),
                    null,
                    supplier);
        }
        return saved;
    }

    /**
     * 修改供应商。
     *
     * @param supplier 供应商对象
     * @return true 表示成功
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean updateSupplier(MdmSupplier supplier) {
        if (supplier == null || supplier.getSupplierId() == null) {
            return false;
        }
        MdmSupplier existed = getOne(new LambdaQueryWrapper<MdmSupplier>()
                .eq(MdmSupplier::getSupplierId, supplier.getSupplierId())
                .eq(MdmSupplier::getDelFlag, DEL_FLAG_EXIST));
        if (existed == null) {
            return false;
        }
        MdmSupplier before = new MdmSupplier();
        BeanUtils.copyProperties(existed, before);

        if (StringUtils.hasText(supplier.getSupplierCode())) {
            String supplierCode = supplier.getSupplierCode().trim();
            if (existsSupplierCode(supplierCode, supplier.getSupplierId())) {
                return false;
            }
            supplier.setSupplierCode(supplierCode);
        }
        if (StringUtils.hasText(supplier.getTaxNo()) && !MdmValueSupport.isValidTaxNo(supplier.getTaxNo())) {
            return false;
        }
        supplier.setSupplierName(MdmValueSupport.trimToNull(supplier.getSupplierName()));
        supplier.setShortName(MdmValueSupport.trimToNull(supplier.getShortName()));
        supplier.setSupplyCategory(MdmValueSupport.trimToNull(supplier.getSupplyCategory()));
        supplier.setTaxNo(MdmValueSupport.trimToNull(supplier.getTaxNo()));
        supplier.setDefaultCurrency(MdmValueSupport.trimToNull(supplier.getDefaultCurrency()));
        supplier.setQualityLevel(MdmValueSupport.trimToNull(supplier.getQualityLevel()));
        supplier.setBankAccountInfo(MdmValueSupport.trimToNull(supplier.getBankAccountInfo()));
        supplier.setContactName(MdmValueSupport.trimToNull(supplier.getContactName()));
        supplier.setContactPhone(MdmValueSupport.trimToNull(supplier.getContactPhone()));
        supplier.setContactEmail(MdmValueSupport.trimToNull(supplier.getContactEmail()));
        supplier.setAddress(MdmValueSupport.trimToNull(supplier.getAddress()));
        String newStatus = MdmStatusSupport.normalizeStatusForUpdate(supplier.getStatus(), existed.getStatus());
        supplier.setStatus(newStatus);
        supplier.setVersionNo(MdmValueSupport.resolveNextVersionNo(existed.getVersionNo()));
        if (MdmStatusSupport.isActive(newStatus) && existed.getEffectiveTime() == null) {
            supplier.setEffectiveTime(new Date());
        }
        supplier.setUpdateBy(resolveOperator());
        supplier.setUpdateTime(new Date());
        boolean updated = updateById(supplier);
        if (updated) {
            MdmSupplier after = getById(supplier.getSupplierId());
            auditTrailService.record(MdmDomainTypeSupport.SUPPLIER,
                    supplier.getSupplierId(),
                    MdmChangeTypeSupport.UPDATE,
                    after == null ? supplier.getVersionNo() : after.getVersionNo(),
                    after == null ? supplier.getStatus() : after.getStatus(),
                    before,
                    after);
        }
        return updated;
    }

    /**
     * 停用供应商。
     *
     * @param supplierId 供应商ID
     * @return true 表示成功
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean disableSupplier(Long supplierId) {
        if (supplierId == null) {
            return false;
        }
        MdmSupplier existed = getOne(new LambdaQueryWrapper<MdmSupplier>()
                .eq(MdmSupplier::getSupplierId, supplierId)
                .eq(MdmSupplier::getDelFlag, DEL_FLAG_EXIST));
        if (existed == null) {
            return false;
        }
        if (MdmStatusSupport.DISABLED.equals(existed.getStatus())) {
            return true;
        }
        MdmSupplier updateEntity = new MdmSupplier();
        updateEntity.setSupplierId(supplierId);
        updateEntity.setStatus(MdmStatusSupport.DISABLED);
        updateEntity.setVersionNo(MdmValueSupport.resolveNextVersionNo(existed.getVersionNo()));
        updateEntity.setUpdateBy(resolveOperator());
        updateEntity.setUpdateTime(new Date());
        boolean updated = updateById(updateEntity);
        if (updated) {
            MdmSupplier after = getById(supplierId);
            auditTrailService.record(MdmDomainTypeSupport.SUPPLIER,
                    supplierId,
                    MdmChangeTypeSupport.STATUS,
                    after == null ? updateEntity.getVersionNo() : after.getVersionNo(),
                    after == null ? updateEntity.getStatus() : after.getStatus(),
                    existed,
                    after);
        }
        return updated;
    }

    /**
     * 删除供应商（逻辑删除）。
     *
     * @param supplierId 供应商ID
     * @return true 表示成功
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean removeSupplier(Long supplierId) {
        if (supplierId == null) {
            return false;
        }
        MdmSupplier existed = getOne(new LambdaQueryWrapper<MdmSupplier>()
                .eq(MdmSupplier::getSupplierId, supplierId)
                .eq(MdmSupplier::getDelFlag, DEL_FLAG_EXIST));
        if (existed == null || !MdmStatusSupport.isDraft(existed.getStatus())) {
            return false;
        }
        MdmSupplier updateEntity = new MdmSupplier();
        updateEntity.setSupplierId(supplierId);
        updateEntity.setDelFlag(DEL_FLAG_DELETED);
        updateEntity.setVersionNo(MdmValueSupport.resolveNextVersionNo(existed.getVersionNo()));
        updateEntity.setUpdateBy(resolveOperator());
        updateEntity.setUpdateTime(new Date());
        boolean updated = updateById(updateEntity);
        if (updated) {
            auditTrailService.record(MdmDomainTypeSupport.SUPPLIER,
                    supplierId,
                    MdmChangeTypeSupport.DELETE,
                    updateEntity.getVersionNo(),
                    existed.getStatus(),
                    existed,
                    null);
        }
        return updated;
    }

    /**
     * 判断供应商编码是否存在。
     *
     * @param supplierCode 供应商编码
     * @param excludeId    排除ID
     * @return true 表示已存在
     */
    private boolean existsSupplierCode(String supplierCode, Long excludeId) {
        LambdaQueryWrapper<MdmSupplier> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(MdmSupplier::getSupplierCode, supplierCode);
        queryWrapper.eq(MdmSupplier::getDelFlag, DEL_FLAG_EXIST);
        if (excludeId != null) {
            queryWrapper.ne(MdmSupplier::getSupplierId, excludeId);
        }
        return count(queryWrapper) > 0;
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

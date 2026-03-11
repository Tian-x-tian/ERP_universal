package com.erp.system.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.erp.system.domain.MdmCustomer;
import com.erp.system.mapper.MdmCustomerMapper;
import com.erp.system.security.service.SecurityUserResolver;
import com.erp.system.service.IMdmAuditTrailService;
import com.erp.system.service.IMdmCustomerService;
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
 * MDM 客户主数据服务实现。
 */
@Service
public class MdmCustomerServiceImpl extends ServiceImpl<MdmCustomerMapper, MdmCustomer> implements IMdmCustomerService {
    private static final String DEL_FLAG_EXIST = "0";
    private static final String DEL_FLAG_DELETED = "2";
    private static final String DEFAULT_OPERATOR = "system";

    private final IMdmAuditTrailService auditTrailService;
    private final SecurityUserResolver securityUserResolver;

    public MdmCustomerServiceImpl(IMdmAuditTrailService auditTrailService, SecurityUserResolver securityUserResolver) {
        this.auditTrailService = auditTrailService;
        this.securityUserResolver = securityUserResolver;
    }

    /**
     * 查询客户列表。
     *
     * @param customerCode 客户编码
     * @param customerName 客户名称
     * @param status       状态
     * @return 客户列表
     */
    @Override
    public List<MdmCustomer> selectCustomerList(String customerCode, String customerName, String status) {
        LambdaQueryWrapper<MdmCustomer> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(MdmCustomer::getDelFlag, DEL_FLAG_EXIST);
        if (StringUtils.hasText(customerCode)) {
            queryWrapper.like(MdmCustomer::getCustomerCode, customerCode.trim());
        }
        if (StringUtils.hasText(customerName)) {
            queryWrapper.like(MdmCustomer::getCustomerName, customerName.trim());
        }
        if (StringUtils.hasText(status)) {
            queryWrapper.eq(MdmCustomer::getStatus, MdmStatusSupport.normalizeStatus(status));
        }
        queryWrapper.orderByDesc(MdmCustomer::getUpdateTime).orderByDesc(MdmCustomer::getCreateTime);
        return list(queryWrapper);
    }

    /**
     * 新增客户。
     *
     * @param customer 客户对象
     * @return true 表示新增成功
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean createCustomer(MdmCustomer customer) {
        if (customer == null || !StringUtils.hasText(customer.getCustomerCode())
                || !StringUtils.hasText(customer.getCustomerName())) {
            return false;
        }
        String tenantId = TenantWriteGuard.currentTenantId();
        if (!StringUtils.hasText(tenantId)) {
            return false;
        }
        String customerCode = customer.getCustomerCode().trim();
        if (existsCustomerCode(customerCode, null) || !MdmValueSupport.isValidTaxNo(customer.getTaxNo())) {
            return false;
        }
        Date now = new Date();
        String operator = resolveOperator();
        customer.setTenantId(tenantId);
        customer.setCustomerCode(customerCode);
        customer.setCustomerName(customer.getCustomerName().trim());
        customer.setShortName(MdmValueSupport.trimToNull(customer.getShortName()));
        customer.setCustomerType(MdmValueSupport.trimToNull(customer.getCustomerType()));
        customer.setTaxNo(MdmValueSupport.trimToNull(customer.getTaxNo()));
        customer.setInvoiceTitle(MdmValueSupport.trimToNull(customer.getInvoiceTitle()));
        customer.setDefaultCurrency(MdmValueSupport.trimToNull(customer.getDefaultCurrency()));
        customer.setContactName(MdmValueSupport.trimToNull(customer.getContactName()));
        customer.setContactPhone(MdmValueSupport.trimToNull(customer.getContactPhone()));
        customer.setContactEmail(MdmValueSupport.trimToNull(customer.getContactEmail()));
        customer.setProvince(MdmValueSupport.trimToNull(customer.getProvince()));
        customer.setCity(MdmValueSupport.trimToNull(customer.getCity()));
        customer.setDistrict(MdmValueSupport.trimToNull(customer.getDistrict()));
        customer.setDetailAddress(MdmValueSupport.trimToNull(customer.getDetailAddress()));
        customer.setStatus(MdmStatusSupport.normalizeStatus(customer.getStatus()));
        customer.setVersionNo(1);
        customer.setDelFlag(DEL_FLAG_EXIST);
        customer.setCreateBy(operator);
        customer.setUpdateBy(operator);
        customer.setCreateTime(now);
        customer.setUpdateTime(now);
        if (MdmStatusSupport.isActive(customer.getStatus())) {
            customer.setEffectiveTime(now);
        }
        boolean saved = save(customer);
        if (saved) {
            auditTrailService.record(MdmDomainTypeSupport.CUSTOMER,
                    customer.getCustomerId(),
                    MdmChangeTypeSupport.CREATE,
                    customer.getVersionNo(),
                    customer.getStatus(),
                    null,
                    customer);
        }
        return saved;
    }

    /**
     * 修改客户。
     *
     * @param customer 客户对象
     * @return true 表示修改成功
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean updateCustomer(MdmCustomer customer) {
        if (customer == null || customer.getCustomerId() == null) {
            return false;
        }
        MdmCustomer existedCustomer = getOne(new LambdaQueryWrapper<MdmCustomer>()
                .eq(MdmCustomer::getCustomerId, customer.getCustomerId())
                .eq(MdmCustomer::getDelFlag, DEL_FLAG_EXIST));
        if (existedCustomer == null) {
            return false;
        }
        if (StringUtils.hasText(customer.getCustomerCode())) {
            String customerCode = customer.getCustomerCode().trim();
            if (existsCustomerCode(customerCode, customer.getCustomerId())) {
                return false;
            }
            customer.setCustomerCode(customerCode);
        }
        if (StringUtils.hasText(customer.getTaxNo()) && !MdmValueSupport.isValidTaxNo(customer.getTaxNo())) {
            return false;
        }
        MdmCustomer before = new MdmCustomer();
        BeanUtils.copyProperties(existedCustomer, before);

        customer.setCustomerName(MdmValueSupport.trimToNull(customer.getCustomerName()));
        customer.setShortName(MdmValueSupport.trimToNull(customer.getShortName()));
        customer.setCustomerType(MdmValueSupport.trimToNull(customer.getCustomerType()));
        customer.setTaxNo(MdmValueSupport.trimToNull(customer.getTaxNo()));
        customer.setInvoiceTitle(MdmValueSupport.trimToNull(customer.getInvoiceTitle()));
        customer.setDefaultCurrency(MdmValueSupport.trimToNull(customer.getDefaultCurrency()));
        customer.setContactName(MdmValueSupport.trimToNull(customer.getContactName()));
        customer.setContactPhone(MdmValueSupport.trimToNull(customer.getContactPhone()));
        customer.setContactEmail(MdmValueSupport.trimToNull(customer.getContactEmail()));
        customer.setProvince(MdmValueSupport.trimToNull(customer.getProvince()));
        customer.setCity(MdmValueSupport.trimToNull(customer.getCity()));
        customer.setDistrict(MdmValueSupport.trimToNull(customer.getDistrict()));
        customer.setDetailAddress(MdmValueSupport.trimToNull(customer.getDetailAddress()));
        String newStatus = MdmStatusSupport.normalizeStatusForUpdate(customer.getStatus(), existedCustomer.getStatus());
        customer.setStatus(newStatus);
        customer.setVersionNo(MdmValueSupport.resolveNextVersionNo(existedCustomer.getVersionNo()));
        if (MdmStatusSupport.isActive(newStatus) && existedCustomer.getEffectiveTime() == null) {
            customer.setEffectiveTime(new Date());
        }
        customer.setUpdateBy(resolveOperator());
        customer.setUpdateTime(new Date());
        boolean updated = updateById(customer);
        if (updated) {
            MdmCustomer after = getById(customer.getCustomerId());
            auditTrailService.record(MdmDomainTypeSupport.CUSTOMER,
                    customer.getCustomerId(),
                    MdmChangeTypeSupport.UPDATE,
                    after == null ? customer.getVersionNo() : after.getVersionNo(),
                    after == null ? customer.getStatus() : after.getStatus(),
                    before,
                    after);
        }
        return updated;
    }

    /**
     * 停用客户。
     *
     * @param customerId 客户ID
     * @return true 表示停用成功
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean disableCustomer(Long customerId) {
        if (customerId == null) {
            return false;
        }
        MdmCustomer existedCustomer = getOne(new LambdaQueryWrapper<MdmCustomer>()
                .eq(MdmCustomer::getCustomerId, customerId)
                .eq(MdmCustomer::getDelFlag, DEL_FLAG_EXIST));
        if (existedCustomer == null) {
            return false;
        }
        if (MdmStatusSupport.DISABLED.equals(existedCustomer.getStatus())) {
            return true;
        }
        MdmCustomer updateEntity = new MdmCustomer();
        updateEntity.setCustomerId(customerId);
        updateEntity.setStatus(MdmStatusSupport.DISABLED);
        updateEntity.setVersionNo(MdmValueSupport.resolveNextVersionNo(existedCustomer.getVersionNo()));
        updateEntity.setUpdateBy(resolveOperator());
        updateEntity.setUpdateTime(new Date());
        boolean updated = updateById(updateEntity);
        if (updated) {
            MdmCustomer after = getById(customerId);
            auditTrailService.record(MdmDomainTypeSupport.CUSTOMER,
                    customerId,
                    MdmChangeTypeSupport.STATUS,
                    after == null ? updateEntity.getVersionNo() : after.getVersionNo(),
                    after == null ? updateEntity.getStatus() : after.getStatus(),
                    existedCustomer,
                    after);
        }
        return updated;
    }

    /**
     * 删除客户（逻辑删除）。
     *
     * @param customerId 客户ID
     * @return true 表示删除成功
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean removeCustomer(Long customerId) {
        if (customerId == null) {
            return false;
        }
        MdmCustomer existedCustomer = getOne(new LambdaQueryWrapper<MdmCustomer>()
                .eq(MdmCustomer::getCustomerId, customerId)
                .eq(MdmCustomer::getDelFlag, DEL_FLAG_EXIST));
        if (existedCustomer == null || !MdmStatusSupport.isDraft(existedCustomer.getStatus())) {
            return false;
        }
        MdmCustomer updateEntity = new MdmCustomer();
        updateEntity.setCustomerId(customerId);
        updateEntity.setDelFlag(DEL_FLAG_DELETED);
        updateEntity.setVersionNo(MdmValueSupport.resolveNextVersionNo(existedCustomer.getVersionNo()));
        updateEntity.setUpdateBy(resolveOperator());
        updateEntity.setUpdateTime(new Date());
        boolean updated = updateById(updateEntity);
        if (updated) {
            auditTrailService.record(MdmDomainTypeSupport.CUSTOMER,
                    customerId,
                    MdmChangeTypeSupport.DELETE,
                    updateEntity.getVersionNo(),
                    existedCustomer.getStatus(),
                    existedCustomer,
                    null);
        }
        return updated;
    }

    /**
     * 判断客户编码是否已存在。
     *
     * @param customerCode 客户编码
     * @param excludeBizId 排除客户ID
     * @return true 表示已存在
     */
    private boolean existsCustomerCode(String customerCode, Long excludeBizId) {
        if (!StringUtils.hasText(customerCode)) {
            return false;
        }
        LambdaQueryWrapper<MdmCustomer> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(MdmCustomer::getCustomerCode, customerCode.trim());
        queryWrapper.eq(MdmCustomer::getDelFlag, DEL_FLAG_EXIST);
        if (excludeBizId != null) {
            queryWrapper.ne(MdmCustomer::getCustomerId, excludeBizId);
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

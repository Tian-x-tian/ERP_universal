package com.erp.system.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.erp.system.domain.MdmEmployee;
import com.erp.system.domain.MdmOrg;
import com.erp.system.domain.MdmWarehouse;
import com.erp.system.mapper.MdmEmployeeMapper;
import com.erp.system.mapper.MdmOrgMapper;
import com.erp.system.mapper.MdmWarehouseMapper;
import com.erp.system.security.service.SecurityUserResolver;
import com.erp.system.service.IMdmAuditTrailService;
import com.erp.system.service.IMdmWarehouseService;
import com.erp.system.service.IMdmReferenceCheckService;
import com.erp.system.support.MdmChangeTypeSupport;
import com.erp.system.support.MdmDomainTypeSupport;
import com.erp.system.support.MdmEmployeeStatusSupport;
import com.erp.system.support.MdmOptimisticLockSupport;
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
 * MDM 仓库主数据服务实现。
 */
@Service
public class MdmWarehouseServiceImpl extends ServiceImpl<MdmWarehouseMapper, MdmWarehouse>
        implements IMdmWarehouseService {
    private static final String DEL_FLAG_EXIST = "0";
    private static final String DEL_FLAG_DELETED = "2";
    private static final String DEFAULT_OPERATOR = "system";

    private final IMdmAuditTrailService auditTrailService;
    private final SecurityUserResolver securityUserResolver;
    private final MdmOrgMapper orgMapper;
    private final MdmEmployeeMapper employeeMapper;
    private final IMdmReferenceCheckService referenceCheckService;

    public MdmWarehouseServiceImpl(IMdmAuditTrailService auditTrailService,
            SecurityUserResolver securityUserResolver,
            MdmOrgMapper orgMapper,
            MdmEmployeeMapper employeeMapper,
            IMdmReferenceCheckService referenceCheckService) {
        this.auditTrailService = auditTrailService;
        this.securityUserResolver = securityUserResolver;
        this.orgMapper = orgMapper;
        this.employeeMapper = employeeMapper;
        this.referenceCheckService = referenceCheckService;
    }

    /**
     * 查询仓库列表。
     *
     * @param whCode 仓库编码
     * @param whName 仓库名称
     * @param status 状态
     * @return 仓库列表
     */
    @Override
    public List<MdmWarehouse> selectWarehouseList(String whCode, String whName, String status) {
        LambdaQueryWrapper<MdmWarehouse> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(MdmWarehouse::getDelFlag, DEL_FLAG_EXIST);
        if (StringUtils.hasText(whCode)) {
            queryWrapper.like(MdmWarehouse::getWhCode, whCode.trim());
        }
        if (StringUtils.hasText(whName)) {
            queryWrapper.like(MdmWarehouse::getWhName, whName.trim());
        }
        if (StringUtils.hasText(status)) {
            queryWrapper.eq(MdmWarehouse::getStatus, MdmStatusSupport.normalizeStatus(status));
        }
        queryWrapper.orderByDesc(MdmWarehouse::getUpdateTime).orderByDesc(MdmWarehouse::getCreateTime);
        return list(queryWrapper);
    }

    /**
     * 新增仓库。
     *
     * @param warehouse 仓库对象
     * @return true 表示成功
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean createWarehouse(MdmWarehouse warehouse) {
        if (warehouse == null || !StringUtils.hasText(warehouse.getWhCode())
                || !StringUtils.hasText(warehouse.getWhName())) {
            return false;
        }
        String tenantId = TenantWriteGuard.currentTenantId();
        if (!StringUtils.hasText(tenantId)) {
            return false;
        }
        String whCode = warehouse.getWhCode().trim();
        if (existsWarehouseCode(whCode, null)) {
            return false;
        }
        if (!isOrgValid(warehouse.getOrgId()) || !isManagerValid(warehouse.getManagerEmpId())) {
            return false;
        }
        if (!isOrgValid(warehouse.getAccountingOrgId())) {
            return false;
        }
        String operator = resolveOperator();
        Date now = new Date();
        warehouse.setTenantId(tenantId);
        warehouse.setWhCode(whCode);
        warehouse.setWhName(warehouse.getWhName().trim());
        warehouse.setWhType(MdmValueSupport.trimToNull(warehouse.getWhType()));
        warehouse.setAddress(MdmValueSupport.trimToNull(warehouse.getAddress()));
        warehouse.setAllowNegativeStock(MdmValueSupport.normalizeYN(warehouse.getAllowNegativeStock(), "N"));
        warehouse.setTemperatureZone(MdmValueSupport.trimToNull(warehouse.getTemperatureZone()));
        warehouse.setHazardousFlag(MdmValueSupport.normalizeYN(warehouse.getHazardousFlag(), "N"));
        warehouse.setLocationCodePrefix(MdmValueSupport.trimToNull(warehouse.getLocationCodePrefix()));
        warehouse.setStatus(MdmStatusSupport.DRAFT);
        warehouse.setVersionNo(1);
        warehouse.setDelFlag(DEL_FLAG_EXIST);
        warehouse.setCreateBy(operator);
        warehouse.setUpdateBy(operator);
        warehouse.setCreateTime(now);
        warehouse.setUpdateTime(now);
        warehouse.setEffectiveTime(null);
        boolean saved = save(warehouse);
        if (saved) {
            auditTrailService.record(MdmDomainTypeSupport.WAREHOUSE,
                    warehouse.getWarehouseId(),
                    MdmChangeTypeSupport.CREATE,
                    warehouse.getVersionNo(),
                    warehouse.getStatus(),
                    null,
                    warehouse);
        }
        return saved;
    }

    /**
     * 修改仓库。
     *
     * @param warehouse 仓库对象
     * @return true 表示成功
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean updateWarehouse(MdmWarehouse warehouse) {
        if (warehouse == null || warehouse.getWarehouseId() == null) {
            return false;
        }
        MdmWarehouse existed = getOne(new LambdaQueryWrapper<MdmWarehouse>()
                .eq(MdmWarehouse::getWarehouseId, warehouse.getWarehouseId())
                .eq(MdmWarehouse::getDelFlag, DEL_FLAG_EXIST));
        if (existed == null) {
            return false;
        }
        if (MdmStatusSupport.isSubmitted(existed.getStatus())) {
            throw new IllegalStateException("仓库审批中，暂不允许直接修改");
        }
        if (!MdmStatusSupport.isDraft(existed.getStatus())) {
            throw new IllegalStateException("已生效仓库请通过审批流程提交变更");
        }
        Integer expectedVersionNo = MdmOptimisticLockSupport.requireVersion(
                warehouse.getVersionNo(),
                existed.getVersionNo(),
                "仓库");
        MdmWarehouse before = new MdmWarehouse();
        BeanUtils.copyProperties(existed, before);

        if (StringUtils.hasText(warehouse.getWhCode())) {
            String whCode = warehouse.getWhCode().trim();
            if (existsWarehouseCode(whCode, warehouse.getWarehouseId())) {
                return false;
            }
            warehouse.setWhCode(whCode);
        }
        Long orgId = warehouse.getOrgId() == null ? existed.getOrgId() : warehouse.getOrgId();
        Long accountingOrgId = warehouse.getAccountingOrgId() == null ? existed.getAccountingOrgId() : warehouse.getAccountingOrgId();
        Long managerEmpId = warehouse.getManagerEmpId() == null ? existed.getManagerEmpId()
                : warehouse.getManagerEmpId();
        if (!isOrgValid(orgId) || !isOrgValid(accountingOrgId) || !isManagerValid(managerEmpId)) {
            return false;
        }
        warehouse.setWhName(MdmValueSupport.trimToNull(warehouse.getWhName()));
        warehouse.setWhType(MdmValueSupport.trimToNull(warehouse.getWhType()));
        warehouse.setAddress(MdmValueSupport.trimToNull(warehouse.getAddress()));
        warehouse.setAllowNegativeStock(MdmValueSupport.normalizeYN(warehouse.getAllowNegativeStock(),
                existed.getAllowNegativeStock()));
        warehouse.setTemperatureZone(MdmValueSupport.trimToNull(warehouse.getTemperatureZone()));
        warehouse.setHazardousFlag(MdmValueSupport.normalizeYN(warehouse.getHazardousFlag(), existed.getHazardousFlag()));
        warehouse.setLocationCodePrefix(MdmValueSupport.trimToNull(warehouse.getLocationCodePrefix()));
        String newStatus = MdmStatusSupport.normalizeStatusForUpdate(warehouse.getStatus(), existed.getStatus());
        warehouse.setStatus(newStatus);
        warehouse.setVersionNo(MdmValueSupport.resolveNextVersionNo(existed.getVersionNo()));
        if (MdmStatusSupport.isActive(newStatus) && existed.getEffectiveTime() == null) {
            warehouse.setEffectiveTime(new Date());
        }
        warehouse.setUpdateBy(resolveOperator());
        warehouse.setUpdateTime(new Date());
        boolean updated = updateWarehouseByVersion(warehouse, expectedVersionNo);
        if (updated) {
            MdmWarehouse after = getById(warehouse.getWarehouseId());
            auditTrailService.record(MdmDomainTypeSupport.WAREHOUSE,
                    warehouse.getWarehouseId(),
                    MdmChangeTypeSupport.UPDATE,
                    after == null ? warehouse.getVersionNo() : after.getVersionNo(),
                    after == null ? warehouse.getStatus() : after.getStatus(),
                    before,
                    after);
        }
        return updated;
    }

    /**
     * 停用仓库。
     *
     * @param warehouseId 仓库ID
     * @return true 表示成功
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean disableWarehouse(Long warehouseId, Integer versionNo) {
        if (warehouseId == null) {
            return false;
        }

        referenceCheckService.check(MdmDomainTypeSupport.WAREHOUSE, warehouseId);

        MdmWarehouse existed = getOne(new LambdaQueryWrapper<MdmWarehouse>()
                .eq(MdmWarehouse::getWarehouseId, warehouseId)
                .eq(MdmWarehouse::getDelFlag, DEL_FLAG_EXIST));
        if (existed == null) {
            return false;
        }
        if (MdmStatusSupport.isSubmitted(existed.getStatus())) {
            throw new IllegalStateException("仓库审批中，暂不允许直接停用");
        }
        if (MdmStatusSupport.isActive(existed.getStatus())) {
            throw new IllegalStateException("已生效仓库请通过审批流程提交停用");
        }
        if (MdmStatusSupport.DISABLED.equals(existed.getStatus())) {
            return true;
        }
        Integer expectedVersionNo = MdmOptimisticLockSupport.requireVersion(versionNo, existed.getVersionNo(), "仓库");
        MdmWarehouse updateEntity = new MdmWarehouse();
        updateEntity.setWarehouseId(warehouseId);
        updateEntity.setStatus(MdmStatusSupport.DISABLED);
        updateEntity.setVersionNo(MdmValueSupport.resolveNextVersionNo(existed.getVersionNo()));
        updateEntity.setUpdateBy(resolveOperator());
        updateEntity.setUpdateTime(new Date());
        boolean updated = updateWarehouseByVersion(updateEntity, expectedVersionNo);
        if (updated) {
            MdmWarehouse after = getById(warehouseId);
            auditTrailService.record(MdmDomainTypeSupport.WAREHOUSE,
                    warehouseId,
                    MdmChangeTypeSupport.STATUS,
                    after == null ? updateEntity.getVersionNo() : after.getVersionNo(),
                    after == null ? updateEntity.getStatus() : after.getStatus(),
                    existed,
                    after);
        }
        return updated;
    }

    /**
     * 删除仓库（逻辑删除）。
     *
     * @param warehouseId 仓库ID
     * @return true 表示成功
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean removeWarehouse(Long warehouseId, Integer versionNo) {
        if (warehouseId == null) {
            return false;
        }

        referenceCheckService.check(MdmDomainTypeSupport.WAREHOUSE, warehouseId);

        MdmWarehouse existed = getOne(new LambdaQueryWrapper<MdmWarehouse>()
                .eq(MdmWarehouse::getWarehouseId, warehouseId)
                .eq(MdmWarehouse::getDelFlag, DEL_FLAG_EXIST));
        if (existed == null || !MdmStatusSupport.isDraft(existed.getStatus())) {
            return false;
        }
        if (MdmStatusSupport.isSubmitted(existed.getStatus())) {
            return false;
        }
        Integer expectedVersionNo = MdmOptimisticLockSupport.requireVersion(versionNo, existed.getVersionNo(), "仓库");
        MdmWarehouse updateEntity = new MdmWarehouse();
        updateEntity.setWarehouseId(warehouseId);
        updateEntity.setDelFlag(DEL_FLAG_DELETED);
        updateEntity.setVersionNo(MdmValueSupport.resolveNextVersionNo(existed.getVersionNo()));
        updateEntity.setUpdateBy(resolveOperator());
        updateEntity.setUpdateTime(new Date());
        boolean updated = updateWarehouseByVersion(updateEntity, expectedVersionNo);
        if (updated) {
            auditTrailService.record(MdmDomainTypeSupport.WAREHOUSE,
                    warehouseId,
                    MdmChangeTypeSupport.DELETE,
                    updateEntity.getVersionNo(),
                    existed.getStatus(),
                    existed,
                    null);
        }
        return updated;
    }

    /**
     * 判断仓库编码是否重复。
     *
     * @param whCode    仓库编码
     * @param excludeId 排除主键
     * @return true 表示重复
     */
    private boolean existsWarehouseCode(String whCode, Long excludeId) {
        LambdaQueryWrapper<MdmWarehouse> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(MdmWarehouse::getWhCode, whCode);
        queryWrapper.eq(MdmWarehouse::getDelFlag, DEL_FLAG_EXIST);
        if (excludeId != null) {
            queryWrapper.ne(MdmWarehouse::getWarehouseId, excludeId);
        }
        return count(queryWrapper) > 0;
    }

    /**
     * 按版本号执行乐观锁更新。
     *
     * @param warehouse        更新对象
     * @param currentVersionNo 当前版本号
     * @return true 表示更新成功
     */
    private boolean updateWarehouseByVersion(MdmWarehouse warehouse, Integer currentVersionNo) {
        if (warehouse == null || warehouse.getWarehouseId() == null) {
            return false;
        }
        LambdaUpdateWrapper<MdmWarehouse> updateWrapper = new LambdaUpdateWrapper<MdmWarehouse>()
                .eq(MdmWarehouse::getWarehouseId, warehouse.getWarehouseId())
                .eq(MdmWarehouse::getDelFlag, DEL_FLAG_EXIST);
        if (currentVersionNo != null) {
            updateWrapper.eq(MdmWarehouse::getVersionNo, currentVersionNo);
        }
        boolean updated = update(warehouse, updateWrapper);
        MdmOptimisticLockSupport.ensureUpdated(updated, "仓库");
        return true;
    }

    /**
     * 校验组织引用是否有效。
     *
     * @param orgId 组织ID
     * @return true 表示有效
     */
    private boolean isOrgValid(Long orgId) {
        if (orgId == null || orgId < 1) {
            return true;
        }
        MdmOrg org = orgMapper.selectById(orgId);
        return org != null && DEL_FLAG_EXIST.equals(org.getDelFlag());
    }

    /**
     * 校验负责人引用是否有效。
     *
     * @param managerEmpId 负责人ID
     * @return true 表示有效
     */
    private boolean isManagerValid(Long managerEmpId) {
        if (managerEmpId == null || managerEmpId < 1) {
            return true;
        }
        MdmEmployee employee = employeeMapper.selectById(managerEmpId);
        return employee != null
                && DEL_FLAG_EXIST.equals(employee.getDelFlag())
                && MdmEmployeeStatusSupport.isActive(employee.getStatus());
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

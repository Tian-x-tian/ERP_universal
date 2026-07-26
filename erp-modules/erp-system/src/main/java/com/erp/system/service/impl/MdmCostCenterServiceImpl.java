package com.erp.system.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.erp.system.domain.MdmEmployee;
import com.erp.system.domain.MdmCostCenter;
import com.erp.system.domain.MdmOrg;
import com.erp.system.mapper.MdmEmployeeMapper;
import com.erp.system.mapper.MdmCostCenterMapper;
import com.erp.system.mapper.MdmOrgMapper;
import com.erp.system.security.service.SecurityUserResolver;
import com.erp.system.service.IMdmAuditTrailService;
import com.erp.system.service.IMdmCostCenterService;
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

/**
 * MDM 成本中心主数据服务实现。
 */
@Service
public class MdmCostCenterServiceImpl extends ServiceImpl<MdmCostCenterMapper, MdmCostCenter>
        implements IMdmCostCenterService {
    private static final String DEL_FLAG_EXIST = "0";
    private static final String DEL_FLAG_DELETED = "2";
    private static final Long ROOT_PARENT_ID = 0L;
    private static final String DEFAULT_OPERATOR = "system";

    private final IMdmAuditTrailService auditTrailService;
    private final SecurityUserResolver securityUserResolver;
    private final MdmOrgMapper orgMapper;
    private final MdmEmployeeMapper employeeMapper;
    private final IMdmReferenceCheckService referenceCheckService;

    public MdmCostCenterServiceImpl(IMdmAuditTrailService auditTrailService,
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
     * 查询成本中心列表。
     *
     * @param ccCode 成本中心编码
     * @param ccName 成本中心名称
     * @param status 状态
     * @return 成本中心列表
     */
    @Override
    public List<MdmCostCenter> selectCostCenterList(String ccCode, String ccName, String status) {
        LambdaQueryWrapper<MdmCostCenter> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(MdmCostCenter::getDelFlag, DEL_FLAG_EXIST);
        if (StringUtils.hasText(ccCode)) {
            queryWrapper.like(MdmCostCenter::getCcCode, ccCode.trim());
        }
        if (StringUtils.hasText(ccName)) {
            queryWrapper.like(MdmCostCenter::getCcName, ccName.trim());
        }
        if (StringUtils.hasText(status)) {
            queryWrapper.eq(MdmCostCenter::getStatus, MdmStatusSupport.normalizeStatus(status));
        }
        queryWrapper.orderByDesc(MdmCostCenter::getUpdateTime).orderByDesc(MdmCostCenter::getCreateTime);
        return list(queryWrapper);
    }

    /**
     * 新增成本中心。
     *
     * @param costCenter 成本中心对象
     * @return true 表示成功
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean createCostCenter(MdmCostCenter costCenter) {
        if (costCenter == null || !StringUtils.hasText(costCenter.getCcCode())
                || !StringUtils.hasText(costCenter.getCcName()) || costCenter.getOrgId() == null) {
            return false;
        }
        String tenantId = TenantWriteGuard.currentTenantId();
        if (!StringUtils.hasText(tenantId)) {
            return false;
        }
        String ccCode = costCenter.getCcCode().trim();
        if (existsCostCenterCode(ccCode, null)) {
            return false;
        }
        if (!isOrgValid(costCenter.getOrgId())) {
            return false;
        }
        Long parentId = normalizeParentId(costCenter.getParentId());
        if (!isParentValid(parentId) || parentId.equals(costCenter.getCcId())) {
            return false;
        }
        Date now = new Date();
        String operator = resolveOperator();
        costCenter.setTenantId(tenantId);
        costCenter.setCcCode(ccCode);
        costCenter.setCcName(costCenter.getCcName().trim());
        costCenter.setParentId(parentId);
        costCenter.setStatus(MdmStatusSupport.DRAFT);
        costCenter.setVersionNo(1);
        costCenter.setDelFlag(DEL_FLAG_EXIST);
        boolean saved = save(costCenter);
        if (saved) {
            auditTrailService.record(MdmDomainTypeSupport.COST_CENTER,
                    costCenter.getCcId(),
                    MdmChangeTypeSupport.CREATE,
                    costCenter.getVersionNo(),
                    costCenter.getStatus(),
                    null,
                    costCenter);
        }
        return saved;
    }

    /**
     * 修改成本中心。
     *
     * @param costCenter 成本中心对象
     * @return true 表示成功
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean updateCostCenter(MdmCostCenter costCenter) {
        if (costCenter == null || costCenter.getCcId() == null) {
            return false;
        }
        MdmCostCenter existed = getOne(new LambdaQueryWrapper<MdmCostCenter>()
                .eq(MdmCostCenter::getCcId, costCenter.getCcId())
                .eq(MdmCostCenter::getDelFlag, DEL_FLAG_EXIST));
        if (existed == null) {
            return false;
        }
        if (MdmStatusSupport.isSubmitted(existed.getStatus())) {
            throw new IllegalStateException("成本中心审批中，暂不允许直接修改");
        }
        if (!MdmStatusSupport.isDraft(existed.getStatus())) {
            throw new IllegalStateException("已生效成本中心请通过审批流程提交变更");
        }
        MdmCostCenter before = new MdmCostCenter();
        BeanUtils.copyProperties(existed, before);

        if (StringUtils.hasText(costCenter.getCcCode())) {
            String ccCode = costCenter.getCcCode().trim();
            if (existsCostCenterCode(ccCode, costCenter.getCcId())) {
                return false;
            }
            costCenter.setCcCode(ccCode);
        }
        Long parentId = normalizeParentId(
                costCenter.getParentId() == null ? existed.getParentId() : costCenter.getParentId());
        if (parentId.equals(costCenter.getCcId()) || !isParentValid(parentId)
                || hasCycle(parentId, costCenter.getCcId())) {
            return false;
        }
        costCenter.setParentId(parentId);
        costCenter.setCcName(MdmValueSupport.trimToNull(costCenter.getCcName()));
        costCenter.setOrgId(costCenter.getOrgId() == null ? existed.getOrgId() : costCenter.getOrgId());
        if (!isOrgValid(costCenter.getOrgId())) {
            return false;
        }
        costCenter.setStatus(MdmStatusSupport.normalizeStatusForUpdate(costCenter.getStatus(), existed.getStatus()));
        costCenter.setVersionNo(MdmValueSupport.resolveNextVersionNo(existed.getVersionNo()));
        boolean updated = updateCostCenterByVersion(costCenter, existed.getVersionNo());
        if (updated) {
            MdmCostCenter after = getById(costCenter.getCcId());
            auditTrailService.record(MdmDomainTypeSupport.COST_CENTER,
                    costCenter.getCcId(),
                    MdmChangeTypeSupport.UPDATE,
                    after == null ? costCenter.getVersionNo() : after.getVersionNo(),
                    after == null ? costCenter.getStatus() : after.getStatus(),
                    before,
                    after);
        }
        return updated;
    }

    /**
     * 停用成本中心。
     *
     * @param ccId 成本中心ID
     * @return true 表示成功
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean disableCostCenter(Long ccId) {
        if (ccId == null) {
            return false;
        }

        referenceCheckService.check(MdmDomainTypeSupport.COST_CENTER, ccId);

        MdmCostCenter existed = getOne(new LambdaQueryWrapper<MdmCostCenter>()
                .eq(MdmCostCenter::getCcId, ccId)
                .eq(MdmCostCenter::getDelFlag, DEL_FLAG_EXIST));
        if (existed == null) {
            return false;
        }
        if (MdmStatusSupport.isSubmitted(existed.getStatus())) {
            throw new IllegalStateException("成本中心审批中，暂不允许直接停用");
        }
        if (MdmStatusSupport.isActive(existed.getStatus())) {
            throw new IllegalStateException("已生效成本中心请通过审批流程提交停用");
        }
        if (MdmStatusSupport.DISABLED.equals(existed.getStatus())) {
            return true;
        }
        MdmCostCenter updateEntity = new MdmCostCenter();
        updateEntity.setCcId(ccId);
        updateEntity.setStatus(MdmStatusSupport.DISABLED);
        updateEntity.setVersionNo(MdmValueSupport.resolveNextVersionNo(existed.getVersionNo()));
        boolean updated = updateCostCenterByVersion(updateEntity, existed.getVersionNo());
        if (updated) {
            MdmCostCenter after = getById(ccId);
            auditTrailService.record(MdmDomainTypeSupport.COST_CENTER,
                    ccId,
                    MdmChangeTypeSupport.STATUS,
                    after == null ? updateEntity.getVersionNo() : after.getVersionNo(),
                    after == null ? updateEntity.getStatus() : after.getStatus(),
                    existed,
                    after);
        }
        return updated;
    }

    /**
     * 删除成本中心（逻辑删除）。
     *
     * @param ccId 成本中心ID
     * @return true 表示成功
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean removeCostCenter(Long ccId) {
        if (ccId == null) {
            return false;
        }

        referenceCheckService.check(MdmDomainTypeSupport.COST_CENTER, ccId);

        if (isReferenced(ccId)) {
            throw new IllegalStateException("成本中心已被员工引用，不能删除");
        }
        MdmCostCenter existed = getOne(new LambdaQueryWrapper<MdmCostCenter>()
                .eq(MdmCostCenter::getCcId, ccId)
                .eq(MdmCostCenter::getDelFlag, DEL_FLAG_EXIST));
        if (existed == null || !MdmStatusSupport.isDraft(existed.getStatus())) {
            return false;
        }
        if (MdmStatusSupport.isSubmitted(existed.getStatus())) {
            return false;
        }
        if (count(new LambdaQueryWrapper<MdmCostCenter>()
                .eq(MdmCostCenter::getParentId, ccId)
                .eq(MdmCostCenter::getDelFlag, DEL_FLAG_EXIST)) > 0) {
            return false;
        }
        MdmCostCenter updateEntity = new MdmCostCenter();
        updateEntity.setCcId(ccId);
        updateEntity.setDelFlag(DEL_FLAG_DELETED);
        updateEntity.setVersionNo(MdmValueSupport.resolveNextVersionNo(existed.getVersionNo()));
        boolean updated = updateCostCenterByVersion(updateEntity, existed.getVersionNo());
        if (updated) {
            auditTrailService.record(MdmDomainTypeSupport.COST_CENTER,
                    ccId,
                    MdmChangeTypeSupport.DELETE,
                    updateEntity.getVersionNo(),
                    existed.getStatus(),
                    existed,
                    null);
        }
        return updated;
    }

    /**
     * 判断成本中心编码是否重复。
     *
     * @param ccCode    成本中心编码
     * @param excludeId 排除主键
     * @return true 表示重复
     */
    private boolean existsCostCenterCode(String ccCode, Long excludeId) {
        LambdaQueryWrapper<MdmCostCenter> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(MdmCostCenter::getCcCode, ccCode);
        queryWrapper.eq(MdmCostCenter::getDelFlag, DEL_FLAG_EXIST);
        if (excludeId != null) {
            queryWrapper.ne(MdmCostCenter::getCcId, excludeId);
        }
        return count(queryWrapper) > 0;
    }

    /**
     * 校验组织引用是否有效。
     *
     * @param orgId 组织ID
     * @return true 表示有效
     */
    private boolean isOrgValid(Long orgId) {
        if (orgId == null || orgId < 1) {
            return false;
        }
        MdmOrg org = orgMapper.selectById(orgId);
        return org != null && DEL_FLAG_EXIST.equals(org.getDelFlag());
    }

    /**
     * 按版本号执行乐观锁更新。
     *
     * @param costCenter       更新对象
     * @param currentVersionNo 当前版本号
     * @return true 表示更新成功
     */
    private boolean updateCostCenterByVersion(MdmCostCenter costCenter, Integer currentVersionNo) {
        if (costCenter == null || costCenter.getCcId() == null) {
            return false;
        }
        LambdaUpdateWrapper<MdmCostCenter> updateWrapper = new LambdaUpdateWrapper<MdmCostCenter>()
                .eq(MdmCostCenter::getCcId, costCenter.getCcId())
                .eq(MdmCostCenter::getDelFlag, DEL_FLAG_EXIST);
        if (currentVersionNo != null) {
            updateWrapper.eq(MdmCostCenter::getVersionNo, currentVersionNo);
        }
        boolean updated = update(costCenter, updateWrapper);
        if (!updated) {
            throw new IllegalStateException("成本中心数据已被其他人更新，请刷新后重试");
        }
        return updated;
    }

    /**
     * 判断成本中心是否已被员工引用。
     *
     * @param ccId 成本中心ID
     * @return true 表示已引用
     */
    private boolean isReferenced(Long ccId) {
        if (ccId == null) {
            return false;
        }
        Long employeeCount = employeeMapper.selectCount(new LambdaQueryWrapper<MdmEmployee>()
                .eq(MdmEmployee::getCostCenterId, ccId)
                .eq(MdmEmployee::getDelFlag, DEL_FLAG_EXIST));
        return employeeCount != null && employeeCount > 0;
    }

    /**
     * 规范父级ID。
     *
     * @param parentId 原始父级ID
     * @return 规范父级ID
     */
    private Long normalizeParentId(Long parentId) {
        return parentId == null || parentId < 1 ? ROOT_PARENT_ID : parentId;
    }

    /**
     * 校验父级成本中心是否有效。
     *
     * @param parentId 父级成本中心ID
     * @return true 表示有效
     */
    private boolean isParentValid(Long parentId) {
        if (parentId == null || parentId.equals(ROOT_PARENT_ID)) {
            return true;
        }
        return count(new LambdaQueryWrapper<MdmCostCenter>()
                .eq(MdmCostCenter::getCcId, parentId)
                .eq(MdmCostCenter::getDelFlag, DEL_FLAG_EXIST)) > 0;
    }

    /**
     * 检查成本中心树是否出现环。
     *
     * @param parentId 父级ID
     * @param ccId     当前成本中心ID
     * @return true 表示存在环
     */
    private boolean hasCycle(Long parentId, Long ccId) {
        if (parentId == null || parentId.equals(ROOT_PARENT_ID) || ccId == null) {
            return false;
        }
        Long current = parentId;
        int guard = 0;
        while (current != null && !current.equals(ROOT_PARENT_ID) && guard < 100) {
            if (current.equals(ccId)) {
                return true;
            }
            MdmCostCenter parent = getById(current);
            if (parent == null) {
                return false;
            }
            current = normalizeParentId(parent.getParentId());
            guard++;
        }
        return false;
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

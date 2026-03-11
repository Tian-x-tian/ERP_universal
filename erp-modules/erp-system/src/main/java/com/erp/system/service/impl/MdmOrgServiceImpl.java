package com.erp.system.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.erp.system.domain.MdmOrg;
import com.erp.system.mapper.MdmOrgMapper;
import com.erp.system.security.service.SecurityUserResolver;
import com.erp.system.service.IMdmAuditTrailService;
import com.erp.system.service.IMdmOrgService;
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
 * MDM 组织主数据服务实现。
 */
@Service
public class MdmOrgServiceImpl extends ServiceImpl<MdmOrgMapper, MdmOrg> implements IMdmOrgService {
    private static final String DEL_FLAG_EXIST = "0";
    private static final String DEL_FLAG_DELETED = "2";
    private static final Long ROOT_PARENT_ID = 0L;
    private static final String ROOT_ANCESTORS = "0";
    private static final String DEFAULT_OPERATOR = "system";

    private final IMdmAuditTrailService auditTrailService;
    private final SecurityUserResolver securityUserResolver;

    public MdmOrgServiceImpl(IMdmAuditTrailService auditTrailService, SecurityUserResolver securityUserResolver) {
        this.auditTrailService = auditTrailService;
        this.securityUserResolver = securityUserResolver;
    }

    /**
     * 查询组织列表。
     *
     * @param orgCode 组织编码
     * @param orgName 组织名称
     * @param status  状态
     * @return 组织列表
     */
    @Override
    public List<MdmOrg> selectOrgList(String orgCode, String orgName, String status) {
        LambdaQueryWrapper<MdmOrg> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(MdmOrg::getDelFlag, DEL_FLAG_EXIST);
        if (StringUtils.hasText(orgCode)) {
            queryWrapper.like(MdmOrg::getOrgCode, orgCode.trim());
        }
        if (StringUtils.hasText(orgName)) {
            queryWrapper.like(MdmOrg::getOrgName, orgName.trim());
        }
        if (StringUtils.hasText(status)) {
            queryWrapper.eq(MdmOrg::getStatus, MdmStatusSupport.normalizeStatus(status));
        }
        queryWrapper.orderByAsc(MdmOrg::getAncestors).orderByAsc(MdmOrg::getOrgId);
        return list(queryWrapper);
    }

    /**
     * 新增组织。
     *
     * @param org 组织对象
     * @return true 表示成功
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean createOrg(MdmOrg org) {
        if (org == null || !StringUtils.hasText(org.getOrgCode()) || !StringUtils.hasText(org.getOrgName())) {
            return false;
        }
        String tenantId = TenantWriteGuard.currentTenantId();
        if (!StringUtils.hasText(tenantId)) {
            return false;
        }
        String orgCode = org.getOrgCode().trim();
        if (existsOrgCode(orgCode, null)) {
            return false;
        }
        Long parentId = normalizeParentId(org.getParentId());
        if (!isParentValid(parentId)) {
            return false;
        }

        Date now = new Date();
        String operator = resolveOperator();
        org.setTenantId(tenantId);
        org.setOrgCode(orgCode);
        org.setOrgName(org.getOrgName().trim());
        org.setOrgType(MdmValueSupport.trimToNull(org.getOrgType()));
        org.setParentId(parentId);
        org.setAncestors(buildAncestors(parentId));
        org.setStatus(MdmStatusSupport.normalizeStatus(org.getStatus()));
        org.setVersionNo(1);
        org.setDelFlag(DEL_FLAG_EXIST);
        org.setCreateBy(operator);
        org.setUpdateBy(operator);
        org.setCreateTime(now);
        org.setUpdateTime(now);
        boolean saved = save(org);
        if (saved) {
            auditTrailService.record(MdmDomainTypeSupport.ORG,
                    org.getOrgId(),
                    MdmChangeTypeSupport.CREATE,
                    org.getVersionNo(),
                    org.getStatus(),
                    null,
                    org);
        }
        return saved;
    }

    /**
     * 修改组织。
     *
     * @param org 组织对象
     * @return true 表示成功
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean updateOrg(MdmOrg org) {
        if (org == null || org.getOrgId() == null) {
            return false;
        }
        MdmOrg existed = getOne(new LambdaQueryWrapper<MdmOrg>()
                .eq(MdmOrg::getOrgId, org.getOrgId())
                .eq(MdmOrg::getDelFlag, DEL_FLAG_EXIST));
        if (existed == null) {
            return false;
        }
        MdmOrg before = new MdmOrg();
        BeanUtils.copyProperties(existed, before);

        if (StringUtils.hasText(org.getOrgCode())) {
            String orgCode = org.getOrgCode().trim();
            if (existsOrgCode(orgCode, org.getOrgId())) {
                return false;
            }
            org.setOrgCode(orgCode);
        }
        Long parentId = normalizeParentId(org.getParentId() == null ? existed.getParentId() : org.getParentId());
        if (parentId.equals(org.getOrgId()) || !isParentValid(parentId) || hasCycle(parentId, org.getOrgId())) {
            return false;
        }
        org.setParentId(parentId);
        org.setAncestors(buildAncestors(parentId));
        org.setOrgName(MdmValueSupport.trimToNull(org.getOrgName()));
        org.setOrgType(MdmValueSupport.trimToNull(org.getOrgType()));
        org.setStatus(MdmStatusSupport.normalizeStatusForUpdate(org.getStatus(), existed.getStatus()));
        org.setVersionNo(MdmValueSupport.resolveNextVersionNo(existed.getVersionNo()));
        org.setUpdateBy(resolveOperator());
        org.setUpdateTime(new Date());
        boolean updated = updateById(org);
        if (updated) {
            MdmOrg after = getById(org.getOrgId());
            auditTrailService.record(MdmDomainTypeSupport.ORG,
                    org.getOrgId(),
                    MdmChangeTypeSupport.UPDATE,
                    after == null ? org.getVersionNo() : after.getVersionNo(),
                    after == null ? org.getStatus() : after.getStatus(),
                    before,
                    after);
        }
        return updated;
    }

    /**
     * 停用组织。
     *
     * @param orgId 组织ID
     * @return true 表示成功
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean disableOrg(Long orgId) {
        if (orgId == null) {
            return false;
        }
        MdmOrg existed = getOne(new LambdaQueryWrapper<MdmOrg>()
                .eq(MdmOrg::getOrgId, orgId)
                .eq(MdmOrg::getDelFlag, DEL_FLAG_EXIST));
        if (existed == null) {
            return false;
        }
        if (MdmStatusSupport.DISABLED.equals(existed.getStatus())) {
            return true;
        }
        MdmOrg updateEntity = new MdmOrg();
        updateEntity.setOrgId(orgId);
        updateEntity.setStatus(MdmStatusSupport.DISABLED);
        updateEntity.setVersionNo(MdmValueSupport.resolveNextVersionNo(existed.getVersionNo()));
        updateEntity.setUpdateBy(resolveOperator());
        updateEntity.setUpdateTime(new Date());
        boolean updated = updateById(updateEntity);
        if (updated) {
            MdmOrg after = getById(orgId);
            auditTrailService.record(MdmDomainTypeSupport.ORG,
                    orgId,
                    MdmChangeTypeSupport.STATUS,
                    after == null ? updateEntity.getVersionNo() : after.getVersionNo(),
                    after == null ? updateEntity.getStatus() : after.getStatus(),
                    existed,
                    after);
        }
        return updated;
    }

    /**
     * 删除组织（逻辑删除）。
     *
     * @param orgId 组织ID
     * @return true 表示成功
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean removeOrg(Long orgId) {
        if (orgId == null) {
            return false;
        }
        MdmOrg existed = getOne(new LambdaQueryWrapper<MdmOrg>()
                .eq(MdmOrg::getOrgId, orgId)
                .eq(MdmOrg::getDelFlag, DEL_FLAG_EXIST));
        if (existed == null || !MdmStatusSupport.isDraft(existed.getStatus())) {
            return false;
        }
        if (count(new LambdaQueryWrapper<MdmOrg>()
                .eq(MdmOrg::getParentId, orgId)
                .eq(MdmOrg::getDelFlag, DEL_FLAG_EXIST)) > 0) {
            return false;
        }
        MdmOrg updateEntity = new MdmOrg();
        updateEntity.setOrgId(orgId);
        updateEntity.setDelFlag(DEL_FLAG_DELETED);
        updateEntity.setVersionNo(MdmValueSupport.resolveNextVersionNo(existed.getVersionNo()));
        updateEntity.setUpdateBy(resolveOperator());
        updateEntity.setUpdateTime(new Date());
        boolean updated = updateById(updateEntity);
        if (updated) {
            auditTrailService.record(MdmDomainTypeSupport.ORG,
                    orgId,
                    MdmChangeTypeSupport.DELETE,
                    updateEntity.getVersionNo(),
                    existed.getStatus(),
                    existed,
                    null);
        }
        return updated;
    }

    /**
     * 判断组织编码是否重复。
     *
     * @param orgCode 组织编码
     * @param excludeId 排除主键
     * @return true 表示重复
     */
    private boolean existsOrgCode(String orgCode, Long excludeId) {
        LambdaQueryWrapper<MdmOrg> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(MdmOrg::getOrgCode, orgCode);
        queryWrapper.eq(MdmOrg::getDelFlag, DEL_FLAG_EXIST);
        if (excludeId != null) {
            queryWrapper.ne(MdmOrg::getOrgId, excludeId);
        }
        return count(queryWrapper) > 0;
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
     * 校验父级组织是否存在。
     *
     * @param parentId 父级组织ID
     * @return true 表示有效
     */
    private boolean isParentValid(Long parentId) {
        if (parentId == null || parentId.equals(ROOT_PARENT_ID)) {
            return true;
        }
        return count(new LambdaQueryWrapper<MdmOrg>()
                .eq(MdmOrg::getOrgId, parentId)
                .eq(MdmOrg::getDelFlag, DEL_FLAG_EXIST)) > 0;
    }

    /**
     * 计算祖级路径。
     *
     * @param parentId 父级组织ID
     * @return 祖级路径
     */
    private String buildAncestors(Long parentId) {
        if (parentId == null || parentId.equals(ROOT_PARENT_ID)) {
            return ROOT_ANCESTORS;
        }
        MdmOrg parent = getById(parentId);
        if (parent == null || !StringUtils.hasText(parent.getAncestors())) {
            return ROOT_ANCESTORS + "," + parentId;
        }
        return parent.getAncestors() + "," + parentId;
    }

    /**
     * 检查组织树是否出现环。
     *
     * @param parentId 待设置父节点
     * @param orgId    当前组织ID
     * @return true 表示存在环
     */
    private boolean hasCycle(Long parentId, Long orgId) {
        if (parentId == null || parentId.equals(ROOT_PARENT_ID) || orgId == null) {
            return false;
        }
        Long current = parentId;
        int guard = 0;
        while (current != null && !current.equals(ROOT_PARENT_ID) && guard < 100) {
            if (current.equals(orgId)) {
                return true;
            }
            MdmOrg parent = getById(current);
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

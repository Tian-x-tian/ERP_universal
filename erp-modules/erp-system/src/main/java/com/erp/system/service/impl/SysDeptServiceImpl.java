package com.erp.system.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.erp.system.domain.SysDept;
import com.erp.system.mapper.SysDeptMapper;
import com.erp.system.service.ISysDeptService;
import com.erp.system.support.StatusFieldSupport;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

/**
 * 部门服务实现
 */
@Service
public class SysDeptServiceImpl extends ServiceImpl<SysDeptMapper, SysDept> implements ISysDeptService {

    private static final String DEFAULT_TENANT_ID = "000000";

    @Override
    public List<SysDept> buildDeptTree(List<SysDept> depts) {
        return buildTree(depts, 0L);
    }

    /**
     * 新增部门并自动补齐祖级链路与基础字段。
     *
     * @param dept 部门对象
     * @return 新增结果
     */
    @Override
    public boolean createDept(SysDept dept) {
        if (dept == null || !StringUtils.hasText(dept.getDeptName())) {
            return false;
        }
        Long parentId = dept.getParentId() == null ? 0L : dept.getParentId();
        dept.setParentId(parentId);
        dept.setDeptName(dept.getDeptName().trim());
        dept.setStatus(StatusFieldSupport.normalizeBinaryStatus(dept.getStatus()));
        dept.setDelFlag(StringUtils.hasText(dept.getDelFlag()) ? dept.getDelFlag() : "0");
        dept.setCreateTime(new Date());

        if (parentId == 0L) {
            dept.setAncestors("0");
            dept.setTenantId(resolveTenantId(dept.getTenantId(), null));
            return save(dept);
        }

        SysDept parentDept = getById(parentId);
        if (parentDept == null) {
            return false;
        }
        dept.setAncestors(normalizeAncestors(parentDept.getAncestors()) + "," + parentId);
        dept.setTenantId(resolveTenantId(dept.getTenantId(), parentDept.getTenantId()));

        if (dept.getCompanyId() == null) {
            dept.setCompanyId(parentDept.getCompanyId());
        }
        return save(dept);
    }

    /**
     * 修改部门并维护祖级链路与租户归属字段。
     *
     * @param dept 部门对象
     * @return 修改结果
     */
    @Override
    public boolean updateDept(SysDept dept) {
        if (dept == null || dept.getDeptId() == null) {
            return false;
        }
        SysDept existedDept = getById(dept.getDeptId());
        if (existedDept == null) {
            return false;
        }

        Long parentId = dept.getParentId() == null
                ? (existedDept.getParentId() == null ? 0L : existedDept.getParentId())
                : dept.getParentId();
        if (parentId.equals(dept.getDeptId())) {
            return false;
        }

        String oldAncestors = normalizeAncestors(existedDept.getAncestors());
        if (parentId == 0L) {
            dept.setAncestors("0");
        } else {
            SysDept parentDept = getById(parentId);
            if (parentDept == null) {
                return false;
            }
            if (containsAncestor(parentDept.getAncestors(), dept.getDeptId())) {
                return false;
            }
            dept.setAncestors(normalizeAncestors(parentDept.getAncestors()) + "," + parentId);
            if (dept.getCompanyId() == null) {
                dept.setCompanyId(parentDept.getCompanyId());
            }
            dept.setTenantId(resolveTenantId(dept.getTenantId(), parentDept.getTenantId()));
        }

        dept.setParentId(parentId);
        if (StringUtils.hasText(dept.getDeptName())) {
            dept.setDeptName(dept.getDeptName().trim());
        }
        dept.setStatus(StatusFieldSupport.normalizeBinaryStatusForUpdate(dept.getStatus(), existedDept.getStatus()));
        if (dept.getCompanyId() == null) {
            dept.setCompanyId(existedDept.getCompanyId());
        }
        dept.setTenantId(resolveTenantId(dept.getTenantId(), existedDept.getTenantId()));
        dept.setUpdateTime(new Date());

        boolean updated = super.updateById(dept);
        if (!updated) {
            return false;
        }
        refreshChildrenAncestors(dept.getDeptId(), oldAncestors, dept.getAncestors());
        return true;
    }

    /**
     * 构建树结构
     */
    private List<SysDept> buildTree(List<SysDept> depts, Long parentId) {
        List<SysDept> tree = new ArrayList<>();
        for (SysDept dept : depts) {
            if (parentId.equals(dept.getParentId())) {
                dept.setChildren(buildTree(depts, dept.getDeptId()));
                tree.add(dept);
            }
        }
        return tree;
    }

    /**
     * 判断祖级列表中是否包含指定部门ID。
     *
     * @param ancestors 祖级列表
     * @param deptId    部门ID
     * @return true 表示包含
     */
    private boolean containsAncestor(String ancestors, Long deptId) {
        if (!StringUtils.hasText(ancestors) || deptId == null) {
            return false;
        }
        return Arrays.asList(ancestors.split(",")).contains(String.valueOf(deptId));
    }

    /**
     * 刷新下级部门祖级链路，避免上级调整后链路失真。
     *
     * @param deptId       当前部门ID
     * @param oldAncestors 旧祖级
     * @param newAncestors 新祖级
     */
    private void refreshChildrenAncestors(Long deptId, String oldAncestors, String newAncestors) {
        if (deptId == null) {
            return;
        }
        String oldPrefix = normalizeAncestors(oldAncestors) + "," + deptId;
        String newPrefix = normalizeAncestors(newAncestors) + "," + deptId;
        List<SysDept> children = list(new LambdaQueryWrapper<SysDept>()
                .likeRight(SysDept::getAncestors, oldPrefix));
        for (SysDept child : children) {
            String childAncestors = child.getAncestors();
            if (!StringUtils.hasText(childAncestors) || !childAncestors.startsWith(oldPrefix)) {
                continue;
            }
            child.setAncestors(newPrefix + childAncestors.substring(oldPrefix.length()));
            child.setUpdateTime(new Date());
            super.updateById(child);
        }
    }

    /**
     * 规范祖级字符串，确保空值回退为根祖级。
     *
     * @param ancestors 原始祖级字符串
     * @return 规范化祖级字符串
     */
    private String normalizeAncestors(String ancestors) {
        return StringUtils.hasText(ancestors) ? ancestors.trim() : "0";
    }

    /**
     * 解析租户编号，优先使用当前值，其次使用回退值，最后使用默认租户。
     *
     * @param currentTenantId 当前租户编号
     * @param fallbackTenantId 回退租户编号
     * @return 最终租户编号
     */
    private String resolveTenantId(String currentTenantId, String fallbackTenantId) {
        if (StringUtils.hasText(currentTenantId)) {
            return currentTenantId.trim();
        }
        if (StringUtils.hasText(fallbackTenantId)) {
            return fallbackTenantId.trim();
        }
        return DEFAULT_TENANT_ID;
    }
}

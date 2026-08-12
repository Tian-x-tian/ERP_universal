package com.erp.system.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.erp.system.domain.SysCompany;
import com.erp.system.mapper.SysCompanyMapper;
import com.erp.system.service.ISysCompanyService;
import com.erp.system.support.StatusFieldSupport;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

/**
 * 公司服务实现
 */
@Service
public class SysCompanyServiceImpl extends ServiceImpl<SysCompanyMapper, SysCompany> implements ISysCompanyService {

    /**
     * 构建公司树结构。
     *
     * @param companies 公司列表
     * @return 公司树
     */
    @Override
    public List<SysCompany> buildCompanyTree(List<SysCompany> companies) {
        return buildTree(companies, 0L);
    }

    /**
     * 新增公司并自动补齐祖级链路与基础字段。
     *
     * @param company 公司对象
     * @return 新增结果
     */
    @Override
    public boolean createCompany(SysCompany company) {
        if (company == null || !StringUtils.hasText(company.getCompanyCode()) || !StringUtils.hasText(company.getCompanyName())) {
            return false;
        }
        Long parentCompanyId = company.getParentCompanyId() == null ? 0L : company.getParentCompanyId();
        company.setParentCompanyId(parentCompanyId);
        company.setCompanyCode(company.getCompanyCode().trim());
        company.setCompanyName(company.getCompanyName().trim());
        company.setStatus(StatusFieldSupport.normalizeBinaryStatus(company.getStatus()));
        company.setDelFlag(StringUtils.hasText(company.getDelFlag()) ? company.getDelFlag() : "0");

        if (parentCompanyId == 0L) {
            company.setAncestors("0");
            return save(company);
        }

        SysCompany parentCompany = getById(parentCompanyId);
        if (parentCompany == null) {
            return false;
        }
        String parentAncestors = parentCompany.getAncestors();
        if (!StringUtils.hasText(parentAncestors)) {
            parentAncestors = "0";
        }
        company.setAncestors(parentAncestors + "," + parentCompanyId);
        if (!StringUtils.hasText(company.getTenantId())) {
            company.setTenantId(parentCompany.getTenantId());
        }
        return save(company);
    }

    /**
     * 修改公司并维护祖级链路字段。
     *
     * @param company 公司对象
     * @return 修改结果
     */
    @Override
    public boolean updateCompany(SysCompany company) {
        if (company == null || company.getCompanyId() == null) {
            return false;
        }
        SysCompany existedCompany = getById(company.getCompanyId());
        if (existedCompany == null) {
            return false;
        }
        Long parentCompanyId = company.getParentCompanyId() == null ? 0L : company.getParentCompanyId();
        String oldAncestors = StringUtils.hasText(existedCompany.getAncestors()) ? existedCompany.getAncestors() : "0";
        if (parentCompanyId.equals(company.getCompanyId())) {
            return false;
        }
        if (parentCompanyId == 0L) {
            company.setAncestors("0");
        } else {
            SysCompany parentCompany = getById(parentCompanyId);
            if (parentCompany == null) {
                return false;
            }
            if (containsAncestor(parentCompany.getAncestors(), company.getCompanyId())) {
                return false;
            }
            String parentAncestors = parentCompany.getAncestors();
            if (!StringUtils.hasText(parentAncestors)) {
                parentAncestors = "0";
            }
            company.setAncestors(parentAncestors + "," + parentCompanyId);
            if (!StringUtils.hasText(company.getTenantId())) {
                company.setTenantId(parentCompany.getTenantId());
            }
        }
        if (StringUtils.hasText(company.getCompanyCode())) {
            company.setCompanyCode(company.getCompanyCode().trim());
        }
        if (StringUtils.hasText(company.getCompanyName())) {
            company.setCompanyName(company.getCompanyName().trim());
        }
        company.setStatus(StatusFieldSupport.normalizeBinaryStatusForUpdate(company.getStatus(), existedCompany.getStatus()));
        if (!StringUtils.hasText(company.getTenantId())) {
            company.setTenantId(existedCompany.getTenantId());
        }
        boolean updated = updateById(company);
        if (!updated) {
            return false;
        }
        refreshChildrenAncestors(company.getCompanyId(), oldAncestors, company.getAncestors());
        return true;
    }

    /**
     * 构建树结构。
     *
     * @param companies 公司列表
     * @param parentId 父级ID
     * @return 子树
     */
    private List<SysCompany> buildTree(List<SysCompany> companies, Long parentId) {
        List<SysCompany> tree = new ArrayList<>();
        for (SysCompany company : companies) {
            Long currentParentId = company.getParentCompanyId() == null ? 0L : company.getParentCompanyId();
            if (parentId.equals(currentParentId)) {
                company.setChildren(buildTree(companies, company.getCompanyId()));
                tree.add(company);
            }
        }
        return tree;
    }

    /**
     * 判断祖级列表是否包含指定公司ID。
     *
     * @param ancestors 祖级列表
     * @param companyId 公司ID
     * @return true 表示包含
     */
    private boolean containsAncestor(String ancestors, Long companyId) {
        if (!StringUtils.hasText(ancestors) || companyId == null) {
            return false;
        }
        return Arrays.asList(ancestors.split(",")).contains(String.valueOf(companyId));
    }

    /**
     * 刷新子公司祖级链路，避免上级调整后子节点祖级链路失真。
     *
     * @param companyId    当前公司ID
     * @param oldAncestors 旧祖级链路
     * @param newAncestors 新祖级链路
     */
    private void refreshChildrenAncestors(Long companyId, String oldAncestors, String newAncestors) {
        if (companyId == null) {
            return;
        }
        String oldPrefix = (StringUtils.hasText(oldAncestors) ? oldAncestors : "0") + "," + companyId;
        String newPrefix = (StringUtils.hasText(newAncestors) ? newAncestors : "0") + "," + companyId;
        List<SysCompany> children = list(new LambdaQueryWrapper<SysCompany>()
                .likeRight(SysCompany::getAncestors, oldPrefix));
        for (SysCompany child : children) {
            String childAncestors = child.getAncestors();
            if (!StringUtils.hasText(childAncestors) || !childAncestors.startsWith(oldPrefix)) {
                continue;
            }
            child.setAncestors(newPrefix + childAncestors.substring(oldPrefix.length()));
            child.setUpdateTime(new Date());
            updateById(child);
        }
    }
}

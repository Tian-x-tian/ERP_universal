package com.erp.system.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.erp.system.domain.SysCompany;

import java.util.List;

/**
 * 公司服务接口
 */
public interface ISysCompanyService extends IService<SysCompany> {

    /**
     * 构建公司树结构。
     *
     * @param companies 公司列表
     * @return 公司树
     */
    List<SysCompany> buildCompanyTree(List<SysCompany> companies);

    /**
     * 新增公司并补齐层级关系字段。
     *
     * @param company 公司对象
     * @return 新增结果
     */
    boolean createCompany(SysCompany company);

    /**
     * 修改公司并维护层级关系字段。
     *
     * @param company 公司对象
     * @return 修改结果
     */
    boolean updateCompany(SysCompany company);
}

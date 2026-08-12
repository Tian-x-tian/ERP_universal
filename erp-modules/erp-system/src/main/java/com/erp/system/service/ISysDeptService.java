package com.erp.system.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.erp.system.domain.SysDept;
import java.util.List;

/**
 * 部门服务接口
 */
public interface ISysDeptService extends IService<SysDept> {
    /**
     * 构建部门树
     */
    List<SysDept> buildDeptTree(List<SysDept> depts);

    /**
     * 新增部门并补齐层级关系字段。
     *
     * @param dept 部门对象
     * @return 新增结果
     */
    boolean createDept(SysDept dept);

    /**
     * 修改部门并维护祖级链路。
     *
     * @param dept 部门对象
     * @return 修改结果
     */
    boolean updateDept(SysDept dept);
}

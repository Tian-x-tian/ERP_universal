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
}

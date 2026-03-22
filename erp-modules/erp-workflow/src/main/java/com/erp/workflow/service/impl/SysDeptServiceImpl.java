package com.erp.workflow.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.erp.workflow.domain.platform.SysDept;
import com.erp.workflow.mapper.SysDeptMapper;
import com.erp.workflow.service.ISysDeptService;
import org.springframework.stereotype.Service;

/**
 * 工作流内部部门只读服务实现。
 */
@Service
public class SysDeptServiceImpl extends ServiceImpl<SysDeptMapper, SysDept> implements ISysDeptService {
}

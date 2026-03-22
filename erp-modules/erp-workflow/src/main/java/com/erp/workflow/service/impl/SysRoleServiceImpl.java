package com.erp.workflow.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.erp.workflow.domain.platform.SysRole;
import com.erp.workflow.mapper.SysRoleMapper;
import com.erp.workflow.service.ISysRoleService;
import org.springframework.stereotype.Service;

/**
 * 工作流内部角色只读服务实现。
 */
@Service
public class SysRoleServiceImpl extends ServiceImpl<SysRoleMapper, SysRole> implements ISysRoleService {
}

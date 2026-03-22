package com.erp.workflow.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.erp.workflow.domain.platform.SysUserRole;
import com.erp.workflow.mapper.SysUserRoleMapper;
import com.erp.workflow.service.ISysUserRoleService;
import org.springframework.stereotype.Service;

/**
 * 工作流内部用户角色关联服务实现。
 */
@Service
public class SysUserRoleServiceImpl extends ServiceImpl<SysUserRoleMapper, SysUserRole> implements ISysUserRoleService {
}

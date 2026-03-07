package com.erp.system.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.erp.system.domain.SysUserRole;
import com.erp.system.mapper.SysUserRoleMapper;
import com.erp.system.service.ISysUserRoleService;
import org.springframework.stereotype.Service;

/**
 * 用户与角色关联服务实现
 */
@Service
public class SysUserRoleServiceImpl extends ServiceImpl<SysUserRoleMapper, SysUserRole> implements ISysUserRoleService {
}

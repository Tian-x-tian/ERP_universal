package com.erp.system.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.erp.system.domain.SysRoleMenu;
import com.erp.system.mapper.SysRoleMenuMapper;
import com.erp.system.service.ISysRoleMenuService;
import org.springframework.stereotype.Service;

/**
 * 角色与菜单关联服务实现
 */
@Service
public class SysRoleMenuServiceImpl extends ServiceImpl<SysRoleMenuMapper, SysRoleMenu> implements ISysRoleMenuService {
}

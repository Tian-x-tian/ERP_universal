package com.erp.system.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.erp.system.domain.SysRoleDept;
import com.erp.system.mapper.SysRoleDeptMapper;
import com.erp.system.service.ISysRoleDeptService;
import org.springframework.stereotype.Service;

/**
 * 角色部门关联服务实现
 */
@Service
public class SysRoleDeptServiceImpl extends ServiceImpl<SysRoleDeptMapper, SysRoleDept> implements ISysRoleDeptService {
}

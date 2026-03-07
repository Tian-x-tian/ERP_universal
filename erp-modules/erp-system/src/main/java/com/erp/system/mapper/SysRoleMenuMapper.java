package com.erp.system.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.erp.system.domain.SysRoleMenu;
import org.apache.ibatis.annotations.Mapper;

/**
 * 角色与菜单关联 Mapper 接口
 */
@Mapper
public interface SysRoleMenuMapper extends BaseMapper<SysRoleMenu> {
}

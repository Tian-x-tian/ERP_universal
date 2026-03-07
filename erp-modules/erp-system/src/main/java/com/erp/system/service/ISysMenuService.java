package com.erp.system.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.erp.system.domain.SysMenu;
import java.util.List;
import java.util.Set;

/**
 * 菜单服务接口
 */
public interface ISysMenuService extends IService<SysMenu> {
    /**
     * 根据用户ID查询权限标识
     */
    Set<String> selectMenuPermsByUserId(Long userId);

    /**
     * 根据用户ID查询菜单树
     */
    List<SysMenu> selectMenuTreeByUserId(Long userId);
}

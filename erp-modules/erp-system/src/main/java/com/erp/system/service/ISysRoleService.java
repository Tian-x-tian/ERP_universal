package com.erp.system.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.erp.system.domain.SysRole;
import java.util.Set;

/**
 * 角色服务接口
 */
public interface ISysRoleService extends IService<SysRole> {
    /**
     * 根据用户ID查询角色权限
     */
    Set<String> selectRoleKeysByUserId(Long userId);
}

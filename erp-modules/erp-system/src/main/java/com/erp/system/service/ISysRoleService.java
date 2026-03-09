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

    /**
     * 查询角色详情并回填菜单与部门权限。
     *
     * @param roleId 角色ID
     * @return 角色详情
     */
    SysRole getRoleWithPermissions(Long roleId);

    /**
     * 更新角色数据权限范围。
     *
     * @param role 角色对象
     * @return 更新结果
     */
    boolean updateDataScope(SysRole role);
}

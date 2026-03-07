package com.erp.system.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.erp.system.domain.SysMenu;
import com.erp.system.domain.SysRoleMenu;
import com.erp.system.domain.SysUserRole;
import com.erp.system.mapper.SysMenuMapper;
import com.erp.system.service.ISysMenuService;
import com.erp.system.service.ISysRoleMenuService;
import com.erp.system.service.ISysUserRoleService;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 菜单服务实现
 */
@Service
public class SysMenuServiceImpl extends ServiceImpl<SysMenuMapper, SysMenu> implements ISysMenuService {

    private final ISysUserRoleService userRoleService;
    private final ISysRoleMenuService roleMenuService;

    public SysMenuServiceImpl(ISysUserRoleService userRoleService, ISysRoleMenuService roleMenuService) {
        this.userRoleService = userRoleService;
        this.roleMenuService = roleMenuService;
    }

    /**
     * 根据用户ID查询菜单权限标识。
     *
     * @param userId 用户ID
     * @return 当前用户拥有的权限标识集合
     */
    @Override
    public Set<String> selectMenuPermsByUserId(Long userId) {
        List<Long> roleIds = userRoleService
                .list(new LambdaQueryWrapper<SysUserRole>().eq(SysUserRole::getUserId, userId))
                .stream()
                .map(SysUserRole::getRoleId)
                .collect(Collectors.toList());
        if (roleIds.isEmpty()) {
            return Collections.emptySet();
        }

        List<Long> menuIds = roleMenuService
                .list(new LambdaQueryWrapper<SysRoleMenu>().in(SysRoleMenu::getRoleId, roleIds))
                .stream()
                .map(SysRoleMenu::getMenuId)
                .distinct()
                .collect(Collectors.toList());
        if (menuIds.isEmpty()) {
            return Collections.emptySet();
        }

        return listByIds(menuIds).stream()
                .map(SysMenu::getPerms)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
    }

    /**
     * 根据用户ID查询菜单树。
     *
     * @param userId 用户ID
     * @return 当前用户可见的菜单树
     */
    @Override
    public List<SysMenu> selectMenuTreeByUserId(Long userId) {
        List<Long> roleIds = userRoleService
                .list(new LambdaQueryWrapper<SysUserRole>().eq(SysUserRole::getUserId, userId))
                .stream()
                .map(SysUserRole::getRoleId)
                .collect(Collectors.toList());
        if (roleIds.isEmpty()) {
            return Collections.emptyList();
        }

        List<Long> menuIds = roleMenuService
                .list(new LambdaQueryWrapper<SysRoleMenu>().in(SysRoleMenu::getRoleId, roleIds))
                .stream()
                .map(SysRoleMenu::getMenuId)
                .distinct()
                .collect(Collectors.toList());
        if (menuIds.isEmpty()) {
            return Collections.emptyList();
        }

        List<SysMenu> menus = listByIds(menuIds).stream()
                .sorted(Comparator.comparing(SysMenu::getOrderNum, Comparator.nullsLast(Integer::compareTo)))
                .collect(Collectors.toList());
        return buildMenuTree(menus, 0L);
    }

    /**
     * 构建菜单树结构。
     *
     * @param menus    菜单列表
     * @param parentId 当前父节点ID
     * @return 以 parentId 为根的子树列表
     */
    private List<SysMenu> buildMenuTree(List<SysMenu> menus, Long parentId) {
        List<SysMenu> tree = new ArrayList<>();
        for (SysMenu menu : menus) {
            if (parentId.equals(menu.getParentId())) {
                menu.setChildren(buildMenuTree(menus, menu.getMenuId()));
                tree.add(menu);
            }
        }
        return tree;
    }
}

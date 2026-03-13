package com.erp.system.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.erp.system.domain.SysMenu;
import com.erp.system.domain.SysRole;
import com.erp.system.domain.SysRoleMenu;
import com.erp.system.domain.SysUserRole;
import com.erp.system.domain.vo.SysMenuSyncNode;
import com.erp.system.mapper.SysMenuMapper;
import com.erp.system.security.service.SecurityUserResolver;
import com.erp.system.service.ISysMenuService;
import com.erp.system.service.ISysRoleMenuService;
import com.erp.system.service.ISysRoleService;
import com.erp.system.service.ISysUserRoleService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 菜单服务实现
 */
@Service
public class SysMenuServiceImpl extends ServiceImpl<SysMenuMapper, SysMenu> implements ISysMenuService {

    private static final String ALL_PERMISSION = "*:*:*";

    private final ISysUserRoleService userRoleService;
    private final ISysRoleMenuService roleMenuService;
    private final ISysRoleService roleService;
    private final SecurityUserResolver securityUserResolver;

    public SysMenuServiceImpl(ISysUserRoleService userRoleService,
            ISysRoleMenuService roleMenuService,
            ISysRoleService roleService,
            SecurityUserResolver securityUserResolver) {
        this.userRoleService = userRoleService;
        this.roleMenuService = roleMenuService;
        this.roleService = roleService;
        this.securityUserResolver = securityUserResolver;
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
        if (roleService.isPlatformSuperAdmin(userId)) {
            Set<String> allPermissionSet = new HashSet<>();
            allPermissionSet.add(ALL_PERMISSION);
            return allPermissionSet;
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
                .filter(StringUtils::hasText)
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

        List<SysMenu> allMenus = list();
        if (allMenus.isEmpty()) {
            return Collections.emptyList();
        }

        List<SysMenu> menus = resolveGrantedMenus(roleIds, roleService.isPlatformSuperAdmin(userId), allMenus).stream()
                // 按钮型权限不参与路由树组装，避免前端路由异常。
                .filter(menu -> !"F".equals(menu.getMenuType()))
                .filter(menu -> !"1".equals(menu.getStatus()))
                .filter(menu -> !"1".equals(menu.getVisible()))
                .sorted(Comparator.comparing(SysMenu::getOrderNum, Comparator.nullsLast(Integer::compareTo)))
                .collect(Collectors.toList());
        return buildMenuTree(menus, 0L);
    }

    /**
     * 按当前页面菜单蓝图同步菜单结构。
     *
     * @param menuTree 菜单同步树
     * @return 处理记录数
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public int syncMenuTree(List<SysMenuSyncNode> menuTree) {
        if (menuTree == null || menuTree.isEmpty()) {
            return 0;
        }
        MenuSyncContext context = new MenuSyncContext(list(), resolveOperator());
        int handledCount = 0;
        for (SysMenuSyncNode rootNode : menuTree) {
            handledCount += syncMenuNode(rootNode, 0L, context);
        }
        return handledCount;
    }

    /**
     * 解析当前用户真正可见的菜单集合，并自动补齐父节点。
     *
     * @param roleIds              当前用户角色ID集合
     * @param platformSuperAdmin   是否平台超级管理员
     * @param allMenus             系统全部菜单
     * @return 授权菜单集合
     */
    private List<SysMenu> resolveGrantedMenus(List<Long> roleIds, boolean platformSuperAdmin, List<SysMenu> allMenus) {
        if (platformSuperAdmin) {
            return allMenus;
        }
        Set<Long> menuIds = roleMenuService
                .list(new LambdaQueryWrapper<SysRoleMenu>().in(SysRoleMenu::getRoleId, roleIds))
                .stream()
                .map(SysRoleMenu::getMenuId)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        if (menuIds.isEmpty()) {
            return Collections.emptyList();
        }
        appendParentMenuIds(menuIds, allMenus);
        return allMenus.stream()
                .filter(menu -> menuIds.contains(menu.getMenuId()))
                .collect(Collectors.toList());
    }

    /**
     * 为已授权菜单补齐全部父节点，避免树结构因父节点缺失而被截断。
     *
     * @param menuIds  已授权菜单ID集合
     * @param allMenus 系统全部菜单
     */
    private void appendParentMenuIds(Set<Long> menuIds, List<SysMenu> allMenus) {
        Map<Long, SysMenu> menuMap = allMenus.stream()
                .collect(Collectors.toMap(SysMenu::getMenuId, menu -> menu));
        Set<Long> snapshotMenuIds = new HashSet<>(menuIds);
        for (Long menuId : snapshotMenuIds) {
            appendParentMenuId(menuId, menuMap, menuIds);
        }
    }

    /**
     * 递归补齐单个菜单的父节点链路。
     *
     * @param menuId   当前菜单ID
     * @param menuMap  菜单索引
     * @param menuIds  已授权菜单ID集合
     */
    private void appendParentMenuId(Long menuId, Map<Long, SysMenu> menuMap, Set<Long> menuIds) {
        SysMenu currentMenu = menuMap.get(menuId);
        if (currentMenu == null || currentMenu.getParentId() == null || currentMenu.getParentId() <= 0L) {
            return;
        }
        if (menuIds.add(currentMenu.getParentId())) {
            appendParentMenuId(currentMenu.getParentId(), menuMap, menuIds);
            return;
        }
        if (menuMap.containsKey(currentMenu.getParentId())) {
            appendParentMenuId(currentMenu.getParentId(), menuMap, menuIds);
        }
    }

    /**
     * 递归同步单个菜单节点及其子节点。
     *
     * @param syncNode  待同步节点
     * @param parentId  父菜单ID
     * @param context   同步上下文
     * @return 当前节点及其子节点的处理数量
     */
    private int syncMenuNode(SysMenuSyncNode syncNode, Long parentId, MenuSyncContext context) {
        if (syncNode == null || !StringUtils.hasText(syncNode.getMenuName())) {
            return 0;
        }
        SysMenu matchedMenu = context.findMatchedMenu(syncNode, parentId);
        if (matchedMenu == null) {
            matchedMenu = buildNewMenu(syncNode, parentId, context.getOperator());
            baseMapper.insert(matchedMenu);
            context.addMenu(matchedMenu);
        } else {
            applySyncNode(matchedMenu, syncNode, parentId, false, context.getOperator());
            baseMapper.updateById(matchedMenu);
            context.refreshMenu(matchedMenu);
        }
        context.markConsumed(matchedMenu.getMenuId());

        int handledCount = 1;
        List<SysMenuSyncNode> children = syncNode.getChildren();
        if (children == null || children.isEmpty()) {
            return handledCount;
        }
        for (SysMenuSyncNode child : children) {
            handledCount += syncMenuNode(child, matchedMenu.getMenuId(), context);
        }
        return handledCount;
    }

    /**
     * 构建新增菜单实体。
     *
     * @param syncNode  菜单同步节点
     * @param parentId  父菜单ID
     * @param operator  操作人
     * @return 菜单实体
     */
    private SysMenu buildNewMenu(SysMenuSyncNode syncNode, Long parentId, String operator) {
        SysMenu menu = new SysMenu();
        applySyncNode(menu, syncNode, parentId, true, operator);
        return menu;
    }

    /**
     * 将同步节点字段映射到菜单实体。
     *
     * @param menu      菜单实体
     * @param syncNode  菜单同步节点
     * @param parentId  父菜单ID
     * @param createNew 是否为新建菜单
     * @param operator  操作人
     */
    private void applySyncNode(SysMenu menu, SysMenuSyncNode syncNode, Long parentId, boolean createNew, String operator) {
        Date now = new Date();
        menu.setParentId(parentId);
        menu.setMenuName(trimToNull(syncNode.getMenuName()));
        menu.setOrderNum(syncNode.getOrderNum() == null ? 0 : syncNode.getOrderNum());
        menu.setPath(defaultString(syncNode.getPath()));
        menu.setComponent(resolveComponent(syncNode));
        menu.setIsFrame(syncNode.getIsFrame() == null ? 1 : syncNode.getIsFrame());
        menu.setMenuType(defaultMenuType(syncNode.getMenuType()));
        menu.setVisible(defaultFlag(syncNode.getVisible()));
        menu.setStatus(defaultFlag(syncNode.getStatus()));
        menu.setPerms(resolvePerms(syncNode));
        menu.setIcon(defaultIcon(syncNode.getIcon()));
        menu.setUpdateBy(operator);
        menu.setUpdateTime(now);
        if (createNew) {
            menu.setCreateBy(operator);
            menu.setCreateTime(now);
        }
    }

    /**
     * 解析组件路径。
     *
     * @param syncNode 菜单同步节点
     * @return 组件路径
     */
    private String resolveComponent(SysMenuSyncNode syncNode) {
        String menuType = defaultMenuType(syncNode.getMenuType());
        if (!"C".equals(menuType)) {
            return null;
        }
        return trimToNull(syncNode.getComponent());
    }

    /**
     * 解析权限标识。
     *
     * @param syncNode 菜单同步节点
     * @return 权限标识
     */
    private String resolvePerms(SysMenuSyncNode syncNode) {
        String menuType = defaultMenuType(syncNode.getMenuType());
        if ("M".equals(menuType)) {
            return null;
        }
        return trimToNull(syncNode.getPerms());
    }

    /**
     * 解析当前操作人账号。
     *
     * @return 操作人账号
     */
    private String resolveOperator() {
        String username = securityUserResolver.getCurrentUsername();
        return StringUtils.hasText(username) ? username.trim() : "system";
    }

    /**
     * 规范化菜单类型。
     *
     * @param menuType 原始菜单类型
     * @return 标准菜单类型
     */
    private String defaultMenuType(String menuType) {
        return StringUtils.hasText(menuType) ? menuType.trim() : "M";
    }

    /**
     * 规范化状态位字段。
     *
     * @param value 原始状态值
     * @return 标准状态值
     */
    private String defaultFlag(String value) {
        return StringUtils.hasText(value) ? value.trim() : "0";
    }

    /**
     * 规范化图标名称。
     *
     * @param icon 原始图标
     * @return 标准图标
     */
    private String defaultIcon(String icon) {
        return StringUtils.hasText(icon) ? icon.trim() : "#";
    }

    /**
     * 将字符串规整为数据库默认值。
     *
     * @param value 原始字符串
     * @return 规范后的字符串
     */
    private String defaultString(String value) {
        return StringUtils.hasText(value) ? value.trim() : "";
    }

    /**
     * 去除字符串首尾空白，空值返回 null。
     *
     * @param value 原始字符串
     * @return 去空白后的字符串
     */
    private String trimToNull(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }

    /**
     * 规范化路径。
     *
     * @param path 原始路径
     * @return 标准化路径
     */
    private String normalizePath(String path) {
        return defaultString(path).toLowerCase().replaceAll("/+$", "");
    }

    /**
     * 菜单同步上下文。
     */
    private final class MenuSyncContext {
        private final List<SysMenu> menuList;
        private final Set<Long> consumedMenuIds = new HashSet<>();
        private final String operator;

        private MenuSyncContext(List<SysMenu> menuList, String operator) {
            this.menuList = new ArrayList<>(menuList);
            this.operator = operator;
        }

        /**
         * 获取当前操作人账号。
         *
         * @return 操作人账号
         */
        private String getOperator() {
            return operator;
        }

        /**
         * 标记菜单已被当前同步节点消费。
         *
         * @param menuId 菜单ID
         */
        private void markConsumed(Long menuId) {
            if (menuId != null) {
                consumedMenuIds.add(menuId);
            }
        }

        /**
         * 将新增菜单加入上下文缓存。
         *
         * @param menu 菜单实体
         */
        private void addMenu(SysMenu menu) {
            menuList.add(menu);
        }

        /**
         * 刷新上下文中的菜单引用。
         *
         * @param menu 最新菜单实体
         */
        private void refreshMenu(SysMenu menu) {
            for (int i = 0; i < menuList.size(); i++) {
                if (menu.getMenuId() != null && menu.getMenuId().equals(menuList.get(i).getMenuId())) {
                    menuList.set(i, menu);
                    return;
                }
            }
            menuList.add(menu);
        }

        /**
         * 为当前同步节点匹配已有菜单。
         *
         * @param syncNode 菜单同步节点
         * @param parentId 父菜单ID
         * @return 命中的菜单实体
         */
        private SysMenu findMatchedMenu(SysMenuSyncNode syncNode, Long parentId) {
            List<String> candidatePaths = buildCandidatePaths(syncNode);
            SysMenu matchedMenu = findByPaths(candidatePaths, parentId, true);
            if (matchedMenu != null) {
                return matchedMenu;
            }
            matchedMenu = findByPaths(candidatePaths, parentId, false);
            if (matchedMenu != null) {
                return matchedMenu;
            }
            return findByName(syncNode.getMenuName(), parentId);
        }

        /**
         * 根据路径集合查找已有菜单。
         *
         * @param candidatePaths 候选路径集合
         * @param parentId       父菜单ID
         * @param sameParentOnly 是否仅匹配相同父菜单
         * @return 命中的菜单
         */
        private SysMenu findByPaths(List<String> candidatePaths, Long parentId, boolean sameParentOnly) {
            for (String candidatePath : candidatePaths) {
                String normalizedCandidatePath = normalizePath(candidatePath);
                if (!StringUtils.hasText(normalizedCandidatePath)) {
                    continue;
                }
                for (SysMenu menu : menuList) {
                    if (menu == null || menu.getMenuId() == null || consumedMenuIds.contains(menu.getMenuId())) {
                        continue;
                    }
                    if (sameParentOnly && !parentId.equals(menu.getParentId())) {
                        continue;
                    }
                    if (normalizedCandidatePath.equals(normalizePath(menu.getPath()))) {
                        return menu;
                    }
                }
            }
            return null;
        }

        /**
         * 按菜单名称补充匹配同级目录。
         *
         * @param menuName 菜单名称
         * @param parentId 父菜单ID
         * @return 命中的菜单
         */
        private SysMenu findByName(String menuName, Long parentId) {
            String normalizedMenuName = trimToNull(menuName);
            if (!StringUtils.hasText(normalizedMenuName)) {
                return null;
            }
            for (SysMenu menu : menuList) {
                if (menu == null || menu.getMenuId() == null || consumedMenuIds.contains(menu.getMenuId())) {
                    continue;
                }
                if (!parentId.equals(menu.getParentId())) {
                    continue;
                }
                if (normalizedMenuName.equals(trimToNull(menu.getMenuName()))) {
                    return menu;
                }
            }
            return null;
        }

        /**
         * 构建同步节点的候选路径集合。
         *
         * @param syncNode 菜单同步节点
         * @return 候选路径集合
         */
        private List<String> buildCandidatePaths(SysMenuSyncNode syncNode) {
            LinkedHashSet<String> candidatePathSet = new LinkedHashSet<>();
            candidatePathSet.add(syncNode.getPath());
            if (syncNode.getSourcePaths() != null) {
                candidatePathSet.addAll(syncNode.getSourcePaths());
            }
            return new ArrayList<>(candidatePathSet);
        }
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

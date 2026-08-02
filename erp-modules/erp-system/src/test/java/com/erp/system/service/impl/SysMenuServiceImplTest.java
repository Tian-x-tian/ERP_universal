package com.erp.system.service.impl;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.erp.common.core.context.TenantContextHolder;
import com.erp.saas.contract.model.TenantLifecycleState;
import com.erp.system.domain.SysMenu;
import com.erp.system.domain.SysRoleMenu;
import com.erp.system.domain.SysUserRole;
import com.erp.system.domain.vo.SysMenuSyncNode;
import com.erp.system.mapper.SysMenuMapper;
import com.erp.system.security.service.SecurityUserResolver;
import com.erp.system.saas.SaasRuntimeEntitlements;
import com.erp.system.saas.SaasRuntimeSnapshotService;
import com.erp.system.saas.SaasRuntimeSource;
import com.erp.system.service.ISysRoleMenuService;
import com.erp.system.service.ISysRoleService;
import com.erp.system.service.ISysUserRoleService;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 菜单服务单元测试。
 */
@ExtendWith(MockitoExtension.class)
class SysMenuServiceImplTest {

    @Mock
    private SysMenuMapper menuMapper;

    @Mock
    private ISysUserRoleService userRoleService;

    @Mock
    private ISysRoleMenuService roleMenuService;

    @Mock
    private ISysRoleService roleService;

    @Mock
    private SecurityUserResolver securityUserResolver;

    @Mock
    private SaasRuntimeSnapshotService snapshotService;

    private SysMenuServiceImpl menuService;

    /**
     * 初始化被测服务与实体元数据。
     */
    @BeforeEach
    void setUp() {
        menuService = new SysMenuServiceImpl(userRoleService, roleMenuService, roleService,
                securityUserResolver, snapshotService);
        ReflectionTestUtils.setField(menuService, "baseMapper", menuMapper);
        initTableInfoIfAbsent(SysUserRole.class);
        initTableInfoIfAbsent(SysRoleMenu.class);
        TenantContextHolder.setTenantId("tenant-a");
    }

    @AfterEach
    void tearDown() {
        TenantContextHolder.clear();
    }

    /**
     * 初始化实体元数据缓存，保证 LambdaQueryWrapper 在纯单测场景可用。
     *
     * @param entityClass 实体类型
     */
    private void initTableInfoIfAbsent(Class<?> entityClass) {
        if (TableInfoHelper.getTableInfo(entityClass) != null) {
            return;
        }
        MybatisConfiguration configuration = new MybatisConfiguration();
        MapperBuilderAssistant builderAssistant = new MapperBuilderAssistant(configuration, "");
        TableInfoHelper.initTableInfo(builderAssistant, entityClass);
    }

    /**
     * 验证菜单树组装时会自动补齐父菜单。
     */
    @Test
    void shouldAppendParentMenusWhenBuildingTree() {
        SysUserRole userRole = new SysUserRole();
        userRole.setRoleId(1L);
        when(userRoleService.list(any(LambdaQueryWrapper.class))).thenReturn(Collections.singletonList(userRole));

        SysRoleMenu roleMenu = new SysRoleMenu();
        roleMenu.setRoleId(1L);
        roleMenu.setMenuId(4L);
        when(roleMenuService.list(any(LambdaQueryWrapper.class))).thenReturn(Collections.singletonList(roleMenu));
        when(menuMapper.selectList(any())).thenReturn(Arrays.asList(
                buildMenu(2L, 0L, "系统管理", "C", "0", "0", 1),
                buildMenu(151L, 2L, "组织权限", "M", "0", "0", 2),
                buildMenu(4L, 151L, "用户管理", "C", "0", "0", 3)));

        List<SysMenu> menuTree = menuService.selectMenuTreeByUserId(1L);

        Assertions.assertEquals(1, menuTree.size());
        Assertions.assertEquals(Long.valueOf(2L), menuTree.get(0).getMenuId());
        Assertions.assertEquals(1, menuTree.get(0).getChildren().size());
        Assertions.assertEquals(Long.valueOf(151L), menuTree.get(0).getChildren().get(0).getMenuId());
        Assertions.assertEquals(1, menuTree.get(0).getChildren().get(0).getChildren().size());
        Assertions.assertEquals(Long.valueOf(4L), menuTree.get(0).getChildren().get(0).getChildren().get(0).getMenuId());
    }

    /**
     * 验证同步当前页面菜单时会复用历史路径菜单并挂到新目录下。
     */
    @Test
    void shouldSyncLegacyMenuToBlueprintPath() {
        AtomicLong nextMenuId = new AtomicLong(100L);
        SysMenu legacyNoticeMenu = buildMenu(12L, 2L, "通知管理", "C", "0", "0", 1);
        legacyNoticeMenu.setPath("/system/notice");
        legacyNoticeMenu.setPerms("system:notice:list");
        when(menuMapper.selectList(any())).thenReturn(Collections.singletonList(legacyNoticeMenu));
        when(menuMapper.insert(any(SysMenu.class))).thenAnswer(invocation -> {
            SysMenu insertedMenu = invocation.getArgument(0);
            insertedMenu.setMenuId(nextMenuId.getAndIncrement());
            return 1;
        });
        when(menuMapper.updateById(any(SysMenu.class))).thenReturn(1);
        when(securityUserResolver.getCurrentUsername()).thenReturn("admin");

        SysMenuSyncNode rootNode = new SysMenuSyncNode();
        rootNode.setMenuName("工作台");
        rootNode.setPath("/workbench");
        rootNode.setMenuType("M");
        rootNode.setVisible("0");
        rootNode.setStatus("0");
        rootNode.setIsFrame(1);
        rootNode.setOrderNum(1);

        SysMenuSyncNode childNode = new SysMenuSyncNode();
        childNode.setMenuName("通知");
        childNode.setPath("/workbench/notice");
        childNode.setComponent("/views/system/notice/index");
        childNode.setMenuType("C");
        childNode.setVisible("0");
        childNode.setStatus("0");
        childNode.setIsFrame(1);
        childNode.setOrderNum(1);
        childNode.setPerms("system:notice:list");
        childNode.setSourcePaths(Arrays.asList("/workbench/notice", "/system/notice"));
        rootNode.setChildren(new ArrayList<>(Collections.singletonList(childNode)));

        int handledCount = menuService.syncMenuTree(Collections.singletonList(rootNode));

        Assertions.assertEquals(2, handledCount);
        verify(menuMapper).insert(any(SysMenu.class));
        verify(menuMapper).updateById(any(SysMenu.class));
        Assertions.assertEquals("/workbench/notice", legacyNoticeMenu.getPath());
        Assertions.assertEquals("通知", legacyNoticeMenu.getMenuName());
        Assertions.assertEquals(Long.valueOf(100L), legacyNoticeMenu.getParentId());
        Assertions.assertEquals("admin", legacyNoticeMenu.getUpdateBy());
    }

    @Test
    void shouldIntersectRbacMenuTreeWithSaasFeatures() {
        SysUserRole userRole = new SysUserRole();
        userRole.setRoleId(1L);
        when(userRoleService.list(any(LambdaQueryWrapper.class))).thenReturn(List.of(userRole));
        SysRoleMenu allowedRoleMenu = new SysRoleMenu();
        allowedRoleMenu.setRoleId(1L);
        allowedRoleMenu.setMenuId(3L);
        SysRoleMenu deniedRoleMenu = new SysRoleMenu();
        deniedRoleMenu.setRoleId(1L);
        deniedRoleMenu.setMenuId(4L);
        when(roleMenuService.list(any(LambdaQueryWrapper.class)))
                .thenReturn(List.of(allowedRoleMenu, deniedRoleMenu));
        SysMenu root = buildMenu(2L, 0L, "业务", "M", "0", "0", 1);
        SysMenu allowed = buildMenu(3L, 2L, "订单查询", "C", "0", "0", 1);
        allowed.setFeatureKey("orders.view");
        SysMenu denied = buildMenu(4L, 2L, "订单维护", "C", "0", "0", 2);
        denied.setFeatureKey("orders.edit");
        when(menuMapper.selectList(any())).thenReturn(List.of(root, allowed, denied));
        when(snapshotService.current("tenant-a")).thenReturn(entitlements());

        List<SysMenu> tree = menuService.selectMenuTreeByUserId(1L);

        Assertions.assertEquals(1, tree.size());
        Assertions.assertEquals(List.of(3L), tree.get(0).getChildren().stream()
                .map(SysMenu::getMenuId).toList());
    }

    @Test
    void shouldIntersectRbacPermissionsWithSaasFeatures() {
        SysUserRole userRole = new SysUserRole();
        userRole.setRoleId(1L);
        when(userRoleService.list(any(LambdaQueryWrapper.class))).thenReturn(List.of(userRole));
        SysRoleMenu first = new SysRoleMenu();
        first.setRoleId(1L);
        first.setMenuId(3L);
        SysRoleMenu second = new SysRoleMenu();
        second.setRoleId(1L);
        second.setMenuId(4L);
        when(roleMenuService.list(any(LambdaQueryWrapper.class))).thenReturn(List.of(first, second));
        SysMenu allowed = buildMenu(3L, 2L, "订单查询", "F", "0", "0", 1);
        allowed.setPerms("orders:view");
        allowed.setFeatureKey("orders.view");
        SysMenu denied = buildMenu(4L, 2L, "订单维护", "F", "0", "0", 2);
        denied.setPerms("orders:edit");
        denied.setFeatureKey("orders.edit");
        when(menuMapper.selectBatchIds(any())).thenReturn(List.of(allowed, denied));
        when(snapshotService.current("tenant-a")).thenReturn(entitlements());

        Assertions.assertEquals(java.util.Set.of("orders:view"), menuService.selectMenuPermsByUserId(1L));
    }

    private SaasRuntimeEntitlements entitlements() {
        return new SaasRuntimeEntitlements("tenant-a", TenantLifecycleState.ACTIVE, 1L,
                SaasRuntimeSource.LOCAL_CACHE, false, true, true,
                Map.of("orders.view", true, "orders.edit", false), Map.of());
    }

    /**
     * 构造菜单测试数据。
     *
     * @param menuId   菜单ID
     * @param parentId 父菜单ID
     * @param menuName 菜单名称
     * @param menuType 菜单类型
     * @param status   状态
     * @param visible  显示状态
     * @param orderNum 排序值
     * @return 菜单对象
     */
    private SysMenu buildMenu(Long menuId, Long parentId, String menuName, String menuType,
            String status, String visible, Integer orderNum) {
        SysMenu menu = new SysMenu();
        menu.setMenuId(menuId);
        menu.setParentId(parentId);
        menu.setMenuName(menuName);
        menu.setMenuType(menuType);
        menu.setStatus(status);
        menu.setVisible(visible);
        menu.setOrderNum(orderNum);
        return menu;
    }
}

package com.erp.system.controller;

import com.erp.common.logging.OperationLogRecorder;
import com.erp.saas.contract.model.SaasQuotaUsage;
import com.erp.saas.contract.model.SaasUsageEvent;
import com.erp.saas.contract.model.SaasUsageOperation;
import com.erp.platform.contract.model.PlatformDeptView;
import com.erp.platform.contract.model.PlatformAiActionPolicyItem;
import com.erp.platform.contract.model.PlatformAiAuditView;
import com.erp.platform.contract.model.PlatformAiConfigView;
import com.erp.platform.contract.model.PlatformPostView;
import com.erp.platform.contract.model.PlatformRoleView;
import com.erp.platform.contract.model.PlatformUserPostLink;
import com.erp.platform.contract.model.PlatformUserRoleLink;
import com.erp.platform.contract.model.PlatformUserView;
import com.erp.system.domain.SysDept;
import com.erp.system.domain.SysPost;
import com.erp.system.domain.SysRole;
import com.erp.system.domain.SysUser;
import com.erp.system.domain.SysUserPost;
import com.erp.system.domain.SysUserRole;
import com.erp.system.security.service.SecurityUserResolver;
import com.erp.system.service.IMdmItemService;
import com.erp.system.service.IMdmWarehouseService;
import com.erp.system.service.ISysAiAuditService;
import com.erp.system.service.ISysAiConfigService;
import com.erp.system.service.ISysConfigService;
import com.erp.system.service.ISysDeptService;
import com.erp.system.service.ISysImexJobService;
import com.erp.system.service.ISysMenuService;
import com.erp.system.service.ISysNoticeService;
import com.erp.system.service.ISysPostService;
import com.erp.system.service.ISysRoleService;
import com.erp.system.service.ISysTenantService;
import com.erp.system.service.ISysUserPostService;
import com.erp.system.service.ISysUserRoleService;
import com.erp.system.service.ISysUserService;
import com.erp.system.saas.SaasLocalQuotaService;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * 平台内部控制器新增只读契约单元测试。
 */
@ExtendWith(MockitoExtension.class)
class SystemInternalControllerTest {

    @Mock
    private SecurityUserResolver securityUserResolver;
    @Mock
    private ISysRoleService roleService;
    @Mock
    private ISysMenuService menuService;
    @Mock
    private ISysConfigService configService;
    @Mock
    private ISysTenantService tenantService;
    @Mock
    private ISysImexJobService imexJobService;
    @Mock
    private ISysAiConfigService aiConfigService;
    @Mock
    private ISysAiAuditService aiAuditService;
    @Mock
    private IMdmItemService itemService;
    @Mock
    private IMdmWarehouseService warehouseService;
    @Mock
    private ISysUserService userService;
    @Mock
    private ISysDeptService deptService;
    @Mock
    private ISysPostService postService;
    @Mock
    private ISysUserRoleService userRoleService;
    @Mock
    private ISysUserPostService userPostService;
    @Mock
    private ISysNoticeService noticeService;
    @Mock
    private OperationLogRecorder operationLogRecorder;
    @Mock
    private SaasLocalQuotaService quotaService;

    private SystemInternalController controller;

    /**
     * 初始化控制器。
     */
    @BeforeEach
    void setUp() {
        controller = new SystemInternalController(
                securityUserResolver,
                roleService,
                menuService,
                configService,
                tenantService,
                imexJobService,
                aiConfigService,
                aiAuditService,
                itemService,
                warehouseService,
                userService,
                deptService,
                postService,
                userRoleService,
                userPostService,
                noticeService,
                operationLogRecorder,
                quotaService);
    }

    @Test
    void shouldApplyLocalQuotaEventThroughInternalEndpoint() {
        SaasUsageEvent event = new SaasUsageEvent("evt-1", "TENANT_A", "storage_bytes",
                SaasUsageOperation.RESERVE, "object-1", 128L, null, 1L);
        SaasQuotaUsage expected = new SaasQuotaUsage("storage_bytes", 0L, 128L, null);
        when(quotaService.apply(event)).thenReturn(expected);

        SaasQuotaUsage actual = controller.applyQuotaEvent(event);

        Assertions.assertSame(expected, actual);
    }

    /**
     * 验证用户相关只读接口会返回部门字段。
     */
    @Test
    void shouldExposePlatformUsersWithDeptId() {
        SysUser user = new SysUser();
        user.setUserId(11L);
        user.setTenantId("TENANT_A");
        user.setDeptId(22L);
        user.setUserName("tester");
        user.setNickName("测试员");
        user.setStatus("0");
        user.setDelFlag("0");
        when(userService.list(org.mockito.ArgumentMatchers.<com.baomidou.mybatisplus.core.conditions.Wrapper<SysUser>>any()))
                .thenReturn(Collections.singletonList(user));
        when(userService.getOne(any())).thenReturn(user);

        List<PlatformUserView> userList = controller.users("11");
        PlatformUserView detail = controller.user(11L);

        Assertions.assertEquals(1, userList.size());
        Assertions.assertEquals(22L, userList.get(0).getDeptId());
        Assertions.assertEquals("tester", detail.getUserName());
        Assertions.assertEquals(22L, detail.getDeptId());
    }

    /**
     * 验证部门、角色、岗位接口返回新契约模型。
     */
    @Test
    void shouldExposeDepartmentRoleAndPostViews() {
        SysDept dept = new SysDept();
        dept.setDeptId(7L);
        dept.setParentId(1L);
        dept.setDeptName("研发部");
        dept.setLeader("leader");
        dept.setStatus("0");
        dept.setDelFlag("0");
        SysRole role = new SysRole();
        role.setRoleId(3L);
        role.setRoleName("审批人");
        role.setRoleKey("approver");
        role.setRoleSort(2);
        role.setStatus("0");
        role.setDelFlag("0");
        SysPost post = new SysPost();
        post.setPostId(5L);
        post.setPostCode("POST_A");
        post.setPostName("经理");
        post.setPostSort(1);
        post.setStatus("0");
        when(deptService.list(org.mockito.ArgumentMatchers.<com.baomidou.mybatisplus.core.conditions.Wrapper<SysDept>>any()))
                .thenReturn(Collections.singletonList(dept));
        when(deptService.getOne(any())).thenReturn(dept);
        when(roleService.list(org.mockito.ArgumentMatchers.<com.baomidou.mybatisplus.core.conditions.Wrapper<SysRole>>any()))
                .thenReturn(Collections.singletonList(role));
        when(postService.list(org.mockito.ArgumentMatchers.<com.baomidou.mybatisplus.core.conditions.Wrapper<SysPost>>any()))
                .thenReturn(Collections.singletonList(post));

        List<PlatformDeptView> deptList = controller.departments("7");
        PlatformDeptView detail = controller.department(7L);
        List<PlatformRoleView> roleList = controller.roles();
        List<PlatformPostView> postList = controller.posts();

        Assertions.assertEquals("研发部", deptList.get(0).getDeptName());
        Assertions.assertEquals(1L, detail.getParentId());
        Assertions.assertEquals("approver", roleList.get(0).getRoleKey());
        Assertions.assertEquals("POST_A", postList.get(0).getPostCode());
    }

    /**
     * 验证用户角色与岗位关联接口返回关系数据。
     */
    @Test
    void shouldExposeUserRoleAndPostLinks() {
        SysUserRole userRole = new SysUserRole();
        userRole.setTenantId("TENANT_A");
        userRole.setUserId(11L);
        userRole.setRoleId(3L);
        SysUserPost userPost = new SysUserPost();
        userPost.setTenantId("TENANT_A");
        userPost.setUserId(11L);
        userPost.setPostId(5L);
        when(userRoleService.list(org.mockito.ArgumentMatchers.<com.baomidou.mybatisplus.core.conditions.Wrapper<SysUserRole>>any()))
                .thenReturn(Collections.singletonList(userRole));
        when(userPostService.list(org.mockito.ArgumentMatchers.<com.baomidou.mybatisplus.core.conditions.Wrapper<SysUserPost>>any()))
                .thenReturn(Collections.singletonList(userPost));

        List<PlatformUserRoleLink> roleLinks = controller.userRoleLinks("3");
        List<PlatformUserPostLink> postLinks = controller.userPostLinks("5");

        Assertions.assertEquals(1, roleLinks.size());
        Assertions.assertEquals(11L, roleLinks.get(0).getUserId());
        Assertions.assertEquals(3L, roleLinks.get(0).getRoleId());
        Assertions.assertEquals(1, postLinks.size());
        Assertions.assertEquals(5L, postLinks.get(0).getPostId());
    }

    /**
     * 验证 AI 配置、策略、审计接口返回租户隔离数据。
     */
    @Test
    void shouldExposeAiConfigPolicyAndAuditViews() {
        when(securityUserResolver.getCurrentTenantId()).thenReturn("TENANT_A");

        PlatformAiConfigView configView = new PlatformAiConfigView();
        configView.setTenantId("TENANT_A");
        configView.setEnabled(true);
        configView.setModel("gpt-5.1");
        when(aiConfigService.getTenantConfig("TENANT_A")).thenReturn(configView);

        PlatformAiActionPolicyItem policyItem = new PlatformAiActionPolicyItem();
        policyItem.setActionKey("todo_finish");
        policyItem.setEnabled(true);
        policyItem.setRiskLevel("high");
        when(aiConfigService.listActionPolicies("TENANT_A")).thenReturn(Collections.singletonList(policyItem));

        PlatformAiAuditView auditView = new PlatformAiAuditView();
        auditView.setTenantId("TENANT_A");
        auditView.setQuestionType("todo_summary");
        when(aiAuditService.listByTenant("TENANT_A", 20)).thenReturn(Collections.singletonList(auditView));

        PlatformAiConfigView responseConfig = controller.aiConfig();
        List<PlatformAiActionPolicyItem> responsePolicy = controller.aiActionPolicies();
        List<PlatformAiAuditView> responseAudit = controller.aiAuditList(20);

        Assertions.assertEquals("TENANT_A", responseConfig.getTenantId());
        Assertions.assertEquals("todo_finish", responsePolicy.get(0).getActionKey());
        Assertions.assertEquals("todo_summary", responseAudit.get(0).getQuestionType());
    }
}

package com.erp.system.saas;

import com.erp.common.core.context.TenantContextHolder;
import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.erp.saas.contract.model.SaasTenantInitializationRequest;
import com.erp.system.domain.SysCompany;
import com.erp.system.domain.SysDept;
import com.erp.system.domain.SysMenu;
import com.erp.system.domain.SysRole;
import com.erp.system.domain.SysSaasProvisioningTask;
import com.erp.system.domain.SysTenant;
import com.erp.system.domain.SysUser;
import com.erp.system.domain.SysUserActivation;
import com.erp.system.mapper.SysSaasProvisioningTaskMapper;
import com.erp.system.mapper.SysTenantMapper;
import com.erp.system.mapper.SysUserActivationMapper;
import com.erp.system.saas.impl.SaasTenantInitializationServiceImpl;
import com.erp.system.service.ISysCompanyService;
import com.erp.system.service.ISysDeptService;
import com.erp.system.service.ISysMenuService;
import com.erp.system.service.ISysRoleService;
import com.erp.system.service.ISysUserService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class SaasTenantInitializationServiceTest {
    private static final Instant NOW = Instant.parse("2026-08-02T12:00:00Z");
    private SysTenantMapper tenantMapper;
    private SysSaasProvisioningTaskMapper taskMapper;
    private SysUserActivationMapper activationMapper;
    private ISysCompanyService companyService;
    private ISysDeptService deptService;
    private ISysRoleService roleService;
    private ISysMenuService menuService;
    private ISysUserService userService;
    private SaasSecureTokenService tokenService;
    private SaasTenantInitializationService service;

    @BeforeEach
    void setUp() {
        tenantMapper = mock(SysTenantMapper.class);
        taskMapper = mock(SysSaasProvisioningTaskMapper.class);
        activationMapper = mock(SysUserActivationMapper.class);
        companyService = mock(ISysCompanyService.class);
        deptService = mock(ISysDeptService.class);
        roleService = mock(ISysRoleService.class);
        menuService = mock(ISysMenuService.class);
        userService = mock(ISysUserService.class);
        tokenService = mock(SaasSecureTokenService.class);
        service = new SaasTenantInitializationServiceImpl(tenantMapper, taskMapper, activationMapper,
                companyService, deptService, roleService, menuService, userService, tokenService,
                Clock.fixed(NOW, ZoneOffset.UTC), 24);
        TenantContextHolder.setTenantId("000000");
    }

    @AfterEach
    void tearDown() {
        TenantContextHolder.clear();
    }

    @Test
    void shouldCreateTenantGraphAndReturnOneTimeActivationToken() {
        SysMenu menu = new SysMenu();
        menu.setMenuId(99L);
        when(taskMapper.insertProcessing(any())).thenReturn(1);
        when(tenantMapper.findByTenantIdForUpdate("tenant-a")).thenReturn(null);
        when(menuService.list(org.mockito.ArgumentMatchers.<Wrapper<SysMenu>>any())).thenReturn(List.of(menu));
        when(tokenService.generate()).thenReturn(
                new SaasSecureTokenService.SecureToken("activation-raw", "activation-hash"),
                new SaasSecureTokenService.SecureToken("password-placeholder", "password-hash"));
        assignIds();
        when(activationMapper.insert(any())).thenReturn(1);
        when(taskMapper.markSucceeded(any(), any(), any(), any(), any(), any(), any(), any(), any()))
                .thenReturn(1);

        var result = service.initialize(request());

        assertThat(result.getActivationToken()).isEqualTo("activation-raw");
        assertThat(result.getActivationExpiresAtEpochMs()).isEqualTo(NOW.plusSeconds(24 * 3600).toEpochMilli());
        assertThat(result.isReplayed()).isFalse();
        assertThat(TenantContextHolder.getTenantId()).isEqualTo("000000");

        ArgumentCaptor<SysRole> role = ArgumentCaptor.forClass(SysRole.class);
        verify(roleService).save(role.capture());
        assertThat(role.getValue().getRoleKey()).isEqualTo("tenant_admin");
        assertThat(role.getValue().getMenuIds()).containsExactly(99L);

        ArgumentCaptor<SysUser> user = ArgumentCaptor.forClass(SysUser.class);
        verify(userService).save(user.capture());
        assertThat(user.getValue().getStatus()).isEqualTo("1");
        assertThat(user.getValue().getPassword()).isEqualTo("password-placeholder");

        ArgumentCaptor<SysUserActivation> activation = ArgumentCaptor.forClass(SysUserActivation.class);
        verify(activationMapper).insert(activation.capture());
        assertThat(activation.getValue().getTokenHash()).isEqualTo("activation-hash");
        assertThat(activation.getValue().getStatus()).isEqualTo("PENDING");
    }

    @Test
    void shouldReplayCompletedRequestWithoutCreatingDuplicatesOrReturningStoredSecret() {
        ArgumentCaptor<SysSaasProvisioningTask> task = ArgumentCaptor.forClass(SysSaasProvisioningTask.class);
        when(taskMapper.insertProcessing(task.capture())).thenReturn(1, 0);
        when(tenantMapper.findByTenantIdForUpdate("tenant-a")).thenReturn(null);
        when(menuService.list(org.mockito.ArgumentMatchers.<Wrapper<SysMenu>>any())).thenReturn(List.of());
        when(tokenService.generate()).thenReturn(
                new SaasSecureTokenService.SecureToken("activation-raw", "activation-hash"),
                new SaasSecureTokenService.SecureToken("placeholder", "placeholder-hash"));
        assignIds();
        when(activationMapper.insert(any())).thenReturn(1);
        when(taskMapper.markSucceeded(any(), any(), any(), any(), any(), any(), any(), any(), any()))
                .thenReturn(1);

        var first = service.initialize(request());
        SysSaasProvisioningTask completed = task.getValue();
        completed.setStatus("SUCCEEDED");
        completed.setTenantRecordId(1L);
        completed.setCompanyId(2L);
        completed.setDeptId(3L);
        completed.setRoleId(4L);
        completed.setUserId(5L);
        completed.setActivationExpiresAt(LocalDateTime.ofInstant(NOW.plusSeconds(86_400), ZoneOffset.UTC));
        when(taskMapper.lock("tenant-a", "req-1")).thenReturn(completed);
        var replay = service.initialize(request());

        assertThat(first.getActivationToken()).isEqualTo("activation-raw");
        assertThat(replay.getActivationToken()).isNull();
        assertThat(replay.isReplayed()).isTrue();
        verify(tenantMapper, times(1)).insert(any(SysTenant.class));
        verify(companyService, times(1)).createCompany(any(SysCompany.class));
        verify(deptService, times(1)).createDept(any(SysDept.class));
        verify(roleService, times(1)).save(any(SysRole.class));
        verify(userService, times(1)).save(any(SysUser.class));
    }

    private void assignIds() {
        doAnswer(invocation -> { ((SysTenant) invocation.getArgument(0)).setId(1L); return 1; })
                .when(tenantMapper).insert(any(SysTenant.class));
        doAnswer(invocation -> { ((SysCompany) invocation.getArgument(0)).setCompanyId(2L); return true; })
                .when(companyService).createCompany(any(SysCompany.class));
        doAnswer(invocation -> { ((SysDept) invocation.getArgument(0)).setDeptId(3L); return true; })
                .when(deptService).createDept(any(SysDept.class));
        doAnswer(invocation -> { ((SysRole) invocation.getArgument(0)).setRoleId(4L); return true; })
                .when(roleService).save(any(SysRole.class));
        doAnswer(invocation -> { ((SysUser) invocation.getArgument(0)).setUserId(5L); return true; })
                .when(userService).save(any(SysUser.class));
    }

    private SaasTenantInitializationRequest request() {
        return new SaasTenantInitializationRequest("req-1", "tenant-a", "Acme Tenant",
                "ACME", "Acme Company", "acme.admin", "Acme Admin", "admin@acme.test");
    }
}

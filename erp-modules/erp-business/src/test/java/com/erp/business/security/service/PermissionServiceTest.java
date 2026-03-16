package com.erp.business.security.service;

import com.erp.business.mapper.SecurityPermissionMapper;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.Collections;

import static org.mockito.Mockito.when;

/**
 * 接口权限校验服务单元测试。
 */
@ExtendWith(MockitoExtension.class)
class PermissionServiceTest {

    @Mock
    private SecurityUserResolver securityUserResolver;

    @Mock
    private SecurityPermissionMapper permissionMapper;

    /**
     * 验证按租户和用户名可以命中库存权限。
     */
    @Test
    void shouldMatchPermissionByTenantAndUserName() {
        when(securityUserResolver.getCurrentUsername()).thenReturn("tester");
        when(securityUserResolver.getCurrentTenantId()).thenReturn("TENANT_A");
        when(permissionMapper.selectRoleKeysByUserName("TENANT_A", "tester")).thenReturn(Collections.singletonList("user"));
        when(permissionMapper.selectPermissionsByUserName("TENANT_A", "tester"))
                .thenReturn(Arrays.asList("business:inventory:ledger:list", "business:inventory:inbound:*"));

        PermissionService permissionService = new PermissionService(securityUserResolver, permissionMapper);

        Assertions.assertTrue(permissionService.hasPermi("business:inventory:ledger:list"));
        Assertions.assertTrue(permissionService.hasPermi("business:inventory:inbound:execute"));
        Assertions.assertFalse(permissionService.hasPermi("business:inventory:outbound:list"));
    }

    /**
     * 验证管理员角色拥有全部权限。
     */
    @Test
    void shouldTreatAdminRoleAsSuperAdmin() {
        when(securityUserResolver.getCurrentUsername()).thenReturn("admin");
        when(securityUserResolver.getCurrentTenantId()).thenReturn("TENANT_A");
        when(permissionMapper.selectRoleKeysByUserName("TENANT_A", "admin")).thenReturn(Collections.singletonList("admin"));

        PermissionService permissionService = new PermissionService(securityUserResolver, permissionMapper);

        Assertions.assertTrue(permissionService.hasPermi("business:inventory:outbound:execute"));
    }
}

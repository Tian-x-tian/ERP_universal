package com.erp.business.security.service;

import com.erp.common.client.internal.InternalSystemClient;
import com.erp.platform.contract.model.PlatformAuthorityBundle;
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
    private InternalSystemClient internalSystemClient;

    /**
     * 验证按租户和用户名可以命中库存权限。
     */
    @Test
    void shouldMatchPermissionByTenantAndUserName() {
        when(securityUserResolver.getCurrentUserId()).thenReturn(1L);
        PlatformAuthorityBundle authorityBundle = new PlatformAuthorityBundle();
        authorityBundle.setRoleKeys(Collections.singletonList("user"));
        authorityBundle.setPermissions(Arrays.asList("business:inventory:ledger:list", "business:inventory:inbound:*"));
        when(internalSystemClient.getAuthorities()).thenReturn(authorityBundle);

        PermissionService permissionService = new PermissionService(securityUserResolver, internalSystemClient);

        Assertions.assertTrue(permissionService.hasPermi("business:inventory:ledger:list"));
        Assertions.assertTrue(permissionService.hasPermi("business:inventory:inbound:execute"));
        Assertions.assertFalse(permissionService.hasPermi("business:inventory:outbound:list"));
    }

    /**
     * 验证管理员角色拥有全部权限。
     */
    @Test
    void shouldTreatAdminRoleAsSuperAdmin() {
        when(securityUserResolver.getCurrentUserId()).thenReturn(1L);
        PlatformAuthorityBundle authorityBundle = new PlatformAuthorityBundle();
        authorityBundle.setRoleKeys(Collections.singletonList("admin"));
        when(internalSystemClient.getAuthorities()).thenReturn(authorityBundle);

        PermissionService permissionService = new PermissionService(securityUserResolver, internalSystemClient);

        Assertions.assertTrue(permissionService.hasPermi("business:inventory:outbound:execute"));
    }
}


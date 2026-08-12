package com.erp.workflow.security.service;

import com.erp.common.client.internal.InternalPlatformClient;
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
 * 工作流权限校验服务单元测试。
 */
@ExtendWith(MockitoExtension.class)
class PermissionServiceTest {

    @Mock
    private SecurityUserResolver securityUserResolver;

    @Mock
    private InternalPlatformClient internalPlatformClient;

    /**
     * 验证普通权限与前缀通配权限可以正确命中。
     */
    @Test
    void shouldMatchWorkflowPermissionAndWildcard() {
        when(securityUserResolver.getCurrentUserId()).thenReturn(1L);
        PlatformAuthorityBundle authorityBundle = new PlatformAuthorityBundle();
        authorityBundle.setRoleKeys(Collections.singletonList("user"));
        authorityBundle.setPermissions(Arrays.asList("workflow:todo:list", "workflow:definition:*"));
        when(internalPlatformClient.getAuthorities()).thenReturn(authorityBundle);

        PermissionService permissionService = new PermissionService(securityUserResolver, internalPlatformClient);

        Assertions.assertTrue(permissionService.hasPermi("workflow:todo:list"));
        Assertions.assertTrue(permissionService.hasPermi("workflow:definition:publish"));
        Assertions.assertFalse(permissionService.hasPermi("workflow:instance:sla"));
    }

    /**
     * 验证管理员角色拥有全部权限。
     */
    @Test
    void shouldTreatAdminRoleAsSuperAdmin() {
        when(securityUserResolver.getCurrentUserId()).thenReturn(1L);
        PlatformAuthorityBundle authorityBundle = new PlatformAuthorityBundle();
        authorityBundle.setRoleKeys(Collections.singletonList("admin"));
        when(internalPlatformClient.getAuthorities()).thenReturn(authorityBundle);

        PermissionService permissionService = new PermissionService(securityUserResolver, internalPlatformClient);

        Assertions.assertTrue(permissionService.hasPermi("workflow:instance:sla"));
    }
}

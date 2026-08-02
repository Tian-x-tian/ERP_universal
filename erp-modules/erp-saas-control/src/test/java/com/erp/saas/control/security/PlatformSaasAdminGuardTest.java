package com.erp.saas.control.security;

import com.erp.common.client.internal.InternalSystemClient;
import com.erp.common.security.AuthenticatedUserPrincipal;
import com.erp.platform.contract.model.PlatformAuthorityBundle;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PlatformSaasAdminGuardTest {
    @Test
    void shouldRequirePlatformTenantAndAdminRole() {
        InternalSystemClient client = mock(InternalSystemClient.class);
        PlatformSaasAdminGuard guard = new PlatformSaasAdminGuard(client);
        PlatformAuthorityBundle bundle = new PlatformAuthorityBundle();
        bundle.setRoleKeys(List.of("admin"));
        when(client.getAuthorities()).thenReturn(bundle);
        var authentication = authentication("000000");

        assertThat(guard.requireAdmin(authentication)).isEqualTo("admin-user");
        verify(client).getAuthorities();

        assertThatThrownBy(() -> guard.requireAdmin(authentication("tenant-a")))
                .isInstanceOf(AccessDeniedException.class);
        verify(client, org.mockito.Mockito.times(1)).getAuthorities();
    }

    private UsernamePasswordAuthenticationToken authentication(String tenantId) {
        return new UsernamePasswordAuthenticationToken(new AuthenticatedUserPrincipal(
                1L, "admin-user", tenantId, 0, Long.MAX_VALUE), null, List.of());
    }
}

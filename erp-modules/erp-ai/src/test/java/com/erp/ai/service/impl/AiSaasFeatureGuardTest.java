package com.erp.ai.service.impl;

import com.erp.common.client.internal.InternalSystemClient;
import com.erp.common.core.context.TenantContextHolder;
import com.erp.saas.contract.model.SaasFeatureKeys;
import com.erp.saas.contract.model.SaasRuntimeAccess;
import com.erp.saas.contract.model.TenantLifecycleState;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.AccessDeniedException;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AiSaasFeatureGuardTest {
    private final InternalSystemClient systemClient = mock(InternalSystemClient.class);
    private final AiSaasFeatureGuard guard = new AiSaasFeatureGuard(systemClient);

    @AfterEach
    void clearContext() {
        TenantContextHolder.clear();
    }

    @Test
    void shouldAllowEnabledFeatureForWritableTenant() {
        TenantContextHolder.setTenantId("tenant-a");
        when(systemClient.getSaasRuntimeAccess()).thenReturn(access(true, true));

        assertThatCode(guard::requirePaidAccess).doesNotThrowAnyException();
    }

    @Test
    void shouldDenyMissingFeatureAndReadOnlyTenant() {
        TenantContextHolder.setTenantId("tenant-a");
        when(systemClient.getSaasRuntimeAccess()).thenReturn(access(true, false), access(false, true));

        assertThatThrownBy(guard::requirePaidAccess).isInstanceOf(AccessDeniedException.class);
        assertThatThrownBy(guard::requirePaidAccess).isInstanceOf(AccessDeniedException.class);
    }

    private SaasRuntimeAccess access(boolean writeAllowed, boolean featureEnabled) {
        return new SaasRuntimeAccess("tenant-a", TenantLifecycleState.ACTIVE, false, true,
                writeAllowed, Map.of(SaasFeatureKeys.AI_ASSISTANT, featureEnabled));
    }
}

package com.erp.system.controller;

import com.erp.saas.contract.model.SaasTenantActivationReissueRequest;
import com.erp.saas.contract.model.SaasTenantActivationReissueResult;
import com.erp.saas.contract.model.SaasTenantInitializationRequest;
import com.erp.saas.contract.model.SaasTenantInitializationResult;
import com.erp.saas.contract.model.SaasRuntimeAccess;
import com.erp.saas.contract.model.TenantLifecycleState;
import com.erp.common.core.context.TenantContextHolder;
import com.erp.system.saas.SaasRuntimeEntitlements;
import com.erp.system.saas.SaasRuntimeSnapshotService;
import com.erp.system.saas.SaasTenantPurgeService;
import com.erp.system.saas.SaasRuntimeSource;
import com.erp.system.saas.SaasTenantActivationReissueService;
import com.erp.system.saas.SaasTenantInitializationService;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class SystemSaasInternalControllerTest {
    @Test
    void shouldDelegateTenantInitialization() {
        SaasTenantInitializationService service = mock(SaasTenantInitializationService.class);
        SystemSaasInternalController controller = new SystemSaasInternalController(
                service, mock(SaasTenantActivationReissueService.class),
                mock(SaasRuntimeSnapshotService.class), mock(SaasTenantPurgeService.class));
        SaasTenantInitializationRequest request = new SaasTenantInitializationRequest();
        SaasTenantInitializationResult expected = new SaasTenantInitializationResult();
        when(service.initialize(request)).thenReturn(expected);

        SaasTenantInitializationResult actual = controller.initializeTenant(request);

        assertThat(actual).isSameAs(expected);
        verify(service).initialize(request);
    }

    @Test
    void shouldDelegateActivationReissue() {
        SaasTenantInitializationService initializationService = mock(SaasTenantInitializationService.class);
        SaasTenantActivationReissueService reissueService = mock(SaasTenantActivationReissueService.class);
        SystemSaasInternalController controller = new SystemSaasInternalController(
                initializationService, reissueService, mock(SaasRuntimeSnapshotService.class),
                mock(SaasTenantPurgeService.class));
        SaasTenantActivationReissueRequest request = new SaasTenantActivationReissueRequest(
                "req-1", "tenant-a");
        SaasTenantActivationReissueResult expected = new SaasTenantActivationReissueResult();
        when(reissueService.reissue(request)).thenReturn(expected);

        assertThat(controller.reissueActivation(request)).isSameAs(expected);

        verify(reissueService).reissue(request);
    }

    @Test
    void shouldExposeCurrentTenantRuntimeAccess() {
        SaasRuntimeSnapshotService snapshotService = mock(SaasRuntimeSnapshotService.class);
        SystemSaasInternalController controller = new SystemSaasInternalController(
                mock(SaasTenantInitializationService.class),
                mock(SaasTenantActivationReissueService.class), snapshotService,
                mock(SaasTenantPurgeService.class));
        TenantContextHolder.setTenantId("tenant-a");
        when(snapshotService.current("tenant-a")).thenReturn(new SaasRuntimeEntitlements(
                "tenant-a", TenantLifecycleState.READ_ONLY, 3L, SaasRuntimeSource.LOCAL_CACHE,
                false, true, false, java.util.Map.of(), java.util.Map.of()));

        try {
            SaasRuntimeAccess access = controller.runtimeAccess();

            assertThat(access.getTenantId()).isEqualTo("tenant-a");
            assertThat(access.getLifecycleState()).isEqualTo(TenantLifecycleState.READ_ONLY);
            assertThat(access.isWriteAllowed()).isFalse();
            verify(snapshotService).current("tenant-a");
        } finally {
            TenantContextHolder.clear();
        }
    }
}

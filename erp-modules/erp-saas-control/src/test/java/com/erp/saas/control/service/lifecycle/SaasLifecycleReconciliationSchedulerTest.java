package com.erp.saas.control.service.lifecycle;

import com.erp.saas.control.mapper.SaasSubscriptionMapper;
import com.erp.saas.control.service.ControlUtcTime;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class SaasLifecycleReconciliationSchedulerTest {
    @Test
    void shouldContinueReconcilingAfterOneTenantFails() {
        SaasSubscriptionMapper mapper = mock(SaasSubscriptionMapper.class);
        SaasTenantLifecycleService service = mock(SaasTenantLifecycleService.class);
        ControlUtcTime time = mock(ControlUtcTime.class);
        LocalDateTime now = LocalDateTime.of(2026, 8, 2, 10, 0);
        when(time.now()).thenReturn(now);
        when(mapper.findDueTenantIds(now, 100)).thenReturn(List.of("tenant-a", "tenant-b"));
        doThrow(new SaasLifecycleException(SaasLifecycleException.ErrorCode.VERSION_CONFLICT, "conflict"))
                .when(service).reconcile("tenant-a", "saas-lifecycle-scheduler");
        SaasLifecycleReconciliationScheduler scheduler =
                new SaasLifecycleReconciliationScheduler(mapper, service, time, 100);

        scheduler.reconcileDueTenants();

        verify(service).reconcile("tenant-a", "saas-lifecycle-scheduler");
        verify(service).reconcile("tenant-b", "saas-lifecycle-scheduler");
    }
}

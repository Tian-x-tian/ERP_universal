package com.erp.system.saas;

import com.erp.common.core.context.TenantContextHolder;
import com.erp.system.mapper.SysTenantMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class SaasSnapshotRefreshSchedulerTest {
    @AfterEach
    void clearContext() {
        TenantContextHolder.clear();
    }

    @Test
    void shouldRefreshEveryBusinessTenantAndAlwaysClearContext() {
        SysTenantMapper tenantMapper = mock(SysTenantMapper.class);
        SaasRuntimeSnapshotService service = mock(SaasRuntimeSnapshotService.class);
        when(tenantMapper.findActiveTenantIds()).thenReturn(List.of("000000", "tenant_1", "tenant_2"));
        doThrow(new IllegalStateException("control unavailable")).when(service).refresh("tenant_2");
        SaasSnapshotRefreshScheduler scheduler = new SaasSnapshotRefreshScheduler(tenantMapper, service);

        scheduler.refreshAll();

        verify(service).refresh("tenant_1");
        verify(service).refresh("tenant_2");
        assertThat(TenantContextHolder.getTenantId()).isNull();
    }
}

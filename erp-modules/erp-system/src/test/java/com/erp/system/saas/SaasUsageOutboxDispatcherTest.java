package com.erp.system.saas;

import com.erp.common.client.internal.InternalSaasClient;
import com.erp.common.core.context.TenantContextHolder;
import com.erp.saas.contract.model.SaasQuotaKeys;
import com.erp.saas.contract.model.SaasUsageEvent;
import com.erp.saas.contract.model.SaasUsageOperation;
import com.erp.system.domain.SysSaasUsageOutbox;
import com.erp.system.mapper.SysSaasUsageOutboxMapper;
import com.erp.system.mapper.SysTenantMapper;
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
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class SaasUsageOutboxDispatcherTest {
    private static final Instant NOW = Instant.parse("2026-08-02T04:00:00Z");
    private SysTenantMapper tenantMapper;
    private SysSaasUsageOutboxMapper outboxMapper;
    private InternalSaasClient saasClient;
    private SaasUsageOutboxDispatcher dispatcher;

    @BeforeEach
    void setUp() {
        tenantMapper = mock(SysTenantMapper.class);
        outboxMapper = mock(SysSaasUsageOutboxMapper.class);
        saasClient = mock(InternalSaasClient.class);
        dispatcher = new SaasUsageOutboxDispatcher(tenantMapper, outboxMapper, saasClient,
                Clock.fixed(NOW, ZoneOffset.UTC));
        when(tenantMapper.findActiveTenantIds()).thenReturn(List.of("000000", "tenant-a"));
    }

    @AfterEach
    void tearDown() {
        TenantContextHolder.clear();
    }

    @Test
    void shouldReportPendingSnapshotAndMarkSentOutsideBusinessTransaction() {
        SysSaasUsageOutbox row = row();
        when(outboxMapper.findPending("tenant-a", LocalDateTime.ofInstant(NOW, ZoneOffset.UTC), 100))
                .thenReturn(List.of(row));
        when(outboxMapper.markSent("tenant-a", 10L, LocalDateTime.ofInstant(NOW, ZoneOffset.UTC)))
                .thenReturn(1);

        dispatcher.dispatchAll();

        ArgumentCaptor<SaasUsageEvent> report = ArgumentCaptor.forClass(SaasUsageEvent.class);
        verify(saasClient).reportUsage(report.capture());
        assertThat(report.getValue()).extracting("idempotencyKey", "tenantId", "metricKey", "operation", "amount")
                .containsExactly("report-a", "tenant-a", SaasQuotaKeys.AI_INPUT_TOKENS,
                        SaasUsageOperation.REPORT, 15L);
        verify(outboxMapper).markSent("tenant-a", 10L, LocalDateTime.ofInstant(NOW, ZoneOffset.UTC));
        assertThat(TenantContextHolder.getTenantId()).isNull();
    }

    @Test
    void shouldKeepPendingAndRecordBoundedFailureMetadataWhenControlPlaneIsDown() {
        SysSaasUsageOutbox row = row();
        LocalDateTime now = LocalDateTime.ofInstant(NOW, ZoneOffset.UTC);
        when(outboxMapper.findPending("tenant-a", now, 100)).thenReturn(List.of(row));
        doThrow(new IllegalStateException("secret connection detail"))
                .when(saasClient).reportUsage(org.mockito.ArgumentMatchers.any());

        dispatcher.dispatchAll();

        verify(outboxMapper, never()).markSent(org.mockito.ArgumentMatchers.anyString(),
                org.mockito.ArgumentMatchers.anyLong(), org.mockito.ArgumentMatchers.any());
        verify(outboxMapper).markRetry("tenant-a", 10L, 1, now.plusMinutes(1),
                "IllegalStateException", now);
        assertThat(TenantContextHolder.getTenantId()).isNull();
    }

    @Test
    void shouldContinueWithOtherTenantsWhenOneTenantCannotLoadPendingRows() {
        LocalDateTime now = LocalDateTime.ofInstant(NOW, ZoneOffset.UTC);
        when(tenantMapper.findActiveTenantIds()).thenReturn(List.of("tenant-a", "tenant-b"));
        when(outboxMapper.findPending("tenant-a", now, 100))
                .thenThrow(new IllegalStateException("database detail"));
        when(outboxMapper.findPending("tenant-b", now, 100)).thenReturn(List.of());

        dispatcher.dispatchAll();

        verify(outboxMapper).findPending("tenant-b", now, 100);
        assertThat(TenantContextHolder.getTenantId()).isNull();
    }

    @Test
    void shouldNotOverflowRetryAttemptCount() {
        SysSaasUsageOutbox row = row();
        row.setAttemptCount(Integer.MAX_VALUE);
        LocalDateTime now = LocalDateTime.ofInstant(NOW, ZoneOffset.UTC);
        when(outboxMapper.findPending("tenant-a", now, 100)).thenReturn(List.of(row));
        doThrow(new IllegalStateException("unavailable"))
                .when(saasClient).reportUsage(org.mockito.ArgumentMatchers.any());

        dispatcher.dispatchAll();

        verify(outboxMapper).markRetry("tenant-a", 10L, Integer.MAX_VALUE,
                now.plusMinutes(1), "IllegalStateException", now);
    }

    private SysSaasUsageOutbox row() {
        SysSaasUsageOutbox row = new SysSaasUsageOutbox();
        row.setOutboxId(10L);
        row.setTenantId("tenant-a");
        row.setEventKey("report-a");
        row.setMetricKey(SaasQuotaKeys.AI_INPUT_TOKENS);
        row.setAmount(15L);
        row.setPeriodStart(LocalDateTime.of(2026, 8, 1, 0, 0));
        row.setOccurredAt(LocalDateTime.ofInstant(NOW.minusSeconds(5), ZoneOffset.UTC));
        row.setStatus("PENDING");
        row.setAttemptCount(0);
        return row;
    }
}

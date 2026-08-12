package com.erp.saas.control.service.usage;

import com.erp.saas.contract.model.SaasQuotaKeys;
import com.erp.saas.contract.model.SaasUsageEvent;
import com.erp.saas.contract.model.SaasUsageOperation;
import com.erp.saas.control.domain.entity.SaasTenantEntity;
import com.erp.saas.control.domain.entity.SaasUsageEventEntity;
import com.erp.saas.control.domain.entity.SaasUsageSummaryEntity;
import com.erp.saas.control.mapper.SaasTenantMapper;
import com.erp.saas.control.mapper.SaasUsageEventMapper;
import com.erp.saas.control.mapper.SaasUsageSummaryMapper;
import com.erp.saas.control.service.ControlUtcTime;
import com.erp.saas.control.service.usage.impl.SaasUsageAggregationServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.dao.DuplicateKeyException;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class SaasUsageAggregationServiceTest {
    private static final Instant NOW = Instant.parse("2026-08-02T04:00:00Z");
    private static final long AUGUST_2026 = Instant.parse("2026-08-01T00:00:00Z").toEpochMilli();

    private SaasTenantMapper tenantMapper;
    private SaasUsageEventMapper eventMapper;
    private SaasUsageSummaryMapper summaryMapper;
    private SaasUsageAggregationService service;

    @BeforeEach
    void setUp() {
        tenantMapper = mock(SaasTenantMapper.class);
        eventMapper = mock(SaasUsageEventMapper.class);
        summaryMapper = mock(SaasUsageSummaryMapper.class);
        service = new SaasUsageAggregationServiceImpl(tenantMapper, eventMapper, summaryMapper,
                new ControlUtcTime(Clock.fixed(NOW, ZoneOffset.UTC)));
        SaasTenantEntity tenant = new SaasTenantEntity();
        tenant.setTenantId("tenant-a");
        when(tenantMapper.findByTenantId("tenant-a")).thenReturn(tenant);
        when(eventMapper.insert(any())).thenReturn(1);
        when(summaryMapper.upsertLatest(any())).thenReturn(1);
    }

    @Test
    void shouldStoreIdempotentEventAndUpsertAbsoluteMonthlySnapshot() {
        SaasUsageEvent report = report("event-a", 321L, NOW.minusSeconds(10).toEpochMilli());

        service.report(report, "erp-system");

        ArgumentCaptor<SaasUsageEventEntity> event = ArgumentCaptor.forClass(SaasUsageEventEntity.class);
        ArgumentCaptor<SaasUsageSummaryEntity> summary = ArgumentCaptor.forClass(SaasUsageSummaryEntity.class);
        verify(eventMapper).insert(event.capture());
        verify(summaryMapper).upsertLatest(summary.capture());
        assertThat(event.getValue()).extracting("idempotencyKey", "tenantId", "metricKey", "amount", "operation")
                .containsExactly("event-a", "tenant-a", SaasQuotaKeys.AI_INPUT_TOKENS, 321L,
                        SaasUsageOperation.REPORT);
        assertThat(summary.getValue()).extracting("tenantId", "metricKey", "usedAmount", "lastEventKey")
                .containsExactly("tenant-a", SaasQuotaKeys.AI_INPUT_TOKENS, 321L, "event-a");
        assertThat(summary.getValue().getPeriodStart()).isEqualTo(LocalDateTime.of(2026, 8, 1, 0, 0));
        assertThat(summary.getValue().getUpdateBy()).isEqualTo("erp-system");
    }

    @Test
    void shouldTreatSameIdempotencyPayloadAsSuccessfulReplay() {
        SaasUsageEvent report = report("event-a", 321L, NOW.minusSeconds(10).toEpochMilli());
        SaasUsageEventEntity stored = stored(report);
        when(eventMapper.insert(any())).thenThrow(new DuplicateKeyException("duplicate"));
        when(eventMapper.findByIdempotencyKey("event-a")).thenReturn(stored);

        service.report(report, "erp-system");

        verify(summaryMapper, never()).upsertLatest(any());
    }

    @Test
    void shouldAcceptStaleSnapshotWhenDatabaseKeepsTheNewerSummary() {
        SaasUsageEvent stale = report("event-stale", 100L, NOW.minusSeconds(60).toEpochMilli());
        when(summaryMapper.upsertLatest(any())).thenReturn(0);

        service.report(stale, "erp-system");

        verify(eventMapper).insert(any());
        verify(summaryMapper).upsertLatest(any());
    }

    @Test
    void shouldRejectDifferentPayloadReusingIdempotencyKey() {
        SaasUsageEvent report = report("event-a", 999L, NOW.minusSeconds(10).toEpochMilli());
        SaasUsageEventEntity stored = stored(report("event-a", 321L, report.getOccurredAtEpochMs()));
        when(eventMapper.insert(any())).thenThrow(new DuplicateKeyException("duplicate"));
        when(eventMapper.findByIdempotencyKey("event-a")).thenReturn(stored);

        assertThatThrownBy(() -> service.report(report, "erp-system"))
                .isInstanceOf(SaasUsageException.class)
                .extracting(error -> ((SaasUsageException) error).getErrorCode())
                .isEqualTo(SaasUsageException.ErrorCode.IDEMPOTENCY_CONFLICT);
        verify(summaryMapper, never()).upsertLatest(any());
    }

    @Test
    void shouldRejectNonReportOperationsAndUnknownTenant() {
        SaasUsageEvent reserve = new SaasUsageEvent("event-a", "tenant-a", SaasQuotaKeys.USER_COUNT,
                SaasUsageOperation.RESERVE, "user-a", 1L, null, NOW.toEpochMilli());
        assertThatThrownBy(() -> service.report(reserve, "erp-system"))
                .isInstanceOf(SaasUsageException.class)
                .extracting(error -> ((SaasUsageException) error).getErrorCode())
                .isEqualTo(SaasUsageException.ErrorCode.INVALID_INPUT);

        when(tenantMapper.findByTenantId("tenant-a")).thenReturn(null);
        assertThatThrownBy(() -> service.report(report("event-b", 1L, NOW.toEpochMilli()), "erp-system"))
                .isInstanceOf(SaasUsageException.class)
                .extracting(error -> ((SaasUsageException) error).getErrorCode())
                .isEqualTo(SaasUsageException.ErrorCode.TENANT_NOT_FOUND);
    }

    private SaasUsageEvent report(String key, long amount, long occurredAt) {
        return new SaasUsageEvent(key, "tenant-a", SaasQuotaKeys.AI_INPUT_TOKENS,
                SaasUsageOperation.REPORT, null, amount, AUGUST_2026, occurredAt);
    }

    private SaasUsageEventEntity stored(SaasUsageEvent report) {
        SaasUsageEventEntity row = new SaasUsageEventEntity();
        row.setIdempotencyKey(report.getIdempotencyKey());
        row.setTenantId(report.getTenantId());
        row.setMetricKey(report.getMetricKey());
        row.setOperation(report.getOperation());
        row.setAmount(report.getAmount());
        row.setPeriodStart(LocalDateTime.ofInstant(Instant.ofEpochMilli(report.getPeriodStartEpochMs()), ZoneOffset.UTC));
        row.setOccurredAt(LocalDateTime.ofInstant(Instant.ofEpochMilli(report.getOccurredAtEpochMs()), ZoneOffset.UTC));
        return row;
    }
}

package com.erp.system.saas;

import com.erp.common.core.context.TenantContextHolder;
import com.erp.common.core.exception.ServiceException;
import com.erp.saas.contract.model.SaasQuotaKeys;
import com.erp.saas.contract.model.SaasQuotaUsage;
import com.erp.saas.contract.model.SaasUsageEvent;
import com.erp.saas.contract.model.SaasUsageOperation;
import com.erp.saas.contract.model.TenantLifecycleState;
import com.erp.system.domain.SysSaasQuotaCounter;
import com.erp.system.domain.SysSaasQuotaReservation;
import com.erp.system.mapper.SysSaasQuotaCounterMapper;
import com.erp.system.mapper.SysSaasQuotaReservationMapper;
import com.erp.system.mapper.SysUserMapper;
import com.erp.system.saas.impl.SaasLocalQuotaServiceImpl;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class SaasLocalQuotaServiceTest {
    private static final Instant NOW = Instant.parse("2026-08-02T03:00:00Z");
    private SysSaasQuotaCounterMapper counterMapper;
    private SysSaasQuotaReservationMapper reservationMapper;
    private SysUserMapper userMapper;
    private SaasRuntimeSnapshotService snapshotService;
    private SaasLocalQuotaService service;

    @BeforeEach
    void setUp() {
        counterMapper = mock(SysSaasQuotaCounterMapper.class);
        reservationMapper = mock(SysSaasQuotaReservationMapper.class);
        userMapper = mock(SysUserMapper.class);
        snapshotService = mock(SaasRuntimeSnapshotService.class);
        service = new SaasLocalQuotaServiceImpl(counterMapper, reservationMapper, userMapper,
                snapshotService, Clock.fixed(NOW, ZoneOffset.UTC));
        TenantContextHolder.setTenantId("tenant-a");
        when(snapshotService.current("tenant-a")).thenReturn(entitlements(true, 2L));
    }

    @AfterEach
    void tearDown() {
        TenantContextHolder.clear();
    }

    @Test
    void shouldReserveWithinLimitUsingExistingActiveUsersAsInitialBaseline() {
        LocalDateTime period = LocalDateTime.of(1970, 1, 1, 0, 0);
        when(userMapper.countActiveUsers()).thenReturn(1L);
        when(reservationMapper.findForUpdate("tenant-a", SaasQuotaKeys.USER_COUNT, "create-1"))
                .thenReturn(null);
        when(counterMapper.findForUpdate("tenant-a", SaasQuotaKeys.USER_COUNT, period))
                .thenReturn(counter(1L, 0L));
        when(counterMapper.addReservation("tenant-a", SaasQuotaKeys.USER_COUNT, period,
                1L, 2L, "quota-service", LocalDateTime.ofInstant(NOW, ZoneOffset.UTC))).thenReturn(1);
        when(reservationMapper.insert(any())).thenReturn(1);

        SaasQuotaUsage usage = service.apply(event(SaasUsageOperation.RESERVE, "create-1", 1L));

        assertThat(usage.getUsed()).isEqualTo(1L);
        assertThat(usage.getReserved()).isEqualTo(1L);
        verify(counterMapper).ensureCounter("tenant-a", SaasQuotaKeys.USER_COUNT, period,
                1L, "quota-service", LocalDateTime.ofInstant(NOW, ZoneOffset.UTC));
    }

    @Test
    void shouldRejectReservationAtConcurrentBoundaryAndReadOnlyRuntime() {
        LocalDateTime period = LocalDateTime.of(1970, 1, 1, 0, 0);
        when(reservationMapper.findForUpdate("tenant-a", SaasQuotaKeys.USER_COUNT, "create-1"))
                .thenReturn(null);
        when(counterMapper.findForUpdate("tenant-a", SaasQuotaKeys.USER_COUNT, period))
                .thenReturn(counter(2L, 0L));
        when(counterMapper.addReservation(any(), any(), any(), any(), any(), any(), any()))
                .thenReturn(0);

        assertThatThrownBy(() -> service.apply(event(SaasUsageOperation.RESERVE, "create-1", 1L)))
                .isInstanceOf(ServiceException.class).hasMessageContaining("配额");
        verify(reservationMapper, never()).insert(any());

        when(snapshotService.current("tenant-a")).thenReturn(entitlements(false, 10L));
        assertThatThrownBy(() -> service.apply(event(SaasUsageOperation.RESERVE, "create-2", 1L)))
                .isInstanceOf(ServiceException.class).hasMessageContaining("只读");
    }

    @Test
    void shouldSettleReleaseAndKeepRetriesIdempotent() {
        LocalDateTime period = LocalDateTime.of(1970, 1, 1, 0, 0);
        SysSaasQuotaReservation reserved = reservation("RESERVED", 3L, 0L);
        when(reservationMapper.findForUpdate("tenant-a", SaasQuotaKeys.STORAGE_BYTES, "object-1"))
                .thenReturn(reserved);
        when(counterMapper.findForUpdate("tenant-a", SaasQuotaKeys.STORAGE_BYTES, period))
                .thenReturn(counter(5L, 3L), counter(7L, 0L));
        when(counterMapper.settleReservation("tenant-a", SaasQuotaKeys.STORAGE_BYTES, period,
                3L, 2L, "quota-service", LocalDateTime.ofInstant(NOW, ZoneOffset.UTC))).thenReturn(1);
        when(reservationMapper.markSettled(eq("tenant-a"), eq(SaasQuotaKeys.STORAGE_BYTES), eq("object-1"),
                eq(2L), eq("event-SETTLE"), eq("quota-service"), any())).thenReturn(1);

        SaasQuotaUsage settled = service.apply(event(SaasUsageOperation.SETTLE, "object-1", 2L));
        assertThat(settled.getUsed()).isEqualTo(7L);
        assertThat(settled.getReserved()).isZero();

        reserved.setStatus("SETTLED");
        reserved.setSettledAmount(2L);
        when(counterMapper.releaseConsumed("tenant-a", SaasQuotaKeys.STORAGE_BYTES, period,
                2L, "quota-service", LocalDateTime.ofInstant(NOW, ZoneOffset.UTC))).thenReturn(1);
        when(reservationMapper.markReleased(eq("tenant-a"), eq(SaasQuotaKeys.STORAGE_BYTES), eq("object-1"),
                eq("event-RELEASE"), eq("quota-service"), any())).thenReturn(1);
        SaasQuotaUsage released = service.apply(event(SaasUsageOperation.RELEASE, "object-1", null));
        assertThat(released.getUsed()).isEqualTo(5L);
    }

    @Test
    void shouldInitializeMissingUserCounterFromUsageBeforeDecrement() {
        LocalDateTime period = LocalDateTime.of(1970, 1, 1, 0, 0);
        LocalDateTime now = LocalDateTime.ofInstant(NOW, ZoneOffset.UTC);
        when(userMapper.countActiveUsers()).thenReturn(2L);
        when(counterMapper.findForUpdate("tenant-a", SaasQuotaKeys.USER_COUNT, period))
                .thenReturn(counter(3L, 0L));
        when(counterMapper.releaseConsumed("tenant-a", SaasQuotaKeys.USER_COUNT, period,
                1L, "user-service", now)).thenReturn(1);

        SaasQuotaUsage usage = service.decreaseUsed(SaasQuotaKeys.USER_COUNT, 1L, "user-service");

        verify(counterMapper).ensureCounter("tenant-a", SaasQuotaKeys.USER_COUNT, period,
                3L, "quota-service", now);
        assertThat(usage.getUsed()).isEqualTo(2L);
    }

    private SaasUsageEvent event(SaasUsageOperation operation, String reference, Long amount) {
        return new SaasUsageEvent("event-" + operation, "tenant-a",
                reference.startsWith("object") ? SaasQuotaKeys.STORAGE_BYTES : SaasQuotaKeys.USER_COUNT,
                operation, reference, amount, null, NOW.toEpochMilli());
    }

    private SaasRuntimeEntitlements entitlements(boolean writable, Long limit) {
        return new SaasRuntimeEntitlements("tenant-a", TenantLifecycleState.ACTIVE, 1L,
                SaasRuntimeSource.LOCAL_CACHE, false, true, writable, Map.of(),
                Map.of(SaasQuotaKeys.USER_COUNT, limit, SaasQuotaKeys.STORAGE_BYTES, 100L));
    }

    private SysSaasQuotaCounter counter(long used, long reserved) {
        SysSaasQuotaCounter counter = new SysSaasQuotaCounter();
        counter.setTenantId("tenant-a");
        counter.setUsedAmount(used);
        counter.setReservedAmount(reserved);
        return counter;
    }

    private SysSaasQuotaReservation reservation(String status, long reserved, long settled) {
        SysSaasQuotaReservation reservation = new SysSaasQuotaReservation();
        reservation.setTenantId("tenant-a");
        reservation.setMetricKey(SaasQuotaKeys.STORAGE_BYTES);
        reservation.setReservationKey("object-1");
        reservation.setPeriodStart(LocalDateTime.of(1970, 1, 1, 0, 0));
        reservation.setReservedAmount(reserved);
        reservation.setSettledAmount(settled);
        reservation.setStatus(status);
        return reservation;
    }
}

package com.erp.system.saas.impl;

import com.erp.common.core.context.TenantContextHolder;
import com.erp.common.core.domain.ResultCode;
import com.erp.common.core.exception.ServiceException;
import com.erp.saas.contract.model.SaasQuotaKeys;
import com.erp.saas.contract.model.SaasQuotaUsage;
import com.erp.saas.contract.model.SaasUsageEvent;
import com.erp.saas.contract.model.SaasUsageEventValidator;
import com.erp.saas.contract.model.SaasUsageOperation;
import com.erp.system.domain.SysSaasQuotaCounter;
import com.erp.system.domain.SysSaasQuotaReservation;
import com.erp.system.domain.SysSaasUsageOutbox;
import com.erp.system.mapper.SysSaasQuotaCounterMapper;
import com.erp.system.mapper.SysSaasQuotaReservationMapper;
import com.erp.system.mapper.SysSaasUsageOutboxMapper;
import com.erp.system.mapper.SysUserMapper;
import com.erp.system.saas.SaasLocalQuotaService;
import com.erp.system.saas.SaasRuntimeEntitlements;
import com.erp.system.saas.SaasRuntimeSnapshotService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

@Service
public class SaasLocalQuotaServiceImpl implements SaasLocalQuotaService {
    private static final String OPERATOR = "quota-service";
    private static final String RESERVED = "RESERVED";
    private static final String SETTLED = "SETTLED";
    private static final String RELEASED = "RELEASED";
    private static final LocalDateTime NON_PERIODIC = LocalDateTime.of(1970, 1, 1, 0, 0);

    private final SysSaasQuotaCounterMapper counterMapper;
    private final SysSaasQuotaReservationMapper reservationMapper;
    private final SysUserMapper userMapper;
    private final SysSaasUsageOutboxMapper outboxMapper;
    private final SaasRuntimeSnapshotService snapshotService;
    private final Clock clock;

    public SaasLocalQuotaServiceImpl(SysSaasQuotaCounterMapper counterMapper,
            SysSaasQuotaReservationMapper reservationMapper, SysUserMapper userMapper,
            SysSaasUsageOutboxMapper outboxMapper,
            SaasRuntimeSnapshotService snapshotService, Clock clock) {
        this.counterMapper = Objects.requireNonNull(counterMapper, "counterMapper must not be null");
        this.reservationMapper = Objects.requireNonNull(reservationMapper, "reservationMapper must not be null");
        this.userMapper = Objects.requireNonNull(userMapper, "userMapper must not be null");
        this.outboxMapper = Objects.requireNonNull(outboxMapper, "outboxMapper must not be null");
        this.snapshotService = Objects.requireNonNull(snapshotService, "snapshotService must not be null");
        this.clock = Objects.requireNonNull(clock, "clock must not be null");
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public SaasQuotaUsage apply(SaasUsageEvent event) {
        SaasUsageEventValidator.validate(event);
        String tenantId = requireTenant(event.getTenantId());
        if (event.getOperation() == SaasUsageOperation.REPORT) {
            throw validation("REPORT is only accepted by the central usage aggregator");
        }
        LocalDateTime now = LocalDateTime.ofInstant(clock.instant(), ZoneOffset.UTC);
        LocalDateTime period = period(event);
        SaasQuotaUsage usage = switch (event.getOperation()) {
            case RESERVE -> reserve(tenantId, event, period, now);
            case SETTLE -> settle(tenantId, event, period, now);
            case RELEASE -> release(tenantId, event, period, now);
            case REPORT -> throw validation("Unsupported local quota operation");
        };
        if (event.getOperation() != SaasUsageOperation.RESERVE) {
            enqueueUsage(tenantId, usage, now);
        }
        return usage;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public List<SaasQuotaUsage> applyBatch(List<SaasUsageEvent> events) {
        if (events == null || events.isEmpty() || events.size() > 16) {
            throw validation("Quota event batch must contain between 1 and 16 events");
        }
        List<SaasQuotaUsage> usages = new ArrayList<>(events.size());
        for (SaasUsageEvent event : events) {
            usages.add(apply(event));
        }
        return usages;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public SaasQuotaUsage decreaseUsed(String metricKey, long amount, String operator) {
        String tenantId = requireTenant(TenantContextHolder.getTenantId());
        if (!SaasQuotaKeys.isKnown(metricKey) || SaasQuotaKeys.isMonthly(metricKey) || amount <= 0) {
            throw validation("Invalid non-periodic quota decrement");
        }
        LocalDateTime now = LocalDateTime.ofInstant(clock.instant(), ZoneOffset.UTC);
        long baseline = SaasQuotaKeys.USER_COUNT.equals(metricKey)
                ? Math.addExact(userMapper.countActiveUsers(), amount) : 0L;
        SysSaasQuotaCounter counter = counter(tenantId, metricKey, NON_PERIODIC, now, baseline);
        if (counterMapper.releaseConsumed(tenantId, metricKey, NON_PERIODIC, amount,
                operator(operator), now) != 1) {
            throw conflict("Quota counter cannot release consumed usage");
        }
        SaasQuotaUsage usage = usage(metricKey, counter.getUsedAmount() - amount,
                counter.getReservedAmount(), null);
        enqueueUsage(tenantId, usage, now);
        return usage;
    }

    private SaasQuotaUsage reserve(String tenantId, SaasUsageEvent event,
            LocalDateTime period, LocalDateTime now) {
        SaasRuntimeEntitlements entitlements = snapshotService.current(tenantId);
        if (!entitlements.writeAllowed()) throw validation("租户当前为只读状态，不能预留配额");
        Long limit = entitlements.quotaLimit(event.getMetricKey());
        SysSaasQuotaReservation existing = reservationMapper.findForUpdate(
                tenantId, event.getMetricKey(), event.getReferenceKey());
        SysSaasQuotaCounter counter = counter(tenantId, event.getMetricKey(), period, now);
        if (existing != null) {
            requireSameReservation(existing, event, period);
            if (RESERVED.equals(existing.getStatus()) || SETTLED.equals(existing.getStatus())) {
                return usage(event.getMetricKey(), counter.getUsedAmount(), counter.getReservedAmount(), event.getPeriodStartEpochMs());
            }
            throw conflict("Released quota reservation cannot be reused");
        }
        if (counterMapper.addReservation(tenantId, event.getMetricKey(), period,
                event.getAmount(), limit, OPERATOR, now) != 1) {
            throw validation("租户配额不足");
        }
        SysSaasQuotaReservation reservation = reservation(event, tenantId, period, now);
        if (reservationMapper.insert(reservation) != 1) throw conflict("Quota reservation was not created");
        return usage(event.getMetricKey(), counter.getUsedAmount(),
                counter.getReservedAmount() + event.getAmount(), event.getPeriodStartEpochMs());
    }

    private SaasQuotaUsage settle(String tenantId, SaasUsageEvent event,
            LocalDateTime period, LocalDateTime now) {
        SysSaasQuotaReservation reservation = requiredReservation(tenantId, event);
        requirePeriod(reservation, period);
        SysSaasQuotaCounter counter = counter(tenantId, event.getMetricKey(), period, now);
        if (SETTLED.equals(reservation.getStatus())) {
            if (!Objects.equals(reservation.getSettledAmount(), event.getAmount())) {
                throw conflict("Settled quota amount does not match the retry");
            }
            return usage(event.getMetricKey(), counter.getUsedAmount(), counter.getReservedAmount(), event.getPeriodStartEpochMs());
        }
        if (!RESERVED.equals(reservation.getStatus()) || event.getAmount() > reservation.getReservedAmount()) {
            throw conflict("Quota reservation cannot be settled");
        }
        if (counterMapper.settleReservation(tenantId, event.getMetricKey(), period,
                reservation.getReservedAmount(), event.getAmount(), OPERATOR, now) != 1
                || reservationMapper.markSettled(tenantId, event.getMetricKey(), event.getReferenceKey(),
                        event.getAmount(), event.getIdempotencyKey(), OPERATOR, now) != 1) {
            throw conflict("Quota settlement changed concurrently");
        }
        return usage(event.getMetricKey(), counter.getUsedAmount() + event.getAmount(),
                counter.getReservedAmount() - reservation.getReservedAmount(), event.getPeriodStartEpochMs());
    }

    private SaasQuotaUsage release(String tenantId, SaasUsageEvent event,
            LocalDateTime period, LocalDateTime now) {
        SysSaasQuotaReservation reservation = requiredReservation(tenantId, event);
        requirePeriod(reservation, period);
        SysSaasQuotaCounter counter = counter(tenantId, event.getMetricKey(), period, now);
        if (RELEASED.equals(reservation.getStatus())) {
            return usage(event.getMetricKey(), counter.getUsedAmount(), counter.getReservedAmount(), event.getPeriodStartEpochMs());
        }
        long used = counter.getUsedAmount();
        long reserved = counter.getReservedAmount();
        if (RESERVED.equals(reservation.getStatus())) {
            if (counterMapper.releaseReservation(tenantId, event.getMetricKey(), period,
                    reservation.getReservedAmount(), OPERATOR, now) != 1) {
                throw conflict("Quota reservation changed concurrently");
            }
            reserved -= reservation.getReservedAmount();
        } else if (SETTLED.equals(reservation.getStatus()) && !SaasQuotaKeys.isMonthly(event.getMetricKey())) {
            if (counterMapper.releaseConsumed(tenantId, event.getMetricKey(), period,
                    reservation.getSettledAmount(), OPERATOR, now) != 1) {
                throw conflict("Consumed quota changed concurrently");
            }
            used -= reservation.getSettledAmount();
        } else {
            throw conflict("Settled monthly usage cannot be released");
        }
        if (reservationMapper.markReleased(tenantId, event.getMetricKey(), event.getReferenceKey(),
                event.getIdempotencyKey(), OPERATOR, now) != 1) {
            throw conflict("Quota release changed concurrently");
        }
        return usage(event.getMetricKey(), used, reserved, event.getPeriodStartEpochMs());
    }

    private SysSaasQuotaCounter counter(String tenantId, String metricKey,
            LocalDateTime period, LocalDateTime now) {
        long baseline = SaasQuotaKeys.USER_COUNT.equals(metricKey) ? userMapper.countActiveUsers() : 0L;
        return counter(tenantId, metricKey, period, now, baseline);
    }

    private SysSaasQuotaCounter counter(String tenantId, String metricKey,
            LocalDateTime period, LocalDateTime now, long baseline) {
        counterMapper.ensureCounter(tenantId, metricKey, period, baseline, OPERATOR, now);
        SysSaasQuotaCounter counter = counterMapper.findForUpdate(tenantId, metricKey, period);
        if (counter == null || counter.getUsedAmount() == null || counter.getReservedAmount() == null) {
            throw conflict("Quota counter is unavailable");
        }
        return counter;
    }

    private SysSaasQuotaReservation requiredReservation(String tenantId, SaasUsageEvent event) {
        SysSaasQuotaReservation reservation = reservationMapper.findForUpdate(
                tenantId, event.getMetricKey(), event.getReferenceKey());
        if (reservation == null) throw conflict("Quota reservation does not exist");
        return reservation;
    }

    private void requireSameReservation(SysSaasQuotaReservation reservation,
            SaasUsageEvent event, LocalDateTime period) {
        requirePeriod(reservation, period);
        if (!Objects.equals(reservation.getReservedAmount(), event.getAmount())) {
            throw conflict("Quota reservation amount does not match the retry");
        }
    }

    private void requirePeriod(SysSaasQuotaReservation reservation, LocalDateTime period) {
        if (!Objects.equals(reservation.getPeriodStart(), period)) {
            throw conflict("Quota reservation period does not match");
        }
    }

    private SysSaasQuotaReservation reservation(SaasUsageEvent event, String tenantId,
            LocalDateTime period, LocalDateTime now) {
        SysSaasQuotaReservation reservation = new SysSaasQuotaReservation();
        reservation.setTenantId(tenantId);
        reservation.setMetricKey(event.getMetricKey());
        reservation.setReservationKey(event.getReferenceKey());
        reservation.setPeriodStart(period);
        reservation.setReservedAmount(event.getAmount());
        reservation.setSettledAmount(0L);
        reservation.setStatus(RESERVED);
        reservation.setReserveEventKey(event.getIdempotencyKey());
        reservation.setCreateBy(OPERATOR);
        reservation.setCreateTime(now);
        reservation.setUpdateBy(OPERATOR);
        reservation.setUpdateTime(now);
        reservation.setVersionNo(0L);
        return reservation;
    }

    private LocalDateTime period(SaasUsageEvent event) {
        return event.getPeriodStartEpochMs() == null ? NON_PERIODIC
                : LocalDateTime.ofInstant(Instant.ofEpochMilli(event.getPeriodStartEpochMs()), ZoneOffset.UTC);
    }

    private void enqueueUsage(String tenantId, SaasQuotaUsage usage, LocalDateTime now) {
        SysSaasUsageOutbox row = new SysSaasUsageOutbox();
        row.setTenantId(tenantId);
        row.setEventKey(UUID.randomUUID().toString());
        row.setMetricKey(usage.getQuotaKey());
        row.setAmount(usage.getUsed());
        row.setPeriodStart(usage.getPeriodStartEpochMs() == null ? null
                : LocalDateTime.ofInstant(Instant.ofEpochMilli(usage.getPeriodStartEpochMs()), ZoneOffset.UTC));
        row.setOccurredAt(now);
        row.setStatus("PENDING");
        row.setAttemptCount(0);
        row.setNextAttemptAt(now);
        row.setCreateBy(OPERATOR);
        row.setCreateTime(now);
        row.setUpdateBy(OPERATOR);
        row.setUpdateTime(now);
        if (outboxMapper.insert(row) != 1) {
            throw conflict("SaaS usage report was not queued");
        }
    }

    private String requireTenant(String requestedTenantId) {
        String current = TenantContextHolder.getTenantId();
        if (!StringUtils.hasText(current) || !current.trim().equals(requestedTenantId)) {
            throw validation("Quota tenant does not match the active tenant context");
        }
        return current.trim();
    }

    private static String operator(String value) {
        if (!StringUtils.hasText(value) || value.trim().length() > 64) throw validation("Invalid quota operator");
        return value.trim();
    }

    private static SaasQuotaUsage usage(String metricKey, long used, long reserved, Long period) {
        return new SaasQuotaUsage(metricKey, used, reserved, period);
    }

    private static ServiceException validation(String message) {
        return new ServiceException(message, (int) ResultCode.VALIDATE_FAILED.getCode());
    }

    private static ServiceException conflict(String message) {
        return new ServiceException(message, (int) ResultCode.CONFLICT.getCode());
    }
}

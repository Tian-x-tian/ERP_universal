package com.erp.saas.control.service.usage.impl;

import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.erp.saas.contract.model.SaasQuotaKeys;
import com.erp.saas.contract.model.SaasUsageEvent;
import com.erp.saas.contract.model.SaasUsageEventValidator;
import com.erp.saas.contract.model.SaasUsageOperation;
import com.erp.saas.control.domain.entity.SaasUsageEventEntity;
import com.erp.saas.control.domain.entity.SaasUsageSummaryEntity;
import com.erp.saas.control.mapper.SaasTenantMapper;
import com.erp.saas.control.mapper.SaasUsageEventMapper;
import com.erp.saas.control.mapper.SaasUsageSummaryMapper;
import com.erp.saas.control.service.ControlUtcTime;
import com.erp.saas.control.service.usage.SaasUsageAggregationService;
import com.erp.saas.control.service.usage.SaasUsageException;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Objects;

@Service
public class SaasUsageAggregationServiceImpl implements SaasUsageAggregationService {
    private static final LocalDateTime NON_PERIODIC = LocalDateTime.of(1970, 1, 1, 0, 0);

    private final SaasTenantMapper tenantMapper;
    private final SaasUsageEventMapper eventMapper;
    private final SaasUsageSummaryMapper summaryMapper;
    private final ControlUtcTime time;

    public SaasUsageAggregationServiceImpl(SaasTenantMapper tenantMapper,
            SaasUsageEventMapper eventMapper, SaasUsageSummaryMapper summaryMapper,
            ControlUtcTime time) {
        this.tenantMapper = Objects.requireNonNull(tenantMapper);
        this.eventMapper = Objects.requireNonNull(eventMapper);
        this.summaryMapper = Objects.requireNonNull(summaryMapper);
        this.time = Objects.requireNonNull(time);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void report(SaasUsageEvent event, String operator) {
        validate(event);
        String tenantId = event.getTenantId().trim();
        String actor = operator(operator);
        if (tenantMapper.findByTenantId(tenantId) == null) {
            throw new SaasUsageException(SaasUsageException.ErrorCode.TENANT_NOT_FOUND,
                    "Usage tenant not found");
        }
        LocalDateTime periodStart = periodStart(event);
        LocalDateTime occurredAt = LocalDateTime.ofInstant(
                Instant.ofEpochMilli(event.getOccurredAtEpochMs()), ZoneOffset.UTC);
        LocalDateTime now = time.now();
        SaasUsageEventEntity row = eventRow(event, tenantId, periodStart, occurredAt, actor, now);
        try {
            if (eventMapper.insert(row) != 1) {
                throw persistence("Usage event was not stored", null);
            }
        } catch (DuplicateKeyException duplicate) {
            SaasUsageEventEntity existing = eventMapper.findByIdempotencyKey(event.getIdempotencyKey());
            if (samePayload(existing, row)) {
                return;
            }
            throw new SaasUsageException(SaasUsageException.ErrorCode.IDEMPOTENCY_CONFLICT,
                    "Usage idempotency key was reused with a different payload", duplicate);
        }
        SaasUsageSummaryEntity summary = summaryRow(row, actor, now);
        summaryMapper.upsertLatest(summary);
    }

    private static void validate(SaasUsageEvent event) {
        try {
            SaasUsageEventValidator.validate(event);
        } catch (IllegalArgumentException exception) {
            throw new SaasUsageException(SaasUsageException.ErrorCode.INVALID_INPUT,
                    exception.getMessage(), exception);
        }
        if (event.getOperation() != SaasUsageOperation.REPORT) {
            throw new SaasUsageException(SaasUsageException.ErrorCode.INVALID_INPUT,
                    "Central usage aggregation accepts REPORT events only");
        }
    }

    private static String operator(String value) {
        if (value == null || value.trim().isEmpty() || value.trim().length() > 64) {
            throw new SaasUsageException(SaasUsageException.ErrorCode.INVALID_INPUT,
                    "operator must contain 1 to 64 characters");
        }
        return value.trim();
    }

    private static LocalDateTime periodStart(SaasUsageEvent event) {
        if (!SaasQuotaKeys.isMonthly(event.getMetricKey())) {
            return NON_PERIODIC;
        }
        return LocalDateTime.ofInstant(Instant.ofEpochMilli(event.getPeriodStartEpochMs()), ZoneOffset.UTC);
    }

    private static SaasUsageEventEntity eventRow(SaasUsageEvent event, String tenantId,
            LocalDateTime periodStart, LocalDateTime occurredAt, String actor, LocalDateTime now) {
        SaasUsageEventEntity row = new SaasUsageEventEntity();
        row.setIdempotencyKey(event.getIdempotencyKey());
        row.setTenantId(tenantId);
        row.setMetricKey(event.getMetricKey());
        row.setOperation(event.getOperation());
        row.setAmount(event.getAmount());
        row.setPeriodStart(periodStart);
        row.setOccurredAt(occurredAt);
        row.setCreateBy(actor);
        row.setCreateTime(now);
        row.setUpdateBy(actor);
        row.setUpdateTime(now);
        return row;
    }

    private static SaasUsageSummaryEntity summaryRow(SaasUsageEventEntity event,
            String actor, LocalDateTime now) {
        SaasUsageSummaryEntity row = new SaasUsageSummaryEntity();
        row.setUsageSummaryId(IdWorker.getId());
        row.setTenantId(event.getTenantId());
        row.setMetricKey(event.getMetricKey());
        row.setPeriodStart(event.getPeriodStart());
        row.setUsedAmount(event.getAmount());
        row.setLastEventKey(event.getIdempotencyKey());
        row.setLastOccurredAt(event.getOccurredAt());
        row.setCreateBy(actor);
        row.setCreateTime(now);
        row.setUpdateBy(actor);
        row.setUpdateTime(now);
        row.setVersionNo(0L);
        return row;
    }

    private static boolean samePayload(SaasUsageEventEntity left, SaasUsageEventEntity right) {
        return left != null
                && Objects.equals(left.getIdempotencyKey(), right.getIdempotencyKey())
                && Objects.equals(left.getTenantId(), right.getTenantId())
                && Objects.equals(left.getMetricKey(), right.getMetricKey())
                && left.getOperation() == right.getOperation()
                && Objects.equals(left.getAmount(), right.getAmount())
                && Objects.equals(left.getPeriodStart(), right.getPeriodStart())
                && Objects.equals(left.getOccurredAt(), right.getOccurredAt());
    }

    private static SaasUsageException persistence(String message, Throwable cause) {
        return cause == null
                ? new SaasUsageException(SaasUsageException.ErrorCode.PERSISTENCE_CONFLICT, message)
                : new SaasUsageException(SaasUsageException.ErrorCode.PERSISTENCE_CONFLICT, message, cause);
    }
}

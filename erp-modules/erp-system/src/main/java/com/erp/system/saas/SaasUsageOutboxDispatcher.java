package com.erp.system.saas;

import com.erp.common.client.internal.InternalSaasClient;
import com.erp.common.core.context.TenantContextHolder;
import com.erp.saas.contract.model.SaasQuotaKeys;
import com.erp.saas.contract.model.SaasUsageEvent;
import com.erp.saas.contract.model.SaasUsageOperation;
import com.erp.system.domain.SysSaasUsageOutbox;
import com.erp.system.mapper.SysSaasUsageOutboxMapper;
import com.erp.system.mapper.SysTenantMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Objects;

@Component
@ConditionalOnProperty(prefix = "erp.saas.usage-report", name = "enabled",
        havingValue = "true", matchIfMissing = true)
public class SaasUsageOutboxDispatcher {
    private static final Logger log = LoggerFactory.getLogger(SaasUsageOutboxDispatcher.class);
    private static final String PLATFORM_TENANT_ID = "000000";

    private final SysTenantMapper tenantMapper;
    private final SysSaasUsageOutboxMapper outboxMapper;
    private final InternalSaasClient saasClient;
    private final Clock clock;

    @Value("${erp.saas.usage-report.batch-size:100}")
    private int batchSize = 100;

    public SaasUsageOutboxDispatcher(SysTenantMapper tenantMapper,
            SysSaasUsageOutboxMapper outboxMapper, InternalSaasClient saasClient, Clock clock) {
        this.tenantMapper = Objects.requireNonNull(tenantMapper);
        this.outboxMapper = Objects.requireNonNull(outboxMapper);
        this.saasClient = Objects.requireNonNull(saasClient);
        this.clock = Objects.requireNonNull(clock);
    }

    @Scheduled(fixedDelayString = "${erp.saas.usage-report.interval-ms:60000}",
            initialDelayString = "${erp.saas.usage-report.initial-delay-ms:45000}")
    public void dispatchAll() {
        LocalDateTime now = LocalDateTime.ofInstant(clock.instant(), ZoneOffset.UTC);
        TenantContextHolder.clear();
        for (String tenantId : tenantMapper.findActiveTenantIds()) {
            if (tenantId == null || tenantId.isBlank() || PLATFORM_TENANT_ID.equals(tenantId)) {
                continue;
            }
            try {
                TenantContextHolder.setTenantId(tenantId);
                dispatchTenant(tenantId, now);
            } catch (RuntimeException exception) {
                log.warn("SaaS usage dispatch skipped tenant: tenantId={}, errorType={}",
                        tenantId, exception.getClass().getSimpleName());
            } finally {
                TenantContextHolder.clear();
            }
        }
    }

    private void dispatchTenant(String tenantId, LocalDateTime now) {
        int limit = Math.max(1, Math.min(batchSize, 1000));
        List<SysSaasUsageOutbox> rows = outboxMapper.findPending(tenantId, now, limit);
        for (SysSaasUsageOutbox row : rows) {
            try {
                saasClient.reportUsage(toReport(row));
                outboxMapper.markSent(tenantId, row.getOutboxId(), now);
            } catch (RuntimeException exception) {
                int currentAttempt = Math.max(0,
                        row.getAttemptCount() == null ? 0 : row.getAttemptCount());
                int attempt = currentAttempt == Integer.MAX_VALUE
                        ? Integer.MAX_VALUE : currentAttempt + 1;
                String errorType = exception.getClass().getSimpleName();
                try {
                    outboxMapper.markRetry(tenantId, row.getOutboxId(), attempt,
                            now.plusMinutes(1), errorType, now);
                } catch (RuntimeException retryFailure) {
                    exception.addSuppressed(retryFailure);
                }
                log.warn("SaaS usage report failed: tenantId={}, outboxId={}, errorType={}",
                        tenantId, row.getOutboxId(), errorType);
            }
        }
    }

    private SaasUsageEvent toReport(SysSaasUsageOutbox row) {
        if (row == null || row.getOccurredAt() == null) {
            throw new IllegalStateException("SaaS usage outbox row is incomplete");
        }
        Long period = null;
        if (SaasQuotaKeys.isMonthly(row.getMetricKey())) {
            if (row.getPeriodStart() == null) {
                throw new IllegalStateException("Monthly SaaS usage outbox row has no period");
            }
            period = row.getPeriodStart().toInstant(ZoneOffset.UTC).toEpochMilli();
        }
        return new SaasUsageEvent(row.getEventKey(), row.getTenantId(), row.getMetricKey(),
                SaasUsageOperation.REPORT, null, row.getAmount(), period,
                row.getOccurredAt().toInstant(ZoneOffset.UTC).toEpochMilli());
    }
}

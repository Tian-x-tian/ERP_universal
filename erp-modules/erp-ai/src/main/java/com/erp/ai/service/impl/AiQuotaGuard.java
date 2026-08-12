package com.erp.ai.service.impl;

import com.erp.ai.config.ErpAiProperties;
import com.erp.common.client.internal.InternalSystemClient;
import com.erp.common.core.context.TenantContextHolder;
import com.erp.saas.contract.model.SaasQuotaKeys;
import com.erp.saas.contract.model.SaasUsageEvent;
import com.erp.saas.contract.model.SaasUsageOperation;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

/**
 * 在每次模型调用前后原子预留、结算或释放租户 AI Token 配额。
 */
@Service
public class AiQuotaGuard {
    private final InternalSystemClient systemClient;
    private final ErpAiProperties properties;
    private final Clock clock;

    public AiQuotaGuard(InternalSystemClient systemClient, ErpAiProperties properties, Clock clock) {
        this.systemClient = systemClient;
        this.properties = properties;
        this.clock = clock;
    }

    public AiQuotaReservation reserve(long inputTokenUpperBound) {
        String tenantId = TenantContextHolder.getTenantId();
        if (!StringUtils.hasText(tenantId)) {
            throw new IllegalStateException("AI quota requires an active tenant context");
        }
        long maxInput = positiveLimit(properties.getMaxInputTokens(), "maxInputTokens");
        long maxOutput = positiveLimit(properties.getMaxOutputTokens(), "maxOutputTokens");
        if (inputTokenUpperBound <= 0 || inputTokenUpperBound > maxInput) {
            throw new IllegalArgumentException("AI input exceeds the configured token limit");
        }
        Instant now = clock.instant();
        long period = LocalDate.ofInstant(now, ZoneOffset.UTC).withDayOfMonth(1)
                .atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli();
        String callReference = "ai-" + UUID.randomUUID();
        AiQuotaReservation reservation = new AiQuotaReservation(tenantId.trim(), period,
                callReference + "-input", callReference + "-output", inputTokenUpperBound, maxOutput);
        systemClient.applyQuotaEvents(List.of(
                event(reservation, SaasQuotaKeys.AI_INPUT_TOKENS, SaasUsageOperation.RESERVE,
                        reservation.inputReference(), inputTokenUpperBound, now),
                event(reservation, SaasQuotaKeys.AI_OUTPUT_TOKENS, SaasUsageOperation.RESERVE,
                        reservation.outputReference(), maxOutput, now)));
        return reservation;
    }

    public void settle(AiQuotaReservation reservation, Long inputTokens, Long outputTokens, boolean usageReported) {
        requireReservation(reservation);
        long settledInput = usageReported && inputTokens != null ? inputTokens : reservation.reservedInputTokens();
        long settledOutput = usageReported && outputTokens != null ? outputTokens : reservation.reservedOutputTokens();
        validateSettled(settledInput, reservation.reservedInputTokens(), "inputTokens");
        validateSettled(settledOutput, reservation.reservedOutputTokens(), "outputTokens");
        Instant now = clock.instant();
        systemClient.applyQuotaEvents(List.of(
                event(reservation, SaasQuotaKeys.AI_INPUT_TOKENS, SaasUsageOperation.SETTLE,
                        reservation.inputReference(), settledInput, now),
                event(reservation, SaasQuotaKeys.AI_OUTPUT_TOKENS, SaasUsageOperation.SETTLE,
                        reservation.outputReference(), settledOutput, now)));
    }

    public void release(AiQuotaReservation reservation) {
        requireReservation(reservation);
        Instant now = clock.instant();
        systemClient.applyQuotaEvents(List.of(
                event(reservation, SaasQuotaKeys.AI_INPUT_TOKENS, SaasUsageOperation.RELEASE,
                        reservation.inputReference(), null, now),
                event(reservation, SaasQuotaKeys.AI_OUTPUT_TOKENS, SaasUsageOperation.RELEASE,
                        reservation.outputReference(), null, now)));
    }

    private SaasUsageEvent event(AiQuotaReservation reservation, String metric,
            SaasUsageOperation operation, String reference, Long amount, Instant now) {
        return new SaasUsageEvent(operation.name().toLowerCase() + "-" + UUID.randomUUID(),
                reservation.tenantId(), metric, operation, reference, amount,
                reservation.periodStartEpochMs(), now.toEpochMilli());
    }

    private static long positiveLimit(int value, String field) {
        if (value <= 0) {
            throw new IllegalStateException(field + " must be positive");
        }
        return value;
    }

    private static void validateSettled(long actual, long reserved, String field) {
        if (actual < 0 || actual > reserved) {
            throw new IllegalArgumentException(field + " exceeds the reserved amount");
        }
    }

    private static void requireReservation(AiQuotaReservation reservation) {
        if (reservation == null) {
            throw new IllegalArgumentException("reservation must not be null");
        }
    }
}

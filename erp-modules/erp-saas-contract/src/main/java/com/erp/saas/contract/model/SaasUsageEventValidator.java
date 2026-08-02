package com.erp.saas.contract.model;

import java.time.Instant;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;

/**
 * Stateless validation for SaaS usage event boundaries.
 */
public final class SaasUsageEventValidator {
    private SaasUsageEventValidator() {
    }

    public static void validate(SaasUsageEvent event) {
        if (event == null) {
            throw new IllegalArgumentException("event must not be null");
        }
        requireText(event.getIdempotencyKey(), "idempotencyKey", 128);
        requireText(event.getTenantId(), "tenantId", 20);
        requireText(event.getMetricKey(), "metricKey", 64);
        if (!SaasQuotaKeys.isKnown(event.getMetricKey())) {
            throw new IllegalArgumentException("unknown metricKey");
        }
        if (event.getOperation() == null) {
            throw new IllegalArgumentException("operation must not be null");
        }
        if (event.getOccurredAtEpochMs() <= 0) {
            throw new IllegalArgumentException("occurredAtEpochMs must be positive");
        }
        validatePeriod(event.getMetricKey(), event.getPeriodStartEpochMs());
        switch (event.getOperation()) {
            case RESERVE -> {
                requireText(event.getReferenceKey(), "referenceKey", 128);
                requireAmount(event.getAmount(), true);
            }
            case SETTLE -> {
                requireText(event.getReferenceKey(), "referenceKey", 128);
                requireAmount(event.getAmount(), false);
            }
            case RELEASE -> {
                requireText(event.getReferenceKey(), "referenceKey", 128);
                if (event.getAmount() != null) {
                    throw new IllegalArgumentException("RELEASE amount must be null");
                }
            }
            case REPORT -> {
                if (event.getReferenceKey() != null) {
                    throw new IllegalArgumentException("REPORT referenceKey must be null");
                }
                requireAmount(event.getAmount(), false);
            }
        }
    }

    private static void validatePeriod(String metricKey, Long periodStartEpochMs) {
        if (!SaasQuotaKeys.isMonthly(metricKey)) {
            if (periodStartEpochMs != null) {
                throw new IllegalArgumentException("non-periodic metric must not have periodStartEpochMs");
            }
            return;
        }
        if (periodStartEpochMs == null) {
            throw new IllegalArgumentException("monthly metric requires periodStartEpochMs");
        }
        ZonedDateTime period = Instant.ofEpochMilli(periodStartEpochMs).atZone(ZoneOffset.UTC);
        if (period.getDayOfMonth() != 1 || period.getHour() != 0 || period.getMinute() != 0
                || period.getSecond() != 0 || period.getNano() != 0) {
            throw new IllegalArgumentException("periodStartEpochMs must be UTC month start");
        }
    }

    private static void requireAmount(Long amount, boolean positive) {
        if (amount == null || (positive ? amount <= 0 : amount < 0)) {
            throw new IllegalArgumentException(positive ? "amount must be positive" : "amount must be non-negative");
        }
    }

    private static void requireText(String value, String field, int maxLength) {
        if (!hasText(value)) {
            throw new IllegalArgumentException(field + " must not be blank");
        }
        if (value.length() > maxLength) {
            throw new IllegalArgumentException(field + " exceeds " + maxLength + " characters");
        }
    }

    private static boolean hasText(String value) {
        return value != null && !value.trim().isEmpty();
    }
}

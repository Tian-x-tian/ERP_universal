package com.erp.saas.control.service;

import com.erp.saas.contract.model.SaasQuotaKeys;
import com.erp.saas.control.domain.QuotaPeriodType;

import java.time.LocalDateTime;
import java.util.regex.Pattern;

public final class SaasCatalogValidation {
    private static final Pattern FEATURE_KEY = Pattern.compile("[a-z][a-z0-9_.-]{1,127}");
    private static final Pattern PLAN_OR_QUOTA_KEY = Pattern.compile("[a-z][a-z0-9_.-]{1,63}");
    private static final Pattern TENANT_ID = Pattern.compile("[A-Za-z0-9_-]{1,20}");

    private SaasCatalogValidation() {
    }

    public static String featureKey(String value) {
        return matchingKey(value, "featureKey", FEATURE_KEY);
    }

    public static String planCode(String value) {
        return matchingKey(value, "planCode", PLAN_OR_QUOTA_KEY);
    }

    public static String knownQuotaKey(String value) {
        String normalized = matchingKey(value, "quotaKey", PLAN_OR_QUOTA_KEY);
        if (!SaasQuotaKeys.isKnown(normalized)) {
            throw new SaasCatalogException(SaasCatalogException.ErrorCode.UNKNOWN_QUOTA_KEY,
                    "quotaKey is unknown: " + normalized);
        }
        return normalized;
    }

    public static String tenantId(String value) {
        return matchingKey(value, "tenantId", TENANT_ID);
    }

    public static String name(String value, String field) {
        String normalized = requiredText(value, field);
        if (normalized.length() > 128) {
            throw invalid(field + " must not exceed 128 characters");
        }
        return normalized;
    }

    public static String optionalDescription(String value, String field) {
        if (value == null) {
            return null;
        }
        String normalized = value.trim();
        if (normalized.length() > 512) {
            throw invalid(field + " must not exceed 512 characters");
        }
        return normalized;
    }

    public static <T> T required(T value, String field) {
        if (value == null) {
            throw invalid(field + " must not be null");
        }
        return value;
    }

    public static int range(Integer value, String field, int minimum, int maximum) {
        required(value, field);
        if (value < minimum || value > maximum) {
            throw invalid(field + " must be between " + minimum + " and " + maximum);
        }
        return value;
    }

    public static Long nonNegative(Long value, String field) {
        if (value != null && value < 0) {
            throw invalid(field + " must not be negative");
        }
        return value;
    }

    public static void quotaPeriod(String quotaKey, QuotaPeriodType periodType) {
        required(periodType, "periodType");
        QuotaPeriodType expected = SaasQuotaKeys.isMonthly(quotaKey)
                ? QuotaPeriodType.MONTHLY : QuotaPeriodType.CURRENT;
        if (periodType != expected) {
            throw invalid("periodType must be " + expected + " for quotaKey " + quotaKey);
        }
    }

    public static void window(LocalDateTime effectiveFrom, LocalDateTime effectiveUntil) {
        required(effectiveFrom, "effectiveFrom");
        if (effectiveUntil != null && !effectiveUntil.isAfter(effectiveFrom)) {
            throw invalid("effectiveUntil must be after effectiveFrom");
        }
    }

    public static SaasCatalogException invalid(String message) {
        return new SaasCatalogException(SaasCatalogException.ErrorCode.INVALID_INPUT, message);
    }

    private static String requiredText(String value, String field) {
        if (value == null) {
            throw invalid(field + " must not be null");
        }
        String normalized = value.trim();
        if (normalized.isEmpty()) {
            throw invalid(field + " must not be blank");
        }
        return normalized;
    }

    private static String matchingKey(String value, String field, Pattern pattern) {
        String normalized = requiredText(value, field);
        if (!pattern.matcher(normalized).matches()) {
            throw invalid(field + " has an invalid format");
        }
        return normalized;
    }
}

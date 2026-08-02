package com.erp.saas.control.service.lifecycle;

import java.time.LocalDateTime;
import java.util.regex.Pattern;

public final class SaasLifecycleValidation {
    private static final Pattern TENANT_ID = Pattern.compile("[A-Za-z0-9_-]{1,20}");

    private SaasLifecycleValidation() { }

    public static String tenantId(String value) {
        if (value == null || !TENANT_ID.matcher(value.trim()).matches()) {
            throw invalid("tenantId has an invalid format");
        }
        return value.trim();
    }

    public static String operator(String value) {
        if (value == null || value.trim().isEmpty() || value.trim().length() > 64) {
            throw invalid("operator must contain 1 to 64 characters");
        }
        return value.trim();
    }

    public static Long id(Long value, String field) {
        if (value == null || value <= 0) {
            throw invalid(field + " must be positive");
        }
        return value;
    }

    public static Long version(Long value) {
        if (value == null || value < 0) {
            throw invalid("expectedTenantVersion must not be negative");
        }
        return value;
    }

    public static LocalDateTime endAt(LocalDateTime value, boolean nonExpiring) {
        if (nonExpiring && value != null) {
            throw invalid("endAt must be null for a non-expiring subscription");
        }
        if (!nonExpiring && value == null) {
            throw invalid("endAt is required for a finite subscription");
        }
        return value;
    }

    public static SaasLifecycleException invalid(String message) {
        return new SaasLifecycleException(SaasLifecycleException.ErrorCode.INVALID_INPUT, message);
    }
}

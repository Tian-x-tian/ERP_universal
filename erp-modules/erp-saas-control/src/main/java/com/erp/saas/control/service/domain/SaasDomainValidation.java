package com.erp.saas.control.service.domain;

import com.erp.saas.control.domain.DomainVerificationMethod;

import java.util.regex.Pattern;

public final class SaasDomainValidation {
    private static final Pattern TENANT_ID = Pattern.compile("[A-Za-z0-9_-]{1,20}");
    private SaasDomainValidation() { }

    public static String tenantId(String value) {
        if (value == null || !TENANT_ID.matcher(value.trim()).matches()) {
            throw invalid("tenantId has an invalid format");
        }
        return value.trim();
    }

    public static String host(String value) {
        if (value == null || value.trim().isEmpty() || value.trim().length() > 512) {
            throw new SaasDomainException(SaasDomainException.ErrorCode.INVALID_HOST, "Invalid domain host");
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
            throw invalid("expectedVersion must not be negative");
        }
        return value;
    }

    public static DomainVerificationMethod verificationMethod(DomainVerificationMethod value) {
        if (value == null) {
            throw invalid("verificationMethod must not be null");
        }
        return value;
    }

    public static <T> T required(T value, String field) {
        if (value == null) {
            throw invalid(field + " must not be null");
        }
        return value;
    }

    private static SaasDomainException invalid(String message) {
        return new SaasDomainException(SaasDomainException.ErrorCode.INVALID_INPUT, message);
    }
}

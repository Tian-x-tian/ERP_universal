package com.erp.saas.control.service;

import java.util.Objects;

public class SaasCatalogException extends RuntimeException {
    private static final long serialVersionUID = 1L;

    public enum ErrorCode {
        NOT_FOUND,
        DUPLICATE,
        INVALID_INPUT,
        IMMUTABLE_PUBLISHED_PLAN,
        VERSION_CONFLICT,
        OVERLAPPING_OVERRIDE,
        UNKNOWN_FEATURE_KEY,
        UNKNOWN_QUOTA_KEY
    }

    private final ErrorCode errorCode;

    public SaasCatalogException(ErrorCode errorCode, String message) {
        super(message);
        this.errorCode = Objects.requireNonNull(errorCode, "errorCode must not be null");
    }

    public SaasCatalogException(ErrorCode errorCode, String message, Throwable cause) {
        super(message, cause);
        this.errorCode = Objects.requireNonNull(errorCode, "errorCode must not be null");
    }

    public ErrorCode getErrorCode() {
        return errorCode;
    }
}

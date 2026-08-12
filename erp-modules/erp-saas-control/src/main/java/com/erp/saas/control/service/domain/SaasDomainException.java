package com.erp.saas.control.service.domain;

import java.util.Objects;

public class SaasDomainException extends RuntimeException {
    public enum ErrorCode {
        INVALID_INPUT,
        INVALID_HOST,
        NOT_FOUND,
        OWNERSHIP_CONFLICT,
        INVALID_STATE,
        VERSION_CONFLICT,
        TENANT_NOT_FOUND,
        TENANT_NOT_ELIGIBLE
    }

    private final ErrorCode errorCode;

    public SaasDomainException(ErrorCode errorCode, String message) {
        super(message);
        this.errorCode = Objects.requireNonNull(errorCode, "errorCode must not be null");
    }

    public SaasDomainException(ErrorCode errorCode, String message, Throwable cause) {
        super(message, cause);
        this.errorCode = Objects.requireNonNull(errorCode, "errorCode must not be null");
    }

    public ErrorCode getErrorCode() {
        return errorCode;
    }
}

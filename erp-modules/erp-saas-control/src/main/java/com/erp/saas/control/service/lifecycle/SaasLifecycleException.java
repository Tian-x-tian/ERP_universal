package com.erp.saas.control.service.lifecycle;

import java.util.Objects;

public class SaasLifecycleException extends RuntimeException {
    public enum ErrorCode {
        INVALID_INPUT,
        NOT_FOUND,
        PLAN_NOT_ACTIVE,
        INVALID_TRANSITION,
        VERSION_CONFLICT,
        CURRENT_SUBSCRIPTION_CONFLICT
    }

    private final ErrorCode errorCode;

    public SaasLifecycleException(ErrorCode errorCode, String message) {
        super(message);
        this.errorCode = Objects.requireNonNull(errorCode, "errorCode must not be null");
    }

    public SaasLifecycleException(ErrorCode errorCode, String message, Throwable cause) {
        super(message, cause);
        this.errorCode = Objects.requireNonNull(errorCode, "errorCode must not be null");
    }

    public ErrorCode getErrorCode() {
        return errorCode;
    }
}

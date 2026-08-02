package com.erp.saas.control.service.usage;

public class SaasUsageException extends RuntimeException {
    public enum ErrorCode {
        INVALID_INPUT,
        TENANT_NOT_FOUND,
        IDEMPOTENCY_CONFLICT,
        PERSISTENCE_CONFLICT
    }

    private final ErrorCode errorCode;

    public SaasUsageException(ErrorCode errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }

    public SaasUsageException(ErrorCode errorCode, String message, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
    }

    public ErrorCode getErrorCode() {
        return errorCode;
    }
}

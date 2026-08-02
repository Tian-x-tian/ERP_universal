package com.erp.saas.control.service.snapshot;

import java.util.Objects;

public class SaasSnapshotException extends RuntimeException {
    public enum ErrorCode {
        INVALID_INPUT,
        TENANT_NOT_FOUND,
        DEPLOYMENT_NOT_ELIGIBLE,
        SNAPSHOT_CORRUPTED,
        VERSION_CONFLICT
    }

    private final ErrorCode errorCode;

    public SaasSnapshotException(ErrorCode errorCode, String message) {
        super(message);
        this.errorCode = Objects.requireNonNull(errorCode, "errorCode must not be null");
    }

    public SaasSnapshotException(ErrorCode errorCode, String message, Throwable cause) {
        super(message, cause);
        this.errorCode = Objects.requireNonNull(errorCode, "errorCode must not be null");
    }

    public ErrorCode getErrorCode() {
        return errorCode;
    }
}

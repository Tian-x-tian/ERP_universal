package com.erp.system.saas;

import java.util.Objects;

public class SaasRuntimeSnapshotException extends RuntimeException {
    public enum ErrorCode {
        INVALID_INPUT,
        TENANT_CONTEXT_MISMATCH,
        INVALID_SIGNATURE,
        INVALID_LEASE,
        SNAPSHOT_CORRUPTED,
        VERSION_ROLLBACK,
        VERSION_COLLISION,
        VERSION_CONFLICT
    }

    private final ErrorCode errorCode;

    public SaasRuntimeSnapshotException(ErrorCode errorCode, String message) {
        super(message);
        this.errorCode = Objects.requireNonNull(errorCode, "errorCode must not be null");
    }

    public SaasRuntimeSnapshotException(ErrorCode errorCode, String message, Throwable cause) {
        super(message, cause);
        this.errorCode = Objects.requireNonNull(errorCode, "errorCode must not be null");
    }

    public ErrorCode getErrorCode() { return errorCode; }
}

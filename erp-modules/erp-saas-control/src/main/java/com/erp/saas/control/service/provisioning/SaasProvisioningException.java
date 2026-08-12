package com.erp.saas.control.service.provisioning;

public class SaasProvisioningException extends RuntimeException {
    public enum ErrorCode {
        NOT_FOUND,
        CONFLICT,
        VERSION_CONFLICT,
        PLAN_NOT_ACTIVE,
        INVALID_RESULT
    }

    private final ErrorCode errorCode;

    public SaasProvisioningException(ErrorCode errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }

    public SaasProvisioningException(ErrorCode errorCode, String message, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
    }

    public ErrorCode getErrorCode() {
        return errorCode;
    }
}

package com.erp.common.core.domain;

import lombok.Getter;

@Getter
public enum ResultCode {
    SUCCESS(0, "OK"),
    PARAM_ERROR(40001, "参数错误"),
    UNAUTHORIZED(40101, "未登录或Token无效"),
    FORBIDDEN(40301, "无权限"),
    NOT_FOUND(40401, "资源不存在"),
    CONFLICT(40901, "状态冲突"),
    VALIDATE_FAILED(42201, "业务校验失败"),
    ERROR(50001, "系统异常");

    private final long code;
    private final String message;

    ResultCode(long code, String message) {
        this.code = code;
        this.message = message;
    }
}

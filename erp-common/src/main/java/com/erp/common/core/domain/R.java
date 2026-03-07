package com.erp.common.core.domain;

import lombok.Data;
import java.io.Serializable;

@Data
public class R<T> implements Serializable {
    private long code;
    private String message;
    private T data;

    protected R() {
    }

    protected R(long code, String message, T data) {
        this.code = code;
        this.message = message;
        this.data = data;
    }

    public static <T> R<T> success(T data) {
        return new R<>(ResultCode.SUCCESS.getCode(), ResultCode.SUCCESS.getMessage(), data);
    }

    public static <T> R<T> success() {
        return success(null);
    }

    public static <T> R<T> failed(ResultCode resultCode) {
        return new R<>(resultCode.getCode(), resultCode.getMessage(), null);
    }

    public static <T> R<T> failed(String message) {
        return new R<>(ResultCode.ERROR.getCode(), message, null);
    }

    public static <T> R<T> failed() {
        return failed(ResultCode.ERROR);
    }
}

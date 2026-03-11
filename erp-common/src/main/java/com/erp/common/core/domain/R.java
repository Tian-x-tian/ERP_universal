package com.erp.common.core.domain;

import com.erp.common.core.context.RequestTraceContextHolder;
import lombok.Data;

import java.io.Serializable;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Data
public class R<T> implements Serializable {
    private static final DateTimeFormatter ISO_OFFSET_FORMATTER = DateTimeFormatter.ISO_OFFSET_DATE_TIME;

    private long code;
    private String message;
    private T data;
    private String timestamp;
    private String traceId;
    private String path;

    protected R() {
    }

    /**
     * 构建统一响应对象。
     *
     * @param code    业务码
     * @param message 响应消息
     * @param data    响应数据
     */
    protected R(long code, String message, T data) {
        this.code = code;
        this.message = message;
        this.data = data;
        this.timestamp = ISO_OFFSET_FORMATTER.format(OffsetDateTime.now(ZoneId.systemDefault()));
        this.traceId = RequestTraceContextHolder.getTraceId();
        this.path = RequestTraceContextHolder.getPath();
    }

    /**
     * 构建成功响应。
     *
     * @param data 业务数据
     * @param <T>  数据类型
     * @return 统一响应
     */
    public static <T> R<T> success(T data) {
        return new R<>(ResultCode.SUCCESS.getCode(), ResultCode.SUCCESS.getMessage(), data);
    }

    /**
     * 构建无数据成功响应。
     *
     * @param <T> 数据类型
     * @return 统一响应
     */
    public static <T> R<T> success() {
        return success(null);
    }

    /**
     * 构建分页成功响应。
     *
     * @param items    列表数据
     * @param pageNum  当前页
     * @param pageSize 每页条数
     * @param total    总记录数
     * @param <T>      列表元素类型
     * @return 统一响应
     */
    public static <T> R<PageData<T>> page(List<T> items, long pageNum, long pageSize, long total) {
        return success(PageData.of(items, pageNum, pageSize, total));
    }

    /**
     * 构建失败响应。
     *
     * @param resultCode 业务码枚举
     * @param <T>        数据类型
     * @return 统一响应
     */
    public static <T> R<T> failed(ResultCode resultCode) {
        return new R<>(resultCode.getCode(), resultCode.getMessage(), null);
    }

    /**
     * 构建失败响应并覆盖默认提示语。
     *
     * @param resultCode 业务码枚举
     * @param message    错误提示
     * @param <T>        数据类型
     * @return 统一响应
     */
    public static <T> R<T> failed(ResultCode resultCode, String message) {
        return new R<>(resultCode.getCode(), message, null);
    }

    /**
     * 构建默认参数错误响应。
     *
     * @param message 错误提示
     * @param <T>     数据类型
     * @return 统一响应
     */
    public static <T> R<T> failed(String message) {
        return failed(ResultCode.PARAM_ERROR, message);
    }

    /**
     * 构建默认系统异常响应。
     *
     * @param <T> 数据类型
     * @return 统一响应
     */
    public static <T> R<T> failed() {
        return failed(ResultCode.ERROR);
    }

    /**
     * 根据业务码动态计算成功标识。
     *
     * @return true 表示业务成功
     */
    public boolean isSuccess() {
        return this.code == ResultCode.SUCCESS.getCode();
    }
}

package com.erp.common.web.error;

import com.erp.common.core.context.RequestTraceContextHolder;
import com.erp.common.core.domain.R;
import com.erp.common.core.domain.ResultCode;
import com.erp.common.web.ApiHttpStatusResolver;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;

import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

/**
 * 统一错误响应构造工厂。
 * 负责构建错误响应体并补齐 traceId、path、timestamp 等公共字段。
 */
public final class ApiErrorResponseFactory {
    /**
     * traceId 请求头名称。
     */
    public static final String TRACE_ID_HEADER = "X-Trace-Id";

    private static final DateTimeFormatter ISO_OFFSET_FORMATTER = DateTimeFormatter.ISO_OFFSET_DATE_TIME;
    private static final int TRACE_ID_LENGTH = 16;

    private ApiErrorResponseFactory() {
    }

    /**
     * 构建基于业务码枚举的响应实体。
     *
     * @param resultCode 业务码
     * @param message    错误消息
     * @return 统一错误响应
     */
    public static ResponseEntity<R<Void>> buildResponseEntity(ResultCode resultCode, String message) {
        return buildResponseEntity(resultCode.getCode(), message);
    }

    /**
     * 构建基于自定义业务码的响应实体。
     *
     * @param code    业务码
     * @param message 错误消息
     * @return 统一错误响应
     */
    public static ResponseEntity<R<Void>> buildResponseEntity(long code, String message) {
        R<Void> body = buildErrorBody(code, message, RequestTraceContextHolder.getTraceId(), RequestTraceContextHolder.getPath());
        return ResponseEntity.status(ApiHttpStatusResolver.resolve(code)).body(body);
    }

    /**
     * 构建基于业务码枚举的错误响应体。
     *
     * @param resultCode 业务码
     * @param message    错误消息
     * @param traceId    请求链路标识
     * @param path       请求路径
     * @return 统一错误响应体
     */
    public static R<Void> buildErrorBody(ResultCode resultCode, String message, String traceId, String path) {
        return buildErrorBody(resultCode.getCode(), message, traceId, path);
    }

    /**
     * 构建基于自定义业务码的错误响应体。
     *
     * @param code    业务码
     * @param message 错误消息
     * @param traceId 请求链路标识
     * @param path    请求路径
     * @return 统一错误响应体
     */
    public static R<Void> buildErrorBody(long code, String message, String traceId, String path) {
        R<Void> body = R.custom(code, message, null);
        body.setTimestamp(ISO_OFFSET_FORMATTER.format(OffsetDateTime.now(ZoneId.systemDefault())));
        body.setTraceId(resolveOrGenerateTraceId(traceId));
        body.setPath(StringUtils.hasText(path) ? path : null);
        return body;
    }

    /**
     * 解析传入 traceId，缺失时自动生成。
     *
     * @param traceId 原始 traceId
     * @return 规范化后的 traceId
     */
    public static String resolveOrGenerateTraceId(String traceId) {
        if (StringUtils.hasText(traceId)) {
            return traceId.trim();
        }
        return generateTraceId();
    }

    /**
     * 生成 16 位十六进制 traceId。
     *
     * @return traceId
     */
    public static String generateTraceId() {
        String raw = java.util.UUID.randomUUID().toString().replace("-", "");
        return raw.length() <= TRACE_ID_LENGTH ? raw : raw.substring(0, TRACE_ID_LENGTH);
    }
}

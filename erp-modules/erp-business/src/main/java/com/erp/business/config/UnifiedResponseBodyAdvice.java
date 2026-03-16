package com.erp.business.config;

import com.erp.common.core.context.RequestTraceContextHolder;
import com.erp.common.core.domain.R;
import com.erp.common.core.domain.ResultCode;
import org.springframework.core.MethodParameter;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpResponse;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyAdvice;

import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

/**
 * 统一响应增强器。
 */
@RestControllerAdvice
public class UnifiedResponseBodyAdvice implements ResponseBodyAdvice<Object> {
    private static final DateTimeFormatter ISO_OFFSET_FORMATTER = DateTimeFormatter.ISO_OFFSET_DATE_TIME;

    /**
     * 判断是否需要包装响应体。
     *
     * @param returnType 返回值类型
     * @param converterType 转换器类型
     * @return true 表示需要处理
     */
    @Override
    public boolean supports(MethodParameter returnType, Class<? extends HttpMessageConverter<?>> converterType) {
        return true;
    }

    /**
     * 在响应写出前补齐统一字段。
     *
     * @param body 原始响应体
     * @param returnType 返回值类型
     * @param selectedContentType 响应内容类型
     * @param selectedConverterType 转换器类型
     * @param request 请求对象
     * @param response 响应对象
     * @return 处理后的响应体
     */
    @Override
    public Object beforeBodyWrite(Object body,
            MethodParameter returnType,
            MediaType selectedContentType,
            Class<? extends HttpMessageConverter<?>> selectedConverterType,
            ServerHttpRequest request,
            ServerHttpResponse response) {
        if (!(body instanceof R<?>)) {
            return body;
        }
        R<?> result = (R<?>) body;
        if (result.getCode() == ResultCode.VALIDATE_FAILED.getCode()) {
            result.setCode(ResultCode.PARAM_ERROR.getCode());
            if (ResultCode.VALIDATE_FAILED.getMessage().equals(result.getMessage())) {
                result.setMessage(ResultCode.PARAM_ERROR.getMessage());
            }
        }
        if (result.getTimestamp() == null || result.getTimestamp().isEmpty()) {
            result.setTimestamp(ISO_OFFSET_FORMATTER.format(OffsetDateTime.now(ZoneId.systemDefault())));
        }
        if (result.getTraceId() == null || result.getTraceId().isEmpty()) {
            result.setTraceId(RequestTraceContextHolder.getTraceId());
        }
        if (result.getPath() == null || result.getPath().isEmpty()) {
            result.setPath(request == null ? null : request.getURI().getPath());
        }
        if (response instanceof ServletServerHttpResponse) {
            ServletServerHttpResponse servletResponse = (ServletServerHttpResponse) response;
            servletResponse.setStatusCode(ApiHttpStatusResolver.resolve(result.getCode()));
            response.getHeaders().setContentType(new MediaType(MediaType.APPLICATION_JSON, StandardCharsets.UTF_8));
        }
        return result;
    }
}

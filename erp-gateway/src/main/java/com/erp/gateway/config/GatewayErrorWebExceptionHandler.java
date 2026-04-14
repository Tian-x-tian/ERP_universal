package com.erp.gateway.config;

import com.erp.common.core.domain.ResultCode;
import com.erp.common.web.error.ApiErrorResponseWriter;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.web.reactive.error.ErrorWebExceptionHandler;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.ErrorResponse;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

/**
 * 网关全局异常处理器。
 * 负责将未被网关认证过滤器消化的 Reactive 异常统一写出为 JSON 错误体。
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class GatewayErrorWebExceptionHandler implements ErrorWebExceptionHandler {
    private static final Logger LOG = LoggerFactory.getLogger(GatewayErrorWebExceptionHandler.class);

    private final ObjectMapper objectMapper;

    public GatewayErrorWebExceptionHandler(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    /**
     * 处理网关异常并写出统一错误响应。
     *
     * @param exchange 请求交换对象
     * @param ex       异常对象
     * @return 写出结果
     */
    @Override
    public Mono<Void> handle(ServerWebExchange exchange, Throwable ex) {
        if (exchange.getResponse().isCommitted()) {
            return Mono.error(ex);
        }
        ResultCode resultCode = resolveResultCode(ex);
        String message = resolveMessage(ex, resultCode);
        if (ResultCode.ERROR.equals(resultCode)) {
            LOG.error("Unhandled gateway exception.", ex);
        } else {
            LOG.warn("Handled gateway exception with status mapping: {}", message, ex);
        }
        return ApiErrorResponseWriter.writeReactive(exchange, objectMapper, resultCode, message);
    }

    /**
     * 根据异常对象解析业务码。
     *
     * @param ex 异常对象
     * @return 业务码
     */
    private ResultCode resolveResultCode(Throwable ex) {
        if (ex instanceof ErrorResponse errorResponse) {
            HttpStatus httpStatus = HttpStatus.resolve(errorResponse.getStatusCode().value());
            return resolveResultCodeByHttpStatus(httpStatus);
        }
        return ResultCode.ERROR;
    }

    /**
     * 解析错误提示。
     *
     * @param ex         异常对象
     * @param resultCode 业务码
     * @return 错误提示
     */
    private String resolveMessage(Throwable ex, ResultCode resultCode) {
        if (ex instanceof ResponseStatusException responseStatusException
                && StringUtils.hasText(responseStatusException.getReason())) {
            return responseStatusException.getReason();
        }
        if (!ResultCode.ERROR.equals(resultCode) && StringUtils.hasText(ex.getMessage())) {
            return ex.getMessage();
        }
        return resultCode.getMessage();
    }

    /**
     * 按 HTTP 状态码映射业务码。
     *
     * @param status HTTP 状态码
     * @return 业务码
     */
    private ResultCode resolveResultCodeByHttpStatus(HttpStatus status) {
        if (HttpStatus.BAD_REQUEST.equals(status)) {
            return ResultCode.PARAM_ERROR;
        }
        if (HttpStatus.UNAUTHORIZED.equals(status)) {
            return ResultCode.UNAUTHORIZED;
        }
        if (HttpStatus.FORBIDDEN.equals(status)) {
            return ResultCode.FORBIDDEN;
        }
        if (HttpStatus.NOT_FOUND.equals(status)) {
            return ResultCode.NOT_FOUND;
        }
        if (HttpStatus.CONFLICT.equals(status)) {
            return ResultCode.CONFLICT;
        }
        return ResultCode.ERROR;
    }
}

package com.erp.system.config;

import com.erp.common.core.domain.ResultCode;
import org.springframework.http.HttpStatus;

/**
 * 统一业务码到 HTTP 状态码映射工具。
 */
public final class ApiHttpStatusResolver {

    private ApiHttpStatusResolver() {
    }

    /**
     * 根据业务码解析 HTTP 状态码。
     *
     * @param code 业务码
     * @return HTTP 状态码
     */
    public static HttpStatus resolve(long code) {
        if (code == ResultCode.SUCCESS.getCode()) {
            return HttpStatus.OK;
        }
        if (code == ResultCode.PARAM_ERROR.getCode()) {
            return HttpStatus.BAD_REQUEST;
        }
        if (code == ResultCode.UNAUTHORIZED.getCode()) {
            return HttpStatus.UNAUTHORIZED;
        }
        if (code == ResultCode.FORBIDDEN.getCode()) {
            return HttpStatus.FORBIDDEN;
        }
        if (code == ResultCode.NOT_FOUND.getCode()) {
            return HttpStatus.NOT_FOUND;
        }
        if (code == ResultCode.CONFLICT.getCode()) {
            return HttpStatus.CONFLICT;
        }
        if (code == ResultCode.VALIDATE_FAILED.getCode()) {
            return HttpStatus.BAD_REQUEST;
        }
        if (code == ResultCode.ERROR.getCode()) {
            return HttpStatus.INTERNAL_SERVER_ERROR;
        }
        if (code >= 40000 && code < 50000) {
            return HttpStatus.BAD_REQUEST;
        }
        return HttpStatus.INTERNAL_SERVER_ERROR;
    }
}

package com.erp.system.exception;

import com.erp.common.web.exception.GlobalExceptionHandlerSupport;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * 全局异常处理器。
 * 统一将异常转换为规范化业务码与 HTTP 状态码。
 */
@RestControllerAdvice
public class GlobalExceptionHandler extends GlobalExceptionHandlerSupport {
}

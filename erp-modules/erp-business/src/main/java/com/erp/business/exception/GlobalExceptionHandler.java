package com.erp.business.exception;

import com.erp.common.web.exception.GlobalExceptionHandlerSupport;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * 业务模块全局异常处理器。
 */
@RestControllerAdvice
public class GlobalExceptionHandler extends GlobalExceptionHandlerSupport {
}

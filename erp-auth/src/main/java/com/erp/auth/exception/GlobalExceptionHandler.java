package com.erp.auth.exception;

import com.erp.common.web.exception.GlobalExceptionHandlerSupport;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * 认证模块全局异常处理器。
 * 负责将控制器层抛出的异常统一转换为标准错误响应。
 */
@RestControllerAdvice
public class GlobalExceptionHandler extends GlobalExceptionHandlerSupport {
}

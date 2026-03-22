package com.erp.common.web.exception;

import com.erp.common.core.domain.R;
import com.erp.common.core.domain.ResultCode;
import com.erp.common.core.exception.ServiceException;
import com.erp.common.web.ApiHttpStatusResolver;
import jakarta.validation.ConstraintViolationException;
import org.mybatis.spring.MyBatisSystemException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.jdbc.CannotGetJdbcConnectionException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.BindException;
import org.springframework.validation.ObjectError;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.util.Optional;

/**
 * 全局异常处理器基类。
 */
public class GlobalExceptionHandlerSupport {
    private static final Logger LOG = LoggerFactory.getLogger(GlobalExceptionHandlerSupport.class);

    /**
     * 处理请求体参数校验异常。
     *
     * @param ex 异常对象
     * @return 统一错误响应
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<R<Void>> handleMethodArgumentNotValidException(MethodArgumentNotValidException ex) {
        String message = Optional.ofNullable(ex.getBindingResult().getAllErrors())
                .flatMap(errors -> errors.stream().findFirst())
                .map(ObjectError::getDefaultMessage)
                .orElse(ResultCode.PARAM_ERROR.getMessage());
        return buildErrorResponse(ResultCode.PARAM_ERROR, message);
    }

    /**
     * 处理绑定参数异常。
     *
     * @param ex 异常对象
     * @return 统一错误响应
     */
    @ExceptionHandler(BindException.class)
    public ResponseEntity<R<Void>> handleBindException(BindException ex) {
        String message = Optional.ofNullable(ex.getAllErrors())
                .flatMap(errors -> errors.stream().findFirst())
                .map(ObjectError::getDefaultMessage)
                .orElse(ResultCode.PARAM_ERROR.getMessage());
        return buildErrorResponse(ResultCode.PARAM_ERROR, message);
    }

    /**
     * 处理方法参数校验异常。
     *
     * @param ex 异常对象
     * @return 统一错误响应
     */
    @ExceptionHandler(HandlerMethodValidationException.class)
    public ResponseEntity<R<Void>> handleHandlerMethodValidationException(HandlerMethodValidationException ex) {
        return buildErrorResponse(ResultCode.PARAM_ERROR, ResultCode.PARAM_ERROR.getMessage());
    }

    /**
     * 处理约束校验异常。
     *
     * @param ex 异常对象
     * @return 统一错误响应
     */
    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<R<Void>> handleConstraintViolationException(ConstraintViolationException ex) {
        return buildErrorResponse(ResultCode.PARAM_ERROR, ex.getMessage());
    }

    /**
     * 处理缺失请求参数异常。
     *
     * @param ex 异常对象
     * @return 统一错误响应
     */
    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<R<Void>> handleMissingServletRequestParameterException(
            MissingServletRequestParameterException ex) {
        return buildErrorResponse(ResultCode.PARAM_ERROR, "缺少参数: " + ex.getParameterName());
    }

    /**
     * 处理请求体反序列化异常。
     *
     * @param ex 异常对象
     * @return 统一错误响应
     */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<R<Void>> handleHttpMessageNotReadableException(HttpMessageNotReadableException ex) {
        return buildErrorResponse(ResultCode.PARAM_ERROR, ResultCode.PARAM_ERROR.getMessage());
    }

    /**
     * 处理不支持的请求方法异常。
     *
     * @param ex 异常对象
     * @return 统一错误响应
     */
    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<R<Void>> handleHttpRequestMethodNotSupportedException(
            HttpRequestMethodNotSupportedException ex) {
        return buildErrorResponse(ResultCode.PARAM_ERROR, ex.getMessage());
    }

    /**
     * 处理认证鉴权异常。
     *
     * @param ex 异常对象
     * @return 统一错误响应
     */
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<R<Void>> handleAccessDeniedException(AccessDeniedException ex) {
        return buildErrorResponse(ResultCode.FORBIDDEN, ResultCode.FORBIDDEN.getMessage());
    }

    /**
     * 处理资源不存在异常。
     *
     * @param ex 异常对象
     * @return 统一错误响应
     */
    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<R<Void>> handleNoResourceFoundException(NoResourceFoundException ex) {
        return buildErrorResponse(ResultCode.NOT_FOUND, ResultCode.NOT_FOUND.getMessage());
    }

    /**
     * 处理主键/唯一约束冲突异常。
     *
     * @param ex 异常对象
     * @return 统一错误响应
     */
    @ExceptionHandler(DuplicateKeyException.class)
    public ResponseEntity<R<Void>> handleDuplicateKeyException(DuplicateKeyException ex) {
        return buildErrorResponse(ResultCode.CONFLICT, ResultCode.CONFLICT.getMessage());
    }

    /**
     * 处理业务服务异常，并透传约定业务码。
     *
     * @param ex 异常对象
     * @return 统一错误响应
     */
    @ExceptionHandler(ServiceException.class)
    public ResponseEntity<R<Void>> handleServiceException(ServiceException ex) {
        int errorCode = Optional.ofNullable(ex.getCode()).orElse((int) ResultCode.ERROR.getCode());
        String message = Optional.ofNullable(ex.getMessage()).orElse(ResultCode.ERROR.getMessage());
        R<Void> body = R.custom(errorCode, message, null);
        return ResponseEntity.status(ApiHttpStatusResolver.resolve(errorCode)).body(body);
    }

    /**
     * 处理非法状态异常。
     *
     * @param ex 异常对象
     * @return 统一错误响应
     */
    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<R<Void>> handleIllegalStateException(IllegalStateException ex) {
        String message = Optional.ofNullable(ex.getMessage()).orElse(ResultCode.CONFLICT.getMessage());
        return buildErrorResponse(ResultCode.CONFLICT, message);
    }

    /**
     * 处理非法参数异常。
     *
     * @param ex 异常对象
     * @return 统一错误响应
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<R<Void>> handleIllegalArgumentException(IllegalArgumentException ex) {
        String message = Optional.ofNullable(ex.getMessage()).orElse(ResultCode.PARAM_ERROR.getMessage());
        return buildErrorResponse(ResultCode.PARAM_ERROR, message);
    }

    /**
     * 处理数据库连接异常。
     *
     * @param ex 异常对象
     * @return 统一错误响应
     */
    @ExceptionHandler({CannotGetJdbcConnectionException.class, MyBatisSystemException.class})
    public ResponseEntity<R<Void>> handleDatabaseConnectivityException(Exception ex) {
        if (containsDatabaseConnectionFailure(ex)) {
            LOG.error("Datasource is unavailable.", ex);
            return buildErrorResponse(ResultCode.ERROR, "数据库连接异常");
        }
        LOG.error("Database exception in API layer.", ex);
        return buildErrorResponse(ResultCode.ERROR, ResultCode.ERROR.getMessage());
    }

    /**
     * 兜底处理所有未捕获异常。
     *
     * @param ex 异常对象
     * @return 统一错误响应
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<R<Void>> handleException(Exception ex) {
        LOG.error("Unhandled exception in API layer.", ex);
        return buildErrorResponse(ResultCode.ERROR, ResultCode.ERROR.getMessage());
    }

    /**
     * 构建统一错误响应实体。
     *
     * @param resultCode 业务码
     * @param message    错误消息
     * @return 响应实体
     */
    protected ResponseEntity<R<Void>> buildErrorResponse(ResultCode resultCode, String message) {
        R<Void> body = R.failed(resultCode, message);
        HttpStatus status = ApiHttpStatusResolver.resolve(resultCode.getCode());
        return ResponseEntity.status(status).body(body);
    }

    /**
     * 判断异常链中是否包含数据库连接失败。
     *
     * @param ex 异常对象
     * @return true 表示数据库连接不可用
     */
    protected boolean containsDatabaseConnectionFailure(Throwable ex) {
        Throwable current = ex;
        while (current != null) {
            if (current instanceof CannotGetJdbcConnectionException) {
                return true;
            }
            String message = current.getMessage();
            if (message != null && (message.contains("Communications link failure")
                    || message.contains("Connection refused")
                    || message.contains("Failed to obtain JDBC Connection"))) {
                return true;
            }
            current = current.getCause();
        }
        return false;
    }
}

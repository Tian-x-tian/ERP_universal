package com.erp.business.exception;

import com.erp.business.config.ApiHttpStatusResolver;
import com.erp.common.core.domain.R;
import com.erp.common.core.domain.ResultCode;
import com.erp.common.core.exception.ServiceException;
import org.mybatis.spring.MyBatisSystemException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.CannotGetJdbcConnectionException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Optional;

/**
 * 业务模块全局异常处理器。
 */
@RestControllerAdvice
public class GlobalExceptionHandler {
    private static final Logger LOG = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    /**
     * 处理业务服务异常。
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
     * 处理状态冲突异常。
     *
     * @param ex 异常对象
     * @return 统一错误响应
     */
    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<R<Void>> handleIllegalStateException(IllegalStateException ex) {
        return buildErrorResponse(ResultCode.CONFLICT,
                Optional.ofNullable(ex.getMessage()).orElse(ResultCode.CONFLICT.getMessage()));
    }

    /**
     * 处理参数异常。
     *
     * @param ex 异常对象
     * @return 统一错误响应
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<R<Void>> handleIllegalArgumentException(IllegalArgumentException ex) {
        return buildErrorResponse(ResultCode.PARAM_ERROR,
                Optional.ofNullable(ex.getMessage()).orElse(ResultCode.PARAM_ERROR.getMessage()));
    }

    /**
     * 处理唯一约束冲突异常。
     *
     * @param ex 异常对象
     * @return 统一错误响应
     */
    @ExceptionHandler(DuplicateKeyException.class)
    public ResponseEntity<R<Void>> handleDuplicateKeyException(DuplicateKeyException ex) {
        return buildErrorResponse(ResultCode.CONFLICT, "数据已存在，请勿重复提交");
    }

    /**
     * 处理权限异常。
     *
     * @param ex 异常对象
     * @return 统一错误响应
     */
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<R<Void>> handleAccessDeniedException(AccessDeniedException ex) {
        return buildErrorResponse(ResultCode.FORBIDDEN, ResultCode.FORBIDDEN.getMessage());
    }

    /**
     * 处理业务模块数据库连接异常，并返回明确提示。
     *
     * @param ex 异常对象
     * @return 统一错误响应
     */
    @ExceptionHandler({CannotGetJdbcConnectionException.class, MyBatisSystemException.class})
    public ResponseEntity<R<Void>> handleDatabaseConnectivityException(Exception ex) {
        if (containsDatabaseConnectionFailure(ex)) {
            LOG.error("Business datasource is unavailable.", ex);
            return buildErrorResponse(ResultCode.ERROR, "数据库连接异常");
        }
        LOG.error("Database exception in business module.", ex);
        return buildErrorResponse(ResultCode.ERROR, ResultCode.ERROR.getMessage());
    }

    /**
     * 兜底处理未知异常。
     *
     * @param ex 异常对象
     * @return 统一错误响应
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<R<Void>> handleException(Exception ex) {
        LOG.error("Unhandled exception in business module.", ex);
        return buildErrorResponse(ResultCode.ERROR, ResultCode.ERROR.getMessage());
    }

    /**
     * 构建统一错误响应。
     *
     * @param resultCode 业务码
     * @param message 错误消息
     * @return 响应实体
     */
    private ResponseEntity<R<Void>> buildErrorResponse(ResultCode resultCode, String message) {
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
    private boolean containsDatabaseConnectionFailure(Throwable ex) {
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

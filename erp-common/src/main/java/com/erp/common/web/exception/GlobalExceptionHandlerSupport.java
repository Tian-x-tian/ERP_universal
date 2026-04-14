package com.erp.common.web.exception;

import com.erp.common.core.domain.R;
import com.erp.common.core.domain.ResultCode;
import com.erp.common.core.exception.ServiceException;
import com.erp.common.web.error.ApiErrorResponseFactory;
import jakarta.validation.ConstraintViolationException;
import org.mybatis.spring.MyBatisSystemException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.jdbc.BadSqlGrammarException;
import org.springframework.jdbc.CannotGetJdbcConnectionException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.BindException;
import org.springframework.validation.ObjectError;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.ResourceAccessException;
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
     * 处理 SQL 语法或表结构异常，优先给出可执行的结构修复提示。
     *
     * @param ex 异常对象
     * @return 统一错误响应
     */
    @ExceptionHandler(BadSqlGrammarException.class)
    public ResponseEntity<R<Void>> handleBadSqlGrammarException(BadSqlGrammarException ex) {
        if (containsSchemaMismatch(ex)) {
            LOG.error("Database schema mismatch in API layer.", ex);
            return buildErrorResponse(ResultCode.ERROR, "数据库结构异常，请执行对应模块初始化/升级脚本");
        }
        LOG.error("Bad SQL grammar in API layer.", ex);
        return buildErrorResponse(ResultCode.ERROR, ResultCode.ERROR.getMessage());
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
        return ApiErrorResponseFactory.buildResponseEntity(errorCode, message);
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
     * 处理内部 HTTP 调用返回的错误状态，尽量透传为对应的业务语义。
     *
     * @param ex 异常对象
     * @return 统一错误响应
     */
    @ExceptionHandler(HttpStatusCodeException.class)
    public ResponseEntity<R<Void>> handleHttpStatusCodeException(HttpStatusCodeException ex) {
        HttpStatus upstreamStatus = HttpStatus.resolve(ex.getStatusCode().value());
        if (upstreamStatus == null) {
            upstreamStatus = HttpStatus.INTERNAL_SERVER_ERROR;
        }
        ResultCode resultCode = resolveResultCodeByHttpStatus(upstreamStatus);
        String message = Optional.ofNullable(ex.getStatusText())
                .filter(text -> !text.isBlank())
                .orElse("内部服务调用失败");
        R<Void> body = R.failed(resultCode, message);
        return ResponseEntity.status(upstreamStatus).body(body);
    }

    /**
     * 处理内部 HTTP 调用网络不可达异常。
     *
     * @param ex 异常对象
     * @return 统一错误响应
     */
    @ExceptionHandler(ResourceAccessException.class)
    public ResponseEntity<R<Void>> handleResourceAccessException(ResourceAccessException ex) {
        LOG.error("Internal service is unreachable.", ex);
        return buildErrorResponse(ResultCode.ERROR, "内部服务不可达");
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
        return ApiErrorResponseFactory.buildResponseEntity(resultCode, message);
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

    /**
     * 判断异常链中是否包含数据库表或字段缺失导致的结构不匹配异常。
     *
     * @param ex 异常对象
     * @return true 表示数据库结构不匹配
     */
    protected boolean containsSchemaMismatch(Throwable ex) {
        Throwable current = ex;
        while (current != null) {
            String message = current.getMessage();
            if (message != null && (message.contains("doesn't exist")
                    || message.contains("Unknown column")
                    || message.contains("Table")
                    || message.contains("Column")
                    || message.contains("BadSqlGrammarException"))) {
                return true;
            }
            current = current.getCause();
        }
        return false;
    }

    /**
     * 按 HTTP 状态码映射内部业务码。
     *
     * @param status HTTP 状态码
     * @return 业务码
     */
    protected ResultCode resolveResultCodeByHttpStatus(HttpStatus status) {
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

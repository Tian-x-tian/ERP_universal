package com.erp.common.logging;

import com.erp.common.core.context.TenantContextHolder;
import com.erp.common.core.domain.R;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import org.aspectj.lang.ProceedingJoinPoint;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.StringUtils;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.multipart.MultipartFile;

import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * 查询审计日志切面基类。
 * 只采集 GET 请求；写操作由 {@link OperationLogInterceptorSupport} 负责。
 *
 * <p>各服务继承本类，用自己的切点表达式声明 {@code @Around} 并转调
 * {@link #recordAround(ProceedingJoinPoint)}，因为切点必须写死本服务的
 * controller 包路径，无法在公共模块里表达。</p>
 */
public abstract class AuditLogAspectSupport {
    protected static final String UNKNOWN_TENANT_ID = "UNKNOWN";
    protected static final int MAX_SERIALIZED_LENGTH = 2000;
    protected static final int MAX_ERROR_MESSAGE_LENGTH = 500;

    private static final Logger LOGGER = LoggerFactory.getLogger(AuditLogAspectSupport.class);

    private final OperationLogRecorder operationLogRecorder;
    private final ApiLogSanitizer apiLogSanitizer;
    private final ObjectMapper objectMapper;

    protected AuditLogAspectSupport(OperationLogRecorder operationLogRecorder, ObjectMapper objectMapper) {
        this.operationLogRecorder = operationLogRecorder;
        this.objectMapper = objectMapper;
        this.apiLogSanitizer = new ApiLogSanitizer(objectMapper);
    }

    /**
     * 环绕执行并记录查询审计日志。
     *
     * @param joinPoint 切点
     * @return 方法执行结果
     * @throws Throwable 方法原始异常
     */
    protected Object recordAround(ProceedingJoinPoint joinPoint) throws Throwable {
        HttpServletRequest request = getCurrentRequest();
        if (request == null || !needRecord(request)) {
            return joinPoint.proceed();
        }

        long startTime = System.currentTimeMillis();
        OperationLogPayload payload = buildBasePayload(request, joinPoint.getArgs());
        try {
            Object result = joinPoint.proceed();
            fillSuccess(payload, result, System.currentTimeMillis() - startTime);
            return result;
        } catch (Throwable ex) {
            fillFailure(payload, ex, System.currentTimeMillis() - startTime);
            throw ex;
        } finally {
            try {
                operationLogRecorder.record(payload);
            } catch (RuntimeException recordException) {
                LOGGER.warn("记录审计日志失败，请求路径 {}", request.getRequestURI(), recordException);
            }
        }
    }

    /**
     * 判断是否记录审计日志，仅记录查询请求。
     *
     * @param request 请求对象
     * @return true 表示需要记录
     */
    protected boolean needRecord(HttpServletRequest request) {
        if (!"GET".equalsIgnoreCase(request.getMethod())) {
            return false;
        }
        String requestUri = request.getRequestURI();
        return excludedUriPrefixes().stream().noneMatch(requestUri::startsWith);
    }

    /**
     * 不记录审计日志的路径前缀，供各服务覆盖补充（如日志查询接口自身）。
     *
     * @return 排除的路径前缀集合
     */
    protected Set<String> excludedUriPrefixes() {
        return Collections.emptySet();
    }

    /**
     * 解析当前操作人账号。
     *
     * @return 操作人账号
     */
    protected abstract String resolveOperator();

    /**
     * 构建审计日志基础字段。
     *
     * @param request 请求对象
     * @param args    控制器方法参数
     * @return 日志载荷
     */
    protected OperationLogPayload buildBasePayload(HttpServletRequest request, Object[] args) {
        OperationLogPayload payload = new OperationLogPayload();
        payload.setLogType(OperationLogPayload.TYPE_AUDIT);
        payload.setTenantId(resolveTenantId(request));
        payload.setOperator(resolveOperator());
        payload.setOperationType(request.getMethod());
        payload.setRequestMethod(request.getMethod());
        payload.setRequestUri(request.getRequestURI());
        payload.setRequestIp(getClientIp(request));
        payload.setRequestParams(serializeArgs(args, request));
        payload.setOperationTime(new Date());
        return payload;
    }

    /**
     * 填充成功结果。
     *
     * @param payload  日志载荷
     * @param result   方法返回值
     * @param costTime 耗时毫秒
     */
    protected void fillSuccess(OperationLogPayload payload, Object result, long costTime) {
        payload.setSuccessFlag("1");
        payload.setCostTime(costTime);
        payload.setResponseCode(result instanceof R<?> response ? (int) response.getCode() : 200);
    }

    /**
     * 填充失败结果。
     *
     * @param payload  日志载荷
     * @param ex       异常
     * @param costTime 耗时毫秒
     */
    protected void fillFailure(OperationLogPayload payload, Throwable ex, long costTime) {
        payload.setSuccessFlag("0");
        payload.setCostTime(costTime);
        payload.setResponseCode(500);
        String errorMsg = ex.getMessage();
        if (errorMsg != null && errorMsg.length() > MAX_ERROR_MESSAGE_LENGTH) {
            errorMsg = errorMsg.substring(0, MAX_ERROR_MESSAGE_LENGTH);
        }
        payload.setErrorMsg(errorMsg);
    }

    /**
     * 获取当前请求对象。
     *
     * @return 请求对象；非 Web 上下文返回 null
     */
    protected HttpServletRequest getCurrentRequest() {
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        return attributes == null ? null : attributes.getRequest();
    }

    /**
     * 解析租户编号。
     *
     * @param request 请求对象
     * @return 租户编号
     */
    protected String resolveTenantId(HttpServletRequest request) {
        String tenantId = request.getHeader("Tenantid");
        if (!StringUtils.hasText(tenantId)) {
            tenantId = request.getHeader("tenantId");
        }
        if (!StringUtils.hasText(tenantId)) {
            tenantId = TenantContextHolder.getTenantId();
        }
        return StringUtils.hasText(tenantId) ? tenantId.trim() : UNKNOWN_TENANT_ID;
    }

    /**
     * 获取客户端 IP。
     *
     * @param request 请求对象
     * @return 客户端 IP
     */
    protected String getClientIp(HttpServletRequest request) {
        String forwardedFor = request.getHeader("X-Forwarded-For");
        if (StringUtils.hasText(forwardedFor)) {
            return forwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }

    /**
     * 序列化并脱敏控制器方法参数。
     *
     * @param args    方法参数
     * @param request 请求对象
     * @return 序列化结果
     */
    protected String serializeArgs(Object[] args, HttpServletRequest request) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("query", apiLogSanitizer.sanitizeQueryString(request.getQueryString()));
        payload.put("args", apiLogSanitizer.sanitizeValue(filterSerializableArgs(args)));
        return apiLogSanitizer.writeCompactJson(payload, MAX_SERIALIZED_LENGTH);
    }

    /**
     * 过滤不可序列化参数。
     *
     * @param args 原始参数
     * @return 过滤后的参数数组
     */
    protected Object[] filterSerializableArgs(Object[] args) {
        if (args == null || args.length == 0) {
            return new Object[0];
        }
        Object[] filtered = new Object[args.length];
        for (int i = 0; i < args.length; i++) {
            Object arg = args[i];
            if (arg instanceof ServletRequest || arg instanceof ServletResponse || arg instanceof MultipartFile) {
                filtered[i] = null;
            } else {
                filtered[i] = arg;
            }
        }
        return filtered;
    }

    /**
     * 提供 JSON 序列化器给子类复用。
     *
     * @return ObjectMapper
     */
    protected ObjectMapper objectMapper() {
        return objectMapper;
    }
}

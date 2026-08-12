package com.erp.common.logging;

import com.erp.common.core.context.TenantContextHolder;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.StringUtils;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * 写操作日志拦截器基类。
 * 基于 Spring MVC 处理器链采集 POST/PUT/DELETE/PATCH 请求，避免 AOP 在部分场景下漏采集。
 * 各服务继承本类并注册到 {@code WebMvcConfigurer}，日志落地方式由 {@link OperationLogRecorder} 决定。
 */
public abstract class OperationLogInterceptorSupport implements HandlerInterceptor {
    /**
     * 标记当前请求已由拦截器负责记录操作日志。
     */
    public static final String OPERATION_LOG_ENABLED_ATTRIBUTE =
            OperationLogInterceptorSupport.class.getName() + ".enabled";

    protected static final String UNKNOWN_TENANT_ID = "UNKNOWN";
    protected static final int MAX_SERIALIZED_LENGTH = 2000;
    protected static final int MAX_ERROR_MESSAGE_LENGTH = 500;

    private static final Logger LOGGER = LoggerFactory.getLogger(OperationLogInterceptorSupport.class);
    private static final String START_TIME_ATTRIBUTE = OperationLogInterceptorSupport.class.getName() + ".startTime";

    private final OperationLogRecorder operationLogRecorder;
    private final ApiLogSanitizer apiLogSanitizer;

    protected OperationLogInterceptorSupport(OperationLogRecorder operationLogRecorder, ObjectMapper objectMapper) {
        this.operationLogRecorder = operationLogRecorder;
        this.apiLogSanitizer = new ApiLogSanitizer(objectMapper);
    }

    /**
     * 记录请求开始时间并打上采集标记。
     *
     * @param request  请求对象
     * @param response 响应对象
     * @param handler  处理器
     * @return true 表示继续执行
     */
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        if (!(handler instanceof HandlerMethod) || !needRecord(request)) {
            return true;
        }
        request.setAttribute(OPERATION_LOG_ENABLED_ATTRIBUTE, Boolean.TRUE);
        request.setAttribute(START_TIME_ATTRIBUTE, System.currentTimeMillis());
        return true;
    }

    /**
     * 请求处理完成后落库操作日志。
     *
     * @param request  请求对象
     * @param response 响应对象
     * @param handler  处理器
     * @param ex       处理异常
     */
    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        if (!Boolean.TRUE.equals(request.getAttribute(OPERATION_LOG_ENABLED_ATTRIBUTE))) {
            return;
        }
        long startTime = resolveStartTime(request);
        OperationLogPayload payload = buildBasePayload(request, handler);
        payload.setLogType(OperationLogPayload.TYPE_OPERATION);
        fillResult(payload, response, ex, System.currentTimeMillis() - startTime);
        try {
            operationLogRecorder.record(payload);
        } catch (RuntimeException recordException) {
            LOGGER.warn("记录操作日志失败，请求路径 {}", request.getRequestURI(), recordException);
        }
    }

    /**
     * 判断请求是否需要记录操作日志。
     *
     * @param request 请求对象
     * @return true 表示需要记录
     */
    protected boolean needRecord(HttpServletRequest request) {
        if ("GET".equalsIgnoreCase(request.getMethod())) {
            return false;
        }
        String requestUri = request.getRequestURI();
        if (isAuthLifecycleRequest(requestUri)) {
            return false;
        }
        return excludedUriPrefixes().stream().noneMatch(requestUri::startsWith);
    }

    /**
     * 不记录日志的路径前缀，供各服务覆盖补充。
     *
     * @return 排除的路径前缀集合
     */
    protected Set<String> excludedUriPrefixes() {
        return Collections.emptySet();
    }

    /**
     * 判断是否登录态生命周期接口，这类请求由登录日志单独记录。
     *
     * @param requestUri 请求路径
     * @return true 表示认证生命周期接口
     */
    protected boolean isAuthLifecycleRequest(String requestUri) {
        return "/login".equals(requestUri)
                || "/logout".equals(requestUri)
                || "/auth/login".equals(requestUri)
                || "/auth/logout".equals(requestUri);
    }

    /**
     * 解析当前操作人账号，由各服务基于自身登录用户解析器实现。
     *
     * @return 操作人账号
     */
    protected abstract String resolveOperator();

    /**
     * 构建日志基础字段。
     *
     * @param request 请求对象
     * @param handler 处理器
     * @return 日志载荷
     */
    protected OperationLogPayload buildBasePayload(HttpServletRequest request, Object handler) {
        OperationLogPayload payload = new OperationLogPayload();
        payload.setTenantId(resolveTenantId(request));
        payload.setOperator(resolveOperator());
        payload.setOperationType(request.getMethod());
        payload.setRequestMethod(request.getMethod());
        payload.setRequestUri(request.getRequestURI());
        payload.setRequestIp(getClientIp(request));
        payload.setRequestParams(serializeRequest(request, handler));
        payload.setOperationTime(new Date());
        return payload;
    }

    /**
     * 填充执行结果。
     *
     * @param payload  日志载荷
     * @param response 响应对象
     * @param ex       处理异常
     * @param costTime 耗时毫秒
     */
    protected void fillResult(OperationLogPayload payload, HttpServletResponse response, Exception ex, long costTime) {
        int responseStatus = response == null ? 200 : response.getStatus();
        payload.setCostTime(costTime);
        payload.setResponseCode(responseStatus);
        if (ex == null && responseStatus < 400) {
            payload.setSuccessFlag("1");
            return;
        }
        payload.setSuccessFlag("0");
        if (ex != null && StringUtils.hasText(ex.getMessage())) {
            payload.setErrorMsg(truncate(ex.getMessage().trim(), MAX_ERROR_MESSAGE_LENGTH));
        }
    }

    /**
     * 序列化并脱敏请求参数。
     *
     * @param request 请求对象
     * @param handler 处理器
     * @return 序列化结果
     */
    protected String serializeRequest(HttpServletRequest request, Object handler) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("query", apiLogSanitizer.sanitizeQueryString(request.getQueryString()));
        payload.put("params", apiLogSanitizer.sanitizeParameterMap(request.getParameterMap()));
        if (handler instanceof HandlerMethod handlerMethod) {
            payload.put("handler", handlerMethod.getBeanType().getSimpleName() + "#" + handlerMethod.getMethod().getName());
        }
        return apiLogSanitizer.writeCompactJson(payload, MAX_SERIALIZED_LENGTH);
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
     * 截断超长字符串。
     *
     * @param value     原始值
     * @param maxLength 最大长度
     * @return 截断结果
     */
    protected String truncate(String value, int maxLength) {
        if (value == null || value.length() <= maxLength) {
            return value;
        }
        return value.substring(0, maxLength);
    }

    /**
     * 解析请求起始时间。
     *
     * @param request 请求对象
     * @return 起始时间戳
     */
    private long resolveStartTime(HttpServletRequest request) {
        Object startTime = request.getAttribute(START_TIME_ATTRIBUTE);
        return startTime instanceof Long value ? value : System.currentTimeMillis();
    }
}

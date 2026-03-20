package com.erp.system.audit;

import com.erp.common.core.context.TenantContextHolder;
import com.erp.system.domain.SysOperLog;
import com.erp.system.security.service.SecurityUserResolver;
import com.erp.system.service.ISysOperLogService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * 操作日志拦截器。
 * 直接基于 Spring MVC 处理器链记录写操作，避免 AOP 在部分场景下漏采集。
 */
@Component
public class OperationLogInterceptor implements HandlerInterceptor {
    /**
     * 标记当前请求由拦截器负责记录操作日志。
     */
    public static final String OPERATION_LOG_ENABLED_ATTRIBUTE =
            OperationLogInterceptor.class.getName() + ".enabled";

    private static final Logger LOGGER = LoggerFactory.getLogger(OperationLogInterceptor.class);
    private static final String UNKNOWN_TENANT_ID = "UNKNOWN";
    private static final String START_TIME_ATTRIBUTE = OperationLogInterceptor.class.getName() + ".startTime";
    private static final int MAX_SERIALIZED_LENGTH = 2000;
    private static final int MAX_ERROR_MESSAGE_LENGTH = 500;

    private final ISysOperLogService operLogService;
    private final SecurityUserResolver securityUserResolver;
    private final ObjectMapper objectMapper;

    public OperationLogInterceptor(ISysOperLogService operLogService,
            SecurityUserResolver securityUserResolver,
            ObjectMapper objectMapper) {
        this.operLogService = operLogService;
        this.securityUserResolver = securityUserResolver;
        this.objectMapper = objectMapper;
    }

    /**
     * 在控制器执行前记录开始时间，并标记当前请求需要采集操作日志。
     *
     * @param request  请求对象
     * @param response 响应对象
     * @param handler  处理器
     * @return true 表示继续执行
     */
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        if (!shouldRecord(request, handler)) {
            return true;
        }
        request.setAttribute(OPERATION_LOG_ENABLED_ATTRIBUTE, Boolean.TRUE);
        request.setAttribute(START_TIME_ATTRIBUTE, System.currentTimeMillis());
        return true;
    }

    /**
     * 在控制器执行完成后落库操作日志。
     *
     * @param request  请求对象
     * @param response 响应对象
     * @param handler  处理器
     * @param ex       控制器执行异常
     */
    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        if (!Boolean.TRUE.equals(request.getAttribute(OPERATION_LOG_ENABLED_ATTRIBUTE))) {
            return;
        }
        long startTime = resolveStartTime(request);
        SysOperLog log = buildBaseLog(request, handler);
        fillResult(log, response, ex, System.currentTimeMillis() - startTime);
        try {
            operLogService.save(log);
        } catch (RuntimeException saveException) {
            LOGGER.warn("Failed to persist operation log for request uri {}", request.getRequestURI(), saveException);
        }
    }

    /**
     * 判断当前请求是否应记录操作日志。
     *
     * @param request 请求对象
     * @param handler 处理器
     * @return true 表示需要记录
     */
    private boolean shouldRecord(HttpServletRequest request, Object handler) {
        return handler instanceof HandlerMethod && needRecord(request);
    }

    /**
     * 判断请求方法和路径是否满足操作日志记录条件。
     *
     * @param request 请求对象
     * @return true 表示需要记录
     */
    private boolean needRecord(HttpServletRequest request) {
        String requestMethod = request.getMethod();
        if ("GET".equalsIgnoreCase(requestMethod)) {
            return false;
        }
        String requestUri = request.getRequestURI();
        return !"/login".equals(requestUri)
                && !"/logout".equals(requestUri)
                && !"/internal/auth/login".equals(requestUri)
                && !"/internal/auth/logout".equals(requestUri)
                && !requestUri.startsWith("/system/login/log")
                && !requestUri.startsWith("/system/audit/log")
                && !requestUri.startsWith("/system/oper/log");
    }

    /**
     * 解析请求起始时间。
     *
     * @param request 请求对象
     * @return 起始时间戳；缺失时返回当前时间
     */
    private long resolveStartTime(HttpServletRequest request) {
        Object startTime = request.getAttribute(START_TIME_ATTRIBUTE);
        return startTime instanceof Long ? (Long) startTime : System.currentTimeMillis();
    }

    /**
     * 构建操作日志基础字段。
     *
     * @param request 请求对象
     * @param handler 处理器
     * @return 操作日志对象
     */
    private SysOperLog buildBaseLog(HttpServletRequest request, Object handler) {
        SysOperLog log = new SysOperLog();
        log.setTenantId(resolveTenantId(request));
        log.setOperator(securityUserResolver.getCurrentUsername());
        log.setRequestMethod(request.getMethod());
        log.setRequestUri(request.getRequestURI());
        log.setRequestIp(getClientIp(request));
        log.setRequestParams(serializeRequest(request, handler));
        log.setOperationTime(new Date());
        return log;
    }

    /**
     * 填充执行结果信息。
     *
     * @param log      操作日志对象
     * @param response 响应对象
     * @param ex       执行异常
     * @param costTime 执行耗时
     */
    private void fillResult(SysOperLog log, HttpServletResponse response, Exception ex, long costTime) {
        int responseStatus = response == null ? 200 : response.getStatus();
        log.setCostTime(costTime);
        log.setResponseCode(responseStatus);
        if (ex == null && responseStatus < 400) {
            log.setSuccessFlag("1");
            return;
        }
        log.setSuccessFlag("0");
        if (ex != null && StringUtils.hasText(ex.getMessage())) {
            String errorMsg = ex.getMessage().trim();
            log.setErrorMsg(errorMsg.length() > MAX_ERROR_MESSAGE_LENGTH
                    ? errorMsg.substring(0, MAX_ERROR_MESSAGE_LENGTH)
                    : errorMsg);
        }
    }

    /**
     * 序列化请求参数。
     *
     * @param request 请求对象
     * @param handler 处理器
     * @return 序列化后的请求参数
     */
    private String serializeRequest(HttpServletRequest request, Object handler) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("query", request.getQueryString());
        payload.put("params", request.getParameterMap());
        if (handler instanceof HandlerMethod) {
            HandlerMethod handlerMethod = (HandlerMethod) handler;
            payload.put("handler", handlerMethod.getBeanType().getSimpleName() + "#" + handlerMethod.getMethod().getName());
        }
        try {
            String json = objectMapper.writeValueAsString(payload);
            return json.length() > MAX_SERIALIZED_LENGTH ? json.substring(0, MAX_SERIALIZED_LENGTH) : json;
        } catch (JsonProcessingException ex) {
            return "{\"error\":\"request-params-serialize-failed\"}";
        }
    }

    /**
     * 解析租户编号。
     *
     * @param request 请求对象
     * @return 租户编号
     */
    private String resolveTenantId(HttpServletRequest request) {
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
    private String getClientIp(HttpServletRequest request) {
        String forwardedFor = request.getHeader("X-Forwarded-For");
        if (StringUtils.hasText(forwardedFor)) {
            return forwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}

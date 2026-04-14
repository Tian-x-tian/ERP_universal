package com.erp.system.logging;

import com.erp.common.core.context.RequestTraceContextHolder;
import com.erp.common.core.context.TenantContextHolder;
import com.erp.common.core.domain.R;
import com.erp.system.security.service.SecurityUserResolver;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 控制层接口请求日志切面。
 * 统一向终端输出请求入参、响应摘要、耗时与 traceId，便于联调排障。
 */
@Aspect
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 20)
public class ApiRequestLogAspect {
    private static final Logger log = LoggerFactory.getLogger(ApiRequestLogAspect.class);
    private static final int MAX_LOG_LENGTH = 4000;
    private static final String NULL_VALUE = "null";

    private final SecurityUserResolver securityUserResolver;
    private final ApiLogSanitizer apiLogSanitizer;

    public ApiRequestLogAspect(ObjectMapper objectMapper, SecurityUserResolver securityUserResolver) {
        this.securityUserResolver = securityUserResolver;
        this.apiLogSanitizer = new ApiLogSanitizer(objectMapper);
    }

    /**
     * 统一记录控制层接口请求与响应摘要。
     *
     * @param joinPoint 切点
     * @return 方法执行结果
     * @throws Throwable 原始异常
     */
    @Around("execution(* com.erp.system.controller..*(..))")
    public Object aroundController(ProceedingJoinPoint joinPoint) throws Throwable {
        HttpServletRequest request = getCurrentRequest();
        if (request == null) {
            return joinPoint.proceed();
        }

        long startTime = System.currentTimeMillis();
        String method = request.getMethod();
        String path = request.getRequestURI();
        String traceId = resolveTraceId(request);
        String tenantId = resolveTenantId(request);
        String username = resolveUsername();
        String clientIp = resolveClientIp(request);
        String handler = joinPoint.getSignature().toShortString();
        String requestPayload = buildRequestPayload(request, joinPoint.getArgs());

        log.info("API_REQUEST traceId={} tenantId={} user={} ip={} method={} path={} handler={} payload={}",
                traceId, tenantId, username, clientIp, method, path, handler, requestPayload);

        try {
            Object result = joinPoint.proceed();
            long costTime = System.currentTimeMillis() - startTime;
            log.info("API_RESPONSE traceId={} tenantId={} user={} method={} path={} costMs={} result={}",
                    traceId, tenantId, username, method, path, costTime, buildResponsePayload(result));
            return result;
        } catch (Throwable ex) {
            long costTime = System.currentTimeMillis() - startTime;
            log.error("API_ERROR traceId={} tenantId={} user={} method={} path={} costMs={} errorType={} message={}",
                    traceId,
                    tenantId,
                    username,
                    method,
                    path,
                    costTime,
                    ex.getClass().getSimpleName(),
                    apiLogSanitizer.truncate(ex.getMessage(), MAX_LOG_LENGTH),
                    ex);
            throw ex;
        }
    }

    /**
     * 获取当前 HTTP 请求。
     *
     * @return 当前请求对象
     */
    private HttpServletRequest getCurrentRequest() {
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        return attributes == null ? null : attributes.getRequest();
    }

    /**
     * 构造请求日志摘要。
     *
     * @param request 请求对象
     * @param args    控制器参数
     * @return JSON 字符串摘要
     */
    private String buildRequestPayload(HttpServletRequest request, Object[] args) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("query", request == null ? null : apiLogSanitizer.sanitizeQueryString(request.getQueryString()));
        payload.put("args", apiLogSanitizer.sanitizeValue(filterArguments(args)));
        return apiLogSanitizer.writeCompactJson(payload, MAX_LOG_LENGTH);
    }

    /**
     * 构造响应日志摘要。
     *
     * @param result 控制器返回结果
     * @return JSON 字符串摘要
     */
    private String buildResponsePayload(Object result) {
        if (result instanceof R<?> response) {
            Map<String, Object> payload = new LinkedHashMap<>();
            payload.put("code", response.getCode());
            payload.put("message", response.getMessage());
            payload.put("success", response.isSuccess());
            payload.put("data", apiLogSanitizer.sanitizeValue(response.getData()));
            return apiLogSanitizer.writeCompactJson(payload, MAX_LOG_LENGTH);
        }
        return apiLogSanitizer.writeCompactJson(apiLogSanitizer.sanitizeValue(result), MAX_LOG_LENGTH);
    }

    /**
     * 过滤不可序列化参数。
     *
     * @param args 原始参数
     * @return 过滤后的参数列表
     */
    private List<Object> filterArguments(Object[] args) {
        if (args == null || args.length == 0) {
            return List.of();
        }
        List<Object> filtered = new ArrayList<>(args.length);
        for (Object arg : args) {
            if (arg instanceof ServletRequest || arg instanceof ServletResponse) {
                filtered.add("[servlet-object]");
                continue;
            }
            if (arg instanceof MultipartFile multipartFile) {
                filtered.add(buildMultipartFileSummary(multipartFile));
                continue;
            }
            filtered.add(arg);
        }
        return filtered;
    }

    /**
     * 构造上传文件参数摘要。
     *
     * @param multipartFile 上传文件
     * @return 摘要对象
     */
    private Map<String, Object> buildMultipartFileSummary(MultipartFile multipartFile) {
        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("name", multipartFile.getName());
        summary.put("originalFilename", multipartFile.getOriginalFilename());
        summary.put("size", multipartFile.getSize());
        return summary;
    }

    /**
     * 解析当前请求链路的 traceId。
     *
     * @param request 请求对象
     * @return traceId
     */
    private String resolveTraceId(HttpServletRequest request) {
        String contextTraceId = RequestTraceContextHolder.getTraceId();
        if (StringUtils.hasText(contextTraceId)) {
            return contextTraceId.trim();
        }
        if (request == null) {
            return NULL_VALUE;
        }
        String traceId = request.getHeader("X-Trace-Id");
        return StringUtils.hasText(traceId) ? traceId.trim() : NULL_VALUE;
    }

    /**
     * 解析当前租户编号。
     *
     * @param request 请求对象
     * @return 租户编号
     */
    private String resolveTenantId(HttpServletRequest request) {
        if (request != null) {
            String tenantId = request.getHeader("Tenantid");
            if (!StringUtils.hasText(tenantId)) {
                tenantId = request.getHeader("tenantId");
            }
            if (StringUtils.hasText(tenantId)) {
                return tenantId.trim();
            }
        }
        String contextTenantId = TenantContextHolder.getTenantId();
        return StringUtils.hasText(contextTenantId) ? contextTenantId.trim() : "UNKNOWN";
    }

    /**
     * 解析当前登录账号。
     *
     * @return 当前账号
     */
    private String resolveUsername() {
        String username = securityUserResolver.getCurrentUsername();
        return StringUtils.hasText(username) ? username.trim() : "anonymous";
    }

    /**
     * 解析客户端 IP。
     *
     * @param request 请求对象
     * @return 客户端 IP
     */
    private String resolveClientIp(HttpServletRequest request) {
        if (request == null) {
            return NULL_VALUE;
        }
        String forwardedFor = request.getHeader("X-Forwarded-For");
        if (StringUtils.hasText(forwardedFor)) {
            return forwardedFor.split(",")[0].trim();
        }
        return StringUtils.hasText(request.getRemoteAddr()) ? request.getRemoteAddr() : NULL_VALUE;
    }
}

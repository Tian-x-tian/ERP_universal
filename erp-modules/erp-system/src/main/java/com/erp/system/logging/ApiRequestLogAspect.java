package com.erp.system.logging;

import com.erp.common.core.context.RequestTraceContextHolder;
import com.erp.common.core.context.TenantContextHolder;
import com.erp.common.core.domain.R;
import com.erp.system.security.service.SecurityUserResolver;
import com.fasterxml.jackson.core.JsonProcessingException;
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

import java.lang.reflect.Array;
import java.time.temporal.Temporal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * 控制层接口请求日志切面。
 * 统一向终端输出请求入参、响应摘要、耗时与 traceId，便于联调排障。
 */
@Aspect
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 20)
public class ApiRequestLogAspect {
    private static final Logger log = LoggerFactory.getLogger(ApiRequestLogAspect.class);
    private static final String MASKED_VALUE = "******";
    private static final String NULL_VALUE = "null";
    private static final int MAX_LOG_LENGTH = 4000;
    private static final int MAX_COLLECTION_LOG_SIZE = 20;
    private static final int MAX_OBJECT_DEPTH = 4;
    private static final Set<String> SENSITIVE_FIELD_NAMES = new LinkedHashSet<>(Arrays.asList(
            "password",
            "oldpassword",
            "newpassword",
            "confirmpassword",
            "token",
            "accesstoken",
            "refreshtoken",
            "authorization",
            "secret",
            "clientsecret",
            "idtoken"
    ));

    private final ObjectMapper objectMapper;
    private final SecurityUserResolver securityUserResolver;

    public ApiRequestLogAspect(ObjectMapper objectMapper, SecurityUserResolver securityUserResolver) {
        this.objectMapper = objectMapper;
        this.securityUserResolver = securityUserResolver;
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
                    truncate(ex.getMessage()),
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
        payload.put("query", request == null ? null : request.getQueryString());
        payload.put("args", sanitizeValue(filterArguments(args), 0, null));
        return writeCompactJson(payload);
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
            payload.put("data", sanitizeValue(response.getData(), 0, null));
            return writeCompactJson(payload);
        }
        return writeCompactJson(sanitizeValue(result, 0, null));
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
     * 对任意对象执行脱敏与裁剪。
     *
     * @param value     原始对象
     * @param depth     当前递归深度
     * @param fieldName 当前字段名
     * @return 可日志化对象
     */
    @SuppressWarnings("unchecked")
    private Object sanitizeValue(Object value, int depth, String fieldName) {
        if (value == null) {
            return null;
        }
        if (isSensitiveField(fieldName)) {
            return MASKED_VALUE;
        }
        if (depth > MAX_OBJECT_DEPTH) {
            return "[depth-limited]";
        }
        if (value instanceof CharSequence) {
            return truncate(value.toString());
        }
        if (value instanceof Number || value instanceof Boolean || value instanceof Enum<?> || value instanceof Temporal || value instanceof Date) {
            return value;
        }
        if (value instanceof MultipartFile multipartFile) {
            return buildMultipartFileSummary(multipartFile);
        }
        if (value instanceof Map<?, ?> mapValue) {
            Map<String, Object> sanitized = new LinkedHashMap<>();
            int index = 0;
            for (Map.Entry<?, ?> entry : mapValue.entrySet()) {
                if (index >= MAX_COLLECTION_LOG_SIZE) {
                    sanitized.put("_truncated", "size>" + MAX_COLLECTION_LOG_SIZE);
                    break;
                }
                String key = entry.getKey() == null ? "null" : String.valueOf(entry.getKey());
                sanitized.put(key, sanitizeValue(entry.getValue(), depth + 1, key));
                index++;
            }
            return sanitized;
        }
        if (value instanceof Collection<?> collection) {
            List<Object> sanitized = new ArrayList<>();
            int index = 0;
            for (Object item : collection) {
                if (index >= MAX_COLLECTION_LOG_SIZE) {
                    sanitized.add("[truncated]");
                    break;
                }
                sanitized.add(sanitizeValue(item, depth + 1, fieldName));
                index++;
            }
            return sanitized;
        }
        if (value.getClass().isArray()) {
            int length = Array.getLength(value);
            List<Object> sanitized = new ArrayList<>();
            for (int i = 0; i < Math.min(length, MAX_COLLECTION_LOG_SIZE); i++) {
                sanitized.add(sanitizeValue(Array.get(value, i), depth + 1, fieldName));
            }
            if (length > MAX_COLLECTION_LOG_SIZE) {
                sanitized.add("[truncated]");
            }
            return sanitized;
        }
        try {
            Object converted = objectMapper.convertValue(value, Object.class);
            if (converted == value) {
                return truncate(String.valueOf(value));
            }
            return sanitizeValue(converted, depth + 1, fieldName);
        } catch (IllegalArgumentException ex) {
            return truncate(String.valueOf(value));
        }
    }

    /**
     * 判断字段名是否为敏感字段。
     *
     * @param fieldName 字段名
     * @return true 表示需要脱敏
     */
    private boolean isSensitiveField(String fieldName) {
        if (!StringUtils.hasText(fieldName)) {
            return false;
        }
        String normalizedFieldName = fieldName.replace("_", "").replace("-", "").toLowerCase(Locale.ROOT);
        return SENSITIVE_FIELD_NAMES.contains(normalizedFieldName);
    }

    /**
     * 将对象写为紧凑 JSON 文本。
     *
     * @param payload 日志对象
     * @return JSON 字符串
     */
    private String writeCompactJson(Object payload) {
        try {
            return truncate(objectMapper.writeValueAsString(payload));
        } catch (JsonProcessingException ex) {
            return truncate(String.valueOf(payload));
        }
    }

    /**
     * 裁剪过长日志内容，避免刷屏。
     *
     * @param text 原始文本
     * @return 裁剪后文本
     */
    private String truncate(String text) {
        if (!StringUtils.hasText(text)) {
            return NULL_VALUE;
        }
        return text.length() <= MAX_LOG_LENGTH ? text : text.substring(0, MAX_LOG_LENGTH) + "...[truncated]";
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

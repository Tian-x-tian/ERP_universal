package com.erp.system.audit;

import com.erp.common.core.context.TenantContextHolder;
import com.erp.common.core.domain.R;
import com.erp.system.domain.SysAuditLog;
import com.erp.system.security.service.SecurityUserResolver;
import com.erp.system.service.ISysAuditLogService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.multipart.MultipartFile;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * 审计日志切面
 */
@Aspect
@Component
public class AuditLogAspect {
    private static final String UNKNOWN_TENANT_ID = "UNKNOWN";


    private final ISysAuditLogService auditLogService;
    private final SecurityUserResolver securityUserResolver;
    private final ObjectMapper objectMapper;

    public AuditLogAspect(ISysAuditLogService auditLogService,
            SecurityUserResolver securityUserResolver,
            ObjectMapper objectMapper) {
        this.auditLogService = auditLogService;
        this.securityUserResolver = securityUserResolver;
        this.objectMapper = objectMapper;
    }

    /**
     * 记录控制层请求审计日志。
     *
     * @param joinPoint 切点
     * @return 方法执行结果
     * @throws Throwable 方法原始异常
     */
    @Around("execution(* com.erp.system.controller..*(..))")
    public Object aroundController(ProceedingJoinPoint joinPoint) throws Throwable {
        HttpServletRequest request = getCurrentRequest();
        if (request == null) {
            return joinPoint.proceed();
        }
        if (!needRecord(request)) {
            return joinPoint.proceed();
        }

        long startTime = System.currentTimeMillis();
        SysAuditLog log = buildBaseLog(request, joinPoint.getArgs());
        try {
            Object result = joinPoint.proceed();
            fillSuccess(log, result, System.currentTimeMillis() - startTime);
            return result;
        } catch (Throwable ex) {
            fillFailure(log, ex, System.currentTimeMillis() - startTime);
            throw ex;
        } finally {
            auditLogService.save(log);
        }
    }

    /**
     * 判断是否记录审计日志。
     * 审计日志仅记录查询场景，写操作由操作日志切面单独采集。
     *
     * @param request HTTP 请求
     * @return true 表示需要记录
     */
    private boolean needRecord(HttpServletRequest request) {
        if (!"GET".equalsIgnoreCase(request.getMethod())) {
            return false;
        }
        String requestUri = request.getRequestURI();
        return !"/login".equals(requestUri)
                && !"/logout".equals(requestUri)
                && !"/internal/auth/login".equals(requestUri)
                && !"/internal/auth/logout".equals(requestUri)
                && !requestUri.startsWith("/system/audit/log")
                && !requestUri.startsWith("/system/login/log")
                && !requestUri.startsWith("/system/mdm/trace")
                && !requestUri.startsWith("/system/oper/log");
    }

    /**
     * 构建审计日志基础字段。
     *
     * @param request HTTP 请求
     * @param args    方法参数
     * @return 审计日志对象
     */
    private SysAuditLog buildBaseLog(HttpServletRequest request, Object[] args) {
        SysAuditLog log = new SysAuditLog();
        log.setTenantId(resolveTenantId(request));
        log.setOperator(securityUserResolver.getCurrentUsername());
        log.setOperationType(request.getMethod());
        log.setRequestMethod(request.getMethod());
        log.setRequestUri(request.getRequestURI());
        log.setRequestIp(getClientIp(request));
        log.setRequestParams(serializeArgs(args, request));
        log.setOperationTime(new Date());
        return log;
    }

    /**
     * 填充成功执行结果。
     *
     * @param log      审计日志对象
     * @param result   方法结果
     * @param costTime 执行耗时
     */
    private void fillSuccess(SysAuditLog log, Object result, long costTime) {
        log.setSuccessFlag("1");
        log.setCostTime(costTime);
        if (result instanceof R) {
            log.setResponseCode((int) ((R<?>) result).getCode());
        } else {
            log.setResponseCode(200);
        }
    }

    /**
     * 填充失败执行结果。
     *
     * @param log      审计日志对象
     * @param ex       异常信息
     * @param costTime 执行耗时
     */
    private void fillFailure(SysAuditLog log, Throwable ex, long costTime) {
        log.setSuccessFlag("0");
        log.setCostTime(costTime);
        log.setResponseCode(500);
        String errorMsg = ex.getMessage();
        if (errorMsg != null && errorMsg.length() > 500) {
            errorMsg = errorMsg.substring(0, 500);
        }
        log.setErrorMsg(errorMsg);
    }

    /**
     * 获取当前请求对象。
     *
     * @return 请求对象
     */
    private HttpServletRequest getCurrentRequest() {
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        return attributes == null ? null : attributes.getRequest();
    }

    /**
     * 解析租户ID。
     *
     * @param request HTTP 请求
     * @return 租户ID
     */
    private String resolveTenantId(HttpServletRequest request) {
        String tenantId = request.getHeader("Tenantid");
        if (tenantId == null) {
            tenantId = request.getHeader("tenantId");
        }
        if (tenantId == null) {
            tenantId = TenantContextHolder.getTenantId();
        }
        return tenantId == null ? UNKNOWN_TENANT_ID : tenantId;
    }

    /**
     * 获取客户端 IP。
     *
     * @param request HTTP 请求
     * @return 客户端 IP
     */
    private String getClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty()) {
            ip = request.getRemoteAddr();
        }
        return ip;
    }

    /**
     * 序列化请求参数。
     *
     * @param args    方法参数
     * @param request HTTP 请求
     * @return 参数 JSON
     */
    private String serializeArgs(Object[] args, HttpServletRequest request) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("query", request.getQueryString());
        payload.put("args", filterSerializableArgs(args));
        try {
            String json = objectMapper.writeValueAsString(payload);
            return json.length() > 2000 ? json.substring(0, 2000) : json;
        } catch (JsonProcessingException e) {
            return "{\"error\":\"request-args-serialize-failed\"}";
        }
    }

    /**
     * 过滤不可序列化参数。
     *
     * @param args 原始参数
     * @return 可序列化参数数组
     */
    private Object[] filterSerializableArgs(Object[] args) {
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
}

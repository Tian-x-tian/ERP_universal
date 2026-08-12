package com.erp.system.audit;

import com.erp.common.logging.AuditLogAspectSupport;
import com.erp.common.logging.OperationLogRecorder;
import com.erp.system.security.service.SecurityUserResolver;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;

import java.util.Set;

/**
 * 系统模块查询审计日志切面。
 */
@Aspect
@Component
public class AuditLogAspect extends AuditLogAspectSupport {
    private static final Set<String> EXCLUDED_URI_PREFIXES = Set.of(
            "/system/audit/log",
            "/system/login/log",
            "/system/mdm/trace",
            "/system/oper/log",
            // 服务间内部调用不是用户查询行为，不进审计日志
            "/system/internal");

    private final SecurityUserResolver securityUserResolver;

    public AuditLogAspect(OperationLogRecorder operationLogRecorder,
            SecurityUserResolver securityUserResolver,
            ObjectMapper objectMapper) {
        super(operationLogRecorder, objectMapper);
        this.securityUserResolver = securityUserResolver;
    }

    /**
     * 记录控制层查询请求审计日志。
     *
     * @param joinPoint 切点
     * @return 方法执行结果
     * @throws Throwable 方法原始异常
     */
    @Around("execution(* com.erp.system.controller..*(..))")
    public Object aroundController(ProceedingJoinPoint joinPoint) throws Throwable {
        return recordAround(joinPoint);
    }

    /**
     * 日志查询接口自身不再记录审计日志。
     *
     * @return 排除的路径前缀
     */
    @Override
    protected Set<String> excludedUriPrefixes() {
        return EXCLUDED_URI_PREFIXES;
    }

    /**
     * 解析当前操作人账号。
     *
     * @return 当前登录用户名
     */
    @Override
    protected String resolveOperator() {
        return securityUserResolver.getCurrentUsername();
    }
}

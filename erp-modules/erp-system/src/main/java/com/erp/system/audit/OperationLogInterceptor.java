package com.erp.system.audit;

import com.erp.common.logging.OperationLogInterceptorSupport;
import com.erp.common.logging.OperationLogRecorder;
import com.erp.system.security.service.SecurityUserResolver;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

import java.util.Set;

/**
 * 系统模块操作日志拦截器。
 */
@Component
public class OperationLogInterceptor extends OperationLogInterceptorSupport {
    private static final Set<String> EXCLUDED_URI_PREFIXES = Set.of(
            "/system/login/log",
            "/system/audit/log",
            "/system/oper/log",
            // 服务间内部调用不是用户操作；尤其 /system/internal/platform/oper-log 本身就是
            // 其他服务回传日志的入口，记录它会造成日志自我放大。
            "/system/internal");

    private final SecurityUserResolver securityUserResolver;

    public OperationLogInterceptor(OperationLogRecorder operationLogRecorder,
            SecurityUserResolver securityUserResolver,
            ObjectMapper objectMapper) {
        super(operationLogRecorder, objectMapper);
        this.securityUserResolver = securityUserResolver;
    }

    /**
     * 日志查询接口自身不再记录操作日志。
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

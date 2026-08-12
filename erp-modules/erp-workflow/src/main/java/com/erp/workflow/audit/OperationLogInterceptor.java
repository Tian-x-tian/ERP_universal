package com.erp.workflow.audit;

import com.erp.common.logging.OperationLogInterceptorSupport;
import com.erp.common.logging.OperationLogRecorder;
import com.erp.workflow.security.service.SecurityUserResolver;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

import java.util.Set;

/**
 * 审批流模块操作日志拦截器。
 */
@Component
public class OperationLogInterceptor extends OperationLogInterceptorSupport {
    private static final Set<String> EXCLUDED_URI_PREFIXES = Set.of("/workflow/internal");

    private final SecurityUserResolver securityUserResolver;

    public OperationLogInterceptor(OperationLogRecorder operationLogRecorder,
            SecurityUserResolver securityUserResolver,
            ObjectMapper objectMapper) {
        super(operationLogRecorder, objectMapper);
        this.securityUserResolver = securityUserResolver;
    }

    /**
     * 服务间内部调用不记录操作日志。
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

package com.erp.workflow.security.filter;

import com.erp.common.security.servlet.InternalAuthenticationFilterSupport;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * JWT 认证过滤器
 */
@Component
public class JwtAuthenticationFilter extends InternalAuthenticationFilterSupport {
    private static final String PUBLIC_ACTIVE_TENANTS_PATH = "/workflow/public/tenants/active";

    public JwtAuthenticationFilter(ObjectMapper objectMapper,
            @Value("${erp.internal.auth-signature-secret:}") String internalSignatureSecret) {
        super(objectMapper, internalSignatureSecret, "/workflow/", "工作流模块");
    }

    /**
     * 判断当前请求是否需要内部签名认证。
     *
     * @param request 请求对象
     * @return true 表示需要认证
     */
    @Override
    protected boolean requiresAuth(HttpServletRequest request) {
        String requestUri = request == null ? null : request.getRequestURI();
        if (PUBLIC_ACTIVE_TENANTS_PATH.equals(requestUri)) {
            return false;
        }
        return super.requiresAuth(request);
    }
}


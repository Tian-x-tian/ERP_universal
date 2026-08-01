package com.erp.saas.control.security;

import com.erp.common.security.servlet.InternalAuthenticationFilterSupport;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class SaasAuthenticationFilter extends InternalAuthenticationFilterSupport {

    public SaasAuthenticationFilter(ObjectMapper objectMapper,
            @Value("${erp.internal.auth-signature-secret:}") String internalSignatureSecret) {
        super(objectMapper, internalSignatureSecret, "/__saas_control__", "SaaS control plane");
    }

    @Override
    protected boolean requiresAuth(HttpServletRequest request) {
        if (request == null) {
            return false;
        }
        String requestUri = request.getRequestURI();
        if (!StringUtils.hasText(requestUri)) {
            return false;
        }
        String contextPath = request.getContextPath();
        String path = requestUri;
        if (StringUtils.hasText(contextPath) && requestUri.startsWith(contextPath)) {
            path = requestUri.substring(contextPath.length());
        }
        return matchesGroup(path, "/saas") || matchesGroup(path, "/internal/saas");
    }

    private boolean matchesGroup(String path, String group) {
        return group.equals(path) || path.startsWith(group + "/");
    }
}

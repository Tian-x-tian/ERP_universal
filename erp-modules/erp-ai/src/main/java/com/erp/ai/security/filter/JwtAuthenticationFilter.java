package com.erp.ai.security.filter;

import com.erp.common.security.servlet.InternalAuthenticationFilterSupport;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * JWT 认证过滤器
 */
@Component
public class JwtAuthenticationFilter extends InternalAuthenticationFilterSupport {

    public JwtAuthenticationFilter(ObjectMapper objectMapper,
            @Value("${erp.internal.auth-signature-secret:erp-internal-auth-signature-2026}") String internalSignatureSecret) {
        super(objectMapper, internalSignatureSecret, "/system/ai/", "AI模块");
    }
}

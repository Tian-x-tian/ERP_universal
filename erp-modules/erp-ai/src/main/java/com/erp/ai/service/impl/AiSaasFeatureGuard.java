package com.erp.ai.service.impl;

import com.erp.common.client.internal.InternalSystemClient;
import com.erp.common.core.context.TenantContextHolder;
import com.erp.saas.contract.model.SaasFeatureKeys;
import com.erp.saas.contract.model.SaasRuntimeAccess;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class AiSaasFeatureGuard {
    private final InternalSystemClient systemClient;

    public AiSaasFeatureGuard(InternalSystemClient systemClient) {
        this.systemClient = systemClient;
    }

    public void requirePaidAccess() {
        String tenantId = TenantContextHolder.getTenantId();
        if (!StringUtils.hasText(tenantId)) {
            throw denied();
        }
        SaasRuntimeAccess access = systemClient.getSaasRuntimeAccess();
        if (access == null || !tenantId.trim().equals(access.getTenantId())
                || !access.isWriteAllowed() || !access.isFeatureEnabled(SaasFeatureKeys.AI_ASSISTANT)) {
            throw denied();
        }
    }

    private AccessDeniedException denied() {
        return new AccessDeniedException("AI assistant is not available for the current tenant");
    }
}

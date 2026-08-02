package com.erp.saas.control.security;

import com.erp.common.client.internal.InternalSystemClient;
import com.erp.common.security.AuthenticatedUserPrincipal;
import com.erp.platform.contract.model.PlatformAuthorityBundle;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class PlatformSaasAdminGuard {
    private static final String PLATFORM_TENANT = "000000";
    private static final String ADMIN_ROLE = "admin";

    private final InternalSystemClient systemClient;

    public PlatformSaasAdminGuard(InternalSystemClient systemClient) {
        this.systemClient = systemClient;
    }

    public String requireAdmin(Authentication authentication) {
        if (authentication == null
                || !(authentication.getPrincipal() instanceof AuthenticatedUserPrincipal principal)
                || !PLATFORM_TENANT.equals(principal.getTenantId())) {
            throw denied();
        }
        PlatformAuthorityBundle authorities = systemClient.getAuthorities();
        List<String> roleKeys = authorities == null ? null : authorities.getRoleKeys();
        if (roleKeys == null || roleKeys.stream().noneMatch(ADMIN_ROLE::equals)) {
            throw denied();
        }
        return principal.getUserName();
    }

    private AccessDeniedException denied() {
        return new AccessDeniedException("Platform SaaS administration requires the admin role");
    }
}

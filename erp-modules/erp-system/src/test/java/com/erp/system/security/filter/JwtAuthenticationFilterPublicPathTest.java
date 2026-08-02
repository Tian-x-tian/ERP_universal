package com.erp.system.security.filter;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;

import static org.assertj.core.api.Assertions.assertThat;

class JwtAuthenticationFilterPublicPathTest {
    @Test
    void shouldAllowTenantActivationWithoutAuthenticatedUserPrincipal() {
        JwtAuthenticationFilter filter = new JwtAuthenticationFilter(new ObjectMapper(), "internal-secret");
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/system/public/saas/activation");

        assertThat(filter.requiresAuth(request)).isFalse();
    }
}

package com.erp.system.controller;

import com.erp.common.core.domain.ResultCode;
import com.erp.common.security.ResolvedTenantAssertion;
import com.erp.system.domain.vo.SaasUserActivationRequest;
import com.erp.system.saas.SaasUserActivationService;
import com.erp.system.security.SaasTenantAssertionVerifier;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class SystemSaasPublicControllerTest {
    @Test
    void shouldActivateUsingTenantFromSignedAssertion() {
        SaasTenantAssertionVerifier verifier = mock(SaasTenantAssertionVerifier.class);
        SaasUserActivationService service = mock(SaasUserActivationService.class);
        SystemSaasPublicController controller = new SystemSaasPublicController(verifier, service);
        MockHttpServletRequest servletRequest = new MockHttpServletRequest(
                "POST", "/system/public/saas/activation");
        SaasUserActivationRequest body = new SaasUserActivationRequest();
        ResolvedTenantAssertion assertion = new ResolvedTenantAssertion(
                "tenant-a", "acme.example", "POST", "/system/public/saas/activation", 1L, "nonce");
        when(verifier.verify(servletRequest)).thenReturn(assertion);

        var response = controller.activate(body, servletRequest);

        assertThat(response.getCode()).isEqualTo(ResultCode.SUCCESS.getCode());
        verify(service).activate("tenant-a", body);
    }

    @Test
    void shouldRejectMissingSignedAssertion() {
        SaasTenantAssertionVerifier verifier = mock(SaasTenantAssertionVerifier.class);
        SaasUserActivationService service = mock(SaasUserActivationService.class);
        SystemSaasPublicController controller = new SystemSaasPublicController(verifier, service);
        MockHttpServletRequest servletRequest = new MockHttpServletRequest();
        when(verifier.verify(servletRequest)).thenThrow(new IllegalArgumentException("invalid"));

        var response = controller.activate(new SaasUserActivationRequest(), servletRequest);

        assertThat(response.getCode()).isEqualTo(ResultCode.UNAUTHORIZED.getCode());
    }
}

package com.erp.system.controller;

import com.erp.common.core.domain.R;
import com.erp.common.core.domain.ResultCode;
import com.erp.common.security.ResolvedTenantAssertion;
import com.erp.system.domain.vo.SaasUserActivationRequest;
import com.erp.system.saas.SaasUserActivationService;
import com.erp.system.security.SaasTenantAssertionVerifier;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/system/public/saas")
public class SystemSaasPublicController {
    private final SaasTenantAssertionVerifier assertionVerifier;
    private final SaasUserActivationService activationService;

    public SystemSaasPublicController(SaasTenantAssertionVerifier assertionVerifier,
            SaasUserActivationService activationService) {
        this.assertionVerifier = assertionVerifier;
        this.activationService = activationService;
    }

    @PostMapping("/activation")
    public R<Void> activate(@RequestBody SaasUserActivationRequest body, HttpServletRequest request) {
        final ResolvedTenantAssertion assertion;
        try {
            assertion = assertionVerifier.verify(request);
        } catch (IllegalArgumentException error) {
            return R.failed(ResultCode.UNAUTHORIZED, "租户上下文无效或已过期");
        }
        activationService.activate(assertion.getTenantId(), body);
        return R.success();
    }
}

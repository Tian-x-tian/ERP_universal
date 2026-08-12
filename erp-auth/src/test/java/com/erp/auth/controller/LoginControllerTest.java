package com.erp.auth.controller;

import com.erp.auth.domain.SysUser;
import com.erp.auth.domain.vo.LoginBody;
import com.erp.auth.security.CaptchaVerifier;
import com.erp.auth.security.LoginGuardService;
import com.erp.auth.security.ResolvedTenantAssertionException;
import com.erp.auth.security.ResolvedTenantAssertionVerifier;
import com.erp.auth.service.AuthAccountService;
import com.erp.auth.service.AuthLoginLogService;
import com.erp.auth.service.AuthTokenService;
import com.erp.common.core.domain.R;
import com.erp.common.core.domain.ResultCode;
import com.erp.common.security.ResolvedTenantAssertion;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class LoginControllerTest {
    private AuthAccountService accountService;
    private AuthLoginLogService loginLogService;
    private PasswordEncoder passwordEncoder;
    private LoginGuardService loginGuardService;
    private CaptchaVerifier captchaVerifier;
    private AuthTokenService tokenService;
    private ResolvedTenantAssertionVerifier assertionVerifier;
    private LoginController controller;

    @BeforeEach
    void setUp() {
        accountService = mock(AuthAccountService.class);
        loginLogService = mock(AuthLoginLogService.class);
        passwordEncoder = mock(PasswordEncoder.class);
        loginGuardService = mock(LoginGuardService.class);
        captchaVerifier = mock(CaptchaVerifier.class);
        tokenService = mock(AuthTokenService.class);
        assertionVerifier = mock(ResolvedTenantAssertionVerifier.class);
        controller = new LoginController(accountService, loginLogService, passwordEncoder,
                loginGuardService, captchaVerifier, tokenService, assertionVerifier);
    }

    @Test
    void shouldUseSignedTenantAssertionInsteadOfClientTenantHeader() {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/login");
        request.addHeader("tenantId", "forged");
        request.setRemoteAddr("127.0.0.1");
        when(assertionVerifier.verify(request)).thenReturn(assertion());
        when(loginGuardService.allowIpAttempt("127.0.0.1")).thenReturn(true);
        when(captchaVerifier.verify(any())).thenReturn(true);
        SysUser user = user();
        when(accountService.selectUserByUserNameAndTenant("admin", "tenant-a")).thenReturn(user);
        when(passwordEncoder.matches("secret", "encoded")).thenReturn(true);
        when(tokenService.createToken(user)).thenReturn("token");

        R<Map<String, Object>> result = controller.login(loginBody(), request);

        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getData()).containsEntry("tenantId", "tenant-a");
        verify(accountService).selectUserByUserNameAndTenant("admin", "tenant-a");
        verify(accountService, never()).selectUserByUserNameAndTenant("admin", "forged");
    }

    @Test
    void shouldRejectLoginBeforeAccountLookupWhenAssertionIsInvalid() {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/login");
        request.setRemoteAddr("127.0.0.1");
        when(loginGuardService.allowIpAttempt("127.0.0.1")).thenReturn(true);
        when(assertionVerifier.verify(request)).thenThrow(
                new ResolvedTenantAssertionException("租户上下文无效或已过期"));

        R<Map<String, Object>> result = controller.login(loginBody(), request);

        assertThat(result.getCode()).isEqualTo(ResultCode.UNAUTHORIZED.getCode());
        verify(accountService, never()).selectUserByUserNameAndTenant(any(), any());
        verify(loginLogService).record(eq(null), eq("admin"), eq("1"),
                eq("租户上下文无效或已过期"), eq("127.0.0.1"));
    }

    private LoginBody loginBody() {
        LoginBody body = new LoginBody();
        body.setUsername("admin");
        body.setPassword("secret");
        return body;
    }

    private SysUser user() {
        SysUser user = new SysUser();
        user.setUserId(1L);
        user.setTenantId("tenant-a");
        user.setUserName("admin");
        user.setPassword("encoded");
        user.setStatus("0");
        user.setDelFlag("0");
        user.setTokenVersion(0);
        return user;
    }

    private ResolvedTenantAssertion assertion() {
        return new ResolvedTenantAssertion("tenant-a", "acme.example", "POST", "/login",
                1L, "nonce");
    }
}

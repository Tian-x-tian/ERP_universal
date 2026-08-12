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
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * 认证登录/登出入口。
 */
@RestController
public class LoginController {
    private final AuthAccountService accountService;
    private final AuthLoginLogService loginLogService;
    private final PasswordEncoder passwordEncoder;
    private final LoginGuardService loginGuardService;
    private final CaptchaVerifier captchaVerifier;
    private final AuthTokenService authTokenService;
    private final ResolvedTenantAssertionVerifier tenantAssertionVerifier;

    public LoginController(AuthAccountService accountService,
            AuthLoginLogService loginLogService,
            PasswordEncoder passwordEncoder,
            LoginGuardService loginGuardService,
            CaptchaVerifier captchaVerifier,
            AuthTokenService authTokenService,
            ResolvedTenantAssertionVerifier tenantAssertionVerifier) {
        this.accountService = accountService;
        this.loginLogService = loginLogService;
        this.passwordEncoder = passwordEncoder;
        this.loginGuardService = loginGuardService;
        this.captchaVerifier = captchaVerifier;
        this.authTokenService = authTokenService;
        this.tenantAssertionVerifier = tenantAssertionVerifier;
    }

    /**
     * 登录入口（auth 本地校验并签发 token）。
     */
    @PostMapping("/login")
    public R<Map<String, Object>> login(@RequestBody LoginBody loginBody, HttpServletRequest request) {
        String username = loginBody == null ? null : trim(loginBody.getUsername());
        String requestIp = resolveRequestIp(request);

        if (!loginGuardService.allowIpAttempt(requestIp)) {
            loginLogService.record(null, username, "1", "登录请求过于频繁，请稍后再试", requestIp);
            return R.failed("登录请求过于频繁，请稍后再试");
        }

        final String loginTenantId;
        try {
            loginTenantId = tenantAssertionVerifier.verify(request).getTenantId();
        } catch (ResolvedTenantAssertionException error) {
            loginLogService.record(null, username, "1", "租户上下文无效或已过期", requestIp);
            return R.failed(ResultCode.UNAUTHORIZED, "租户上下文无效或已过期");
        }
        if (loginBody == null || !StringUtils.hasText(loginBody.getUsername())
                || !StringUtils.hasText(loginBody.getPassword())) {
            loginLogService.record(loginTenantId, username, "1", "用户名或密码不能为空", requestIp);
            return R.failed("用户名或密码不能为空");
        }

        if (loginGuardService.isAccountLocked(loginTenantId, username)) {
            long leftSeconds = loginGuardService.lockedSecondsLeft(loginTenantId, username);
            String message = "账号已临时锁定，请 " + leftSeconds + " 秒后重试";
            loginLogService.record(loginTenantId, username, "1", message, requestIp);
            return R.failed(message);
        }

        if (!captchaVerifier.verify(loginBody)) {
            loginGuardService.onLoginFailed(loginTenantId, username);
            loginLogService.record(loginTenantId, username, "1", "验证码错误", requestIp);
            return R.failed("验证码错误");
        }

        SysUser user = accountService.selectUserByUserNameAndTenant(username, loginTenantId);
        if (user == null || !passwordEncoder.matches(loginBody.getPassword(), user.getPassword())) {
            loginGuardService.onLoginFailed(loginTenantId, username);
            loginLogService.record(loginTenantId, username, "1", "用户名或密码错误", requestIp);
            return R.failed("用户名或密码错误");
        }
        if (!"0".equals(user.getStatus()) || "2".equals(user.getDelFlag())) {
            loginGuardService.onLoginFailed(loginTenantId, username);
            loginLogService.record(loginTenantId, username, "1", "账号不可用", requestIp);
            return R.failed("账号不可用");
        }

        String tenantId = user.getTenantId();
        String token = authTokenService.createToken(user);
        accountService.updateLoginInfo(user.getUserId(), requestIp, new Date());
        loginLogService.record(tenantId, user.getUserName(), "0", "登录成功", requestIp);
        loginGuardService.onLoginSuccess(tenantId, user.getUserName());

        Map<String, Object> data = new HashMap<>();
        data.put("token", token);
        data.put("tenantId", tenantId);
        return R.success(data);
    }

    /**
     * 登出入口（auth 本地失效 tokenVersion）。
     */
    @PostMapping("/logout")
    public R<Void> logout(HttpServletRequest request) {
        final String tenantId;
        try {
            tenantId = tenantAssertionVerifier.verify(request).getTenantId();
        } catch (ResolvedTenantAssertionException error) {
            return R.failed(ResultCode.UNAUTHORIZED, "租户上下文无效或已过期");
        }
        String authorization = request == null ? null : request.getHeader("Authorization");
        Long userId = resolveUserIdFromAuthorization(authorization, tenantId);

        if (StringUtils.hasText(tenantId) && userId != null) {
            SysUser user = accountService.selectUserByIdAndTenant(userId, tenantId);
            if (user != null) {
                accountService.incrementTokenVersion(user.getUserId());
            }
        }
        return R.success();
    }

    private Long resolveUserIdFromAuthorization(String authorization, String tenantId) {
        if (!StringUtils.hasText(authorization) || !authorization.startsWith("Bearer ")) {
            return null;
        }
        String token = authorization.substring(7).trim();
        if (!StringUtils.hasText(token)) {
            return null;
        }
        try {
            return authTokenService.verifyToken(token, tenantId).getUserId();
        } catch (Exception ex) {
            return null;
        }
    }

    private String resolveRequestIp(HttpServletRequest request) {
        if (request == null) {
            return "unknown";
        }
        String forwardedFor = request.getHeader("X-Forwarded-For");
        if (StringUtils.hasText(forwardedFor)) {
            return forwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }

    private String trim(String value) {
        return value == null ? null : value.trim();
    }
}

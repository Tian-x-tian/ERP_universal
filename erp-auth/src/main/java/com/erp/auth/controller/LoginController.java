package com.erp.auth.controller;

import com.erp.auth.domain.vo.LoginBody;
import com.erp.common.core.domain.R;
import com.erp.common.core.domain.ResultCode;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;

/**
 * 认证登录/登出入口（标准化迁移到 erp-auth）。
 */
@RestController
public class LoginController {
    private final RestTemplate restTemplate;

    public LoginController(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    /**
     * 登录入口，转发到 erp-system 执行账号校验与令牌签发。
     */
    @PostMapping("/login")
    public R<?> login(@RequestBody LoginBody loginBody, HttpServletRequest request) {
        HttpHeaders headers = buildHeaders(request, false);
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<LoginBody> entity = new HttpEntity<>(loginBody, headers);
        try {
            ResponseEntity<R> response = restTemplate.exchange(
                    "http://erp-system/internal/auth/login",
                    HttpMethod.POST,
                    entity,
                    R.class);
            return response.getBody() == null ? R.failed(ResultCode.ERROR, "登录服务无响应") : response.getBody();
        } catch (HttpStatusCodeException ex) {
            return R.failed(ResultCode.ERROR, "登录服务调用失败: " + ex.getStatusCode().value());
        } catch (Exception ex) {
            return R.failed(ResultCode.ERROR, "登录服务异常: " + ex.getMessage());
        }
    }

    /**
     * 登出入口，转发到 erp-system 失效令牌版本。
     */
    @PostMapping("/logout")
    public R<?> logout(HttpServletRequest request) {
        HttpHeaders headers = buildHeaders(request, true);
        HttpEntity<Void> entity = new HttpEntity<>(headers);
        try {
            ResponseEntity<R> response = restTemplate.exchange(
                    "http://erp-system/internal/auth/logout",
                    HttpMethod.POST,
                    entity,
                    R.class);
            return response.getBody() == null ? R.success() : response.getBody();
        } catch (HttpStatusCodeException ex) {
            return R.failed(ResultCode.ERROR, "登出服务调用失败: " + ex.getStatusCode().value());
        } catch (Exception ex) {
            return R.failed(ResultCode.ERROR, "登出服务异常: " + ex.getMessage());
        }
    }

    private HttpHeaders buildHeaders(HttpServletRequest request, boolean includeAuthorization) {
        HttpHeaders headers = new HttpHeaders();
        String tenantId = resolveTenantId(request);
        if (StringUtils.hasText(tenantId)) {
            headers.set("tenantId", tenantId);
        }
        if (includeAuthorization) {
            String authorization = request == null ? null : request.getHeader("Authorization");
            if (StringUtils.hasText(authorization)) {
                headers.set("Authorization", authorization);
            }
        }
        return headers;
    }

    private String resolveTenantId(HttpServletRequest request) {
        if (request == null) {
            return null;
        }
        String tenantId = request.getHeader("tenantId");
        if (!StringUtils.hasText(tenantId)) {
            tenantId = request.getHeader("Tenantid");
        }
        if (!StringUtils.hasText(tenantId)) {
            return null;
        }
        return tenantId.trim();
    }
}

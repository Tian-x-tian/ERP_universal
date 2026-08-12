package com.erp.auth.controller;

import com.erp.auth.domain.vo.AuthTokenVerifyResult;
import com.erp.auth.service.AuthTokenService;
import com.erp.common.core.domain.R;
import com.erp.common.core.domain.ResultCode;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

/**
 * 认证中心基础接口。
 */
@RestController
@RequestMapping("/auth")
public class AuthController {
    private final AuthTokenService authTokenService;

    public AuthController(AuthTokenService authTokenService) {
        this.authTokenService = authTokenService;
    }

    /**
     * 健康探针。
     *
     * @return 服务状态
     */
    @GetMapping("/ping")
    public R<Map<String, Object>> ping() {
        Map<String, Object> data = new HashMap<>();
        data.put("service", "erp-auth");
        data.put("status", "up");
        return R.success(data);
    }

    /**
     * 令牌解析与校验。
     *
     * @param authorization Authorization 请求头（Bearer Token）
     * @return 校验结果
     */
    @GetMapping("/token/verify")
    public R<Map<String, Object>> verifyToken(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @RequestHeader(value = "tenantId", required = false) String tenantId) {
        String token = parseBearerToken(authorization);
        if (!StringUtils.hasText(token)) {
            return R.failed(ResultCode.UNAUTHORIZED, "缺少Bearer Token");
        }

        try {
            AuthTokenVerifyResult verifyResult = authTokenService.verifyToken(token, tenantId);
            Map<String, Object> data = new HashMap<>();
            data.put("valid", true);
            data.put("userId", verifyResult.getUserId());
            data.put("userName", verifyResult.getUserName());
            data.put("tenantId", verifyResult.getTenantId());
            data.put("tokenVersion", verifyResult.getTokenVersion());
            data.put("expiresAt", verifyResult.getExpiresAt());
            return R.success(data);
        } catch (Exception ex) {
            return R.failed(ResultCode.UNAUTHORIZED,
                    StringUtils.hasText(ex.getMessage()) ? ex.getMessage() : "Token无效或已过期");
        }
    }

    /**
     * 提取 Bearer Token。
     *
     * @param authorization Authorization 请求头
     * @return token 字符串
     */
    private String parseBearerToken(String authorization) {
        if (!StringUtils.hasText(authorization)) {
            return null;
        }
        String prefix = "Bearer ";
        if (!authorization.startsWith(prefix)) {
            return null;
        }
        String token = authorization.substring(prefix.length()).trim();
        return StringUtils.hasText(token) ? token : null;
    }
}

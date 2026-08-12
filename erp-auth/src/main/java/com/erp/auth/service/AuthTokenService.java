package com.erp.auth.service;

import com.erp.auth.domain.SysUser;
import com.erp.auth.domain.vo.AuthTokenVerifyResult;

/**
 * 认证中心 Token 服务。
 */
public interface AuthTokenService {

    /**
     * 为指定用户签发访问令牌。
     *
     * @param user 用户对象
     * @return JWT 字符串
     */
    String createToken(SysUser user);

    /**
     * 校验访问令牌并返回权威用户上下文。
     *
     * @param token              Bearer Token 去前缀后的原文
     * @param requestedTenantId  调用方请求头中的租户编号
     * @return 校验通过后的用户上下文
     */
    AuthTokenVerifyResult verifyToken(String token, String requestedTenantId);
}

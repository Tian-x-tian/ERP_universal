package com.erp.common.security;

import java.io.Serializable;

/**
 * 统一的已认证用户主体。
 */
public class AuthenticatedUserPrincipal implements Serializable {
    private static final long serialVersionUID = 1L;

    private final Long userId;
    private final String userName;
    private final String tenantId;
    private final Integer tokenVersion;
    private final Long expiresAt;

    /**
     * 创建已认证用户主体。
     *
     * @param userId       用户ID
     * @param userName     用户账号
     * @param tenantId     租户编号
     * @param tokenVersion Token 版本号
     * @param expiresAt    过期时间戳（毫秒）
     */
    public AuthenticatedUserPrincipal(Long userId, String userName, String tenantId, Integer tokenVersion,
            Long expiresAt) {
        this.userId = userId;
        this.userName = userName;
        this.tenantId = tenantId;
        this.tokenVersion = tokenVersion;
        this.expiresAt = expiresAt;
    }

    public Long getUserId() {
        return userId;
    }

    public String getUserName() {
        return userName;
    }

    public String getTenantId() {
        return tenantId;
    }

    public Integer getTokenVersion() {
        return tokenVersion;
    }

    public Long getExpiresAt() {
        return expiresAt;
    }
}

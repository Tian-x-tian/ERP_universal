package com.erp.auth.domain.vo;

/**
 * 认证中心 Token 校验结果。
 */
public class AuthTokenVerifyResult {
    private final Long userId;
    private final String userName;
    private final String tenantId;
    private final Integer tokenVersion;
    private final Long expiresAt;

    /**
     * 创建 Token 校验结果。
     *
     * @param userId       用户ID
     * @param userName     用户账号
     * @param tenantId     租户编号
     * @param tokenVersion Token 版本号
     * @param expiresAt    过期时间戳（毫秒）
     */
    public AuthTokenVerifyResult(Long userId, String userName, String tenantId, Integer tokenVersion, Long expiresAt) {
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

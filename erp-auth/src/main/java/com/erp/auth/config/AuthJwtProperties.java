package com.erp.auth.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 认证中心 JWT 配置。
 */
@ConfigurationProperties(prefix = "security.jwt")
public class AuthJwtProperties {
    private String secret;
    private long expire = 86400000L;

    public String getSecret() {
        return secret;
    }

    public void setSecret(String secret) {
        this.secret = secret;
    }

    public long getExpire() {
        return expire;
    }

    public void setExpire(long expire) {
        this.expire = expire;
    }
}

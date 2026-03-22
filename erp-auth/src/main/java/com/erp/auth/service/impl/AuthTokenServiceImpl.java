package com.erp.auth.service.impl;

import com.erp.auth.config.AuthJwtProperties;
import com.erp.auth.domain.SysUser;
import com.erp.auth.domain.vo.AuthTokenVerifyResult;
import com.erp.auth.service.AuthAccountService;
import com.erp.auth.service.AuthTokenService;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * 认证中心 Token 服务实现。
 */
@Service
public class AuthTokenServiceImpl implements AuthTokenService {
    private final AuthAccountService accountService;
    private final AuthJwtProperties jwtProperties;

    private Key signingKey;

    public AuthTokenServiceImpl(AuthAccountService accountService, AuthJwtProperties jwtProperties) {
        this.accountService = accountService;
        this.jwtProperties = jwtProperties;
    }

    /**
     * 初始化签名密钥并进行启动期校验。
     */
    @PostConstruct
    public void init() {
        String secret = normalize(jwtProperties.getSecret());
        if (!StringUtils.hasText(secret)) {
            throw new IllegalStateException("security.jwt.secret 未配置，认证中心无法签发或校验 Token");
        }
        if (jwtProperties.getExpire() <= 0) {
            throw new IllegalStateException("security.jwt.expire 必须大于 0");
        }
        this.signingKey = Keys.hmacShaKeyFor(toKeyBytes(secret));
    }

    @Override
    public String createToken(SysUser user) {
        if (user == null || user.getUserId() == null || !StringUtils.hasText(user.getUserName())
                || !StringUtils.hasText(user.getTenantId())) {
            throw new IllegalArgumentException("签发 Token 时用户上下文不完整");
        }
        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", user.getUserId());
        claims.put("userName", user.getUserName());
        claims.put("tenantId", user.getTenantId());
        claims.put("tokenVersion", user.getTokenVersion() == null ? 0 : user.getTokenVersion());
        Date now = new Date();
        Date expiresAt = new Date(now.getTime() + jwtProperties.getExpire());
        return Jwts.builder()
                .setClaims(claims)
                .setSubject(user.getUserName())
                .setIssuedAt(now)
                .setExpiration(expiresAt)
                .signWith(signingKey, SignatureAlgorithm.HS512)
                .compact();
    }

    @Override
    public AuthTokenVerifyResult verifyToken(String token, String requestedTenantId) {
        if (!StringUtils.hasText(token)) {
            throw new IllegalArgumentException("缺少 Token");
        }
        Claims claims = Jwts.parserBuilder()
                .setSigningKey(signingKey)
                .build()
                .parseClaimsJws(token.trim())
                .getBody();
        Long userId = toLong(claims.get("userId"));
        String userName = normalize(claims.get("userName") == null ? claims.getSubject() : claims.get("userName"));
        String tenantId = normalize(claims.get("tenantId"));
        Integer tokenVersion = toInteger(claims.get("tokenVersion"));
        Long expiresAt = claims.getExpiration() == null ? null : claims.getExpiration().getTime();

        if (userId == null || !StringUtils.hasText(userName) || !StringUtils.hasText(tenantId)) {
            throw new IllegalArgumentException("Token 载荷缺失");
        }
        if (StringUtils.hasText(requestedTenantId) && !tenantId.equals(normalize(requestedTenantId))) {
            throw new IllegalArgumentException("租户与令牌不匹配");
        }

        SysUser user = accountService.selectUserByIdAndTenant(userId, tenantId);
        if (user == null) {
            throw new IllegalArgumentException("账号不存在");
        }
        if (!"0".equals(user.getStatus()) || "2".equals(user.getDelFlag())) {
            throw new IllegalArgumentException("账号不可用");
        }
        int currentTokenVersion = user.getTokenVersion() == null ? 0 : user.getTokenVersion();
        if (currentTokenVersion != tokenVersion) {
            throw new IllegalArgumentException("登录状态已失效，请重新登录");
        }
        return new AuthTokenVerifyResult(user.getUserId(), user.getUserName(), user.getTenantId(),
                currentTokenVersion, expiresAt);
    }

    /**
     * 规范化字符串值。
     *
     * @param value 原始值
     * @return 去空白后的字符串
     */
    private String normalize(Object value) {
        if (value == null) {
            return null;
        }
        String normalizedValue = String.valueOf(value).trim();
        return StringUtils.hasText(normalizedValue) ? normalizedValue : null;
    }

    /**
     * 将对象转换为 Long。
     *
     * @param value 原始值
     * @return Long 值
     */
    private Long toLong(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Number number) {
            return number.longValue();
        }
        try {
            return Long.parseLong(String.valueOf(value).trim());
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    /**
     * 将对象转换为 Integer。
     *
     * @param value 原始值
     * @return Integer 值
     */
    private Integer toInteger(Object value) {
        if (value == null) {
            return 0;
        }
        if (value instanceof Number number) {
            return number.intValue();
        }
        try {
            return Integer.parseInt(String.valueOf(value).trim());
        } catch (NumberFormatException ex) {
            return 0;
        }
    }

    /**
     * 将外部配置密钥转换为满足 HS512 长度要求的字节数组。
     *
     * @param configuredSecret 外部配置密钥
     * @return 可用于签名的字节数组
     */
    private byte[] toKeyBytes(String configuredSecret) {
        byte[] secretBytes = configuredSecret.getBytes(StandardCharsets.UTF_8);
        if (secretBytes.length >= 64) {
            return secretBytes;
        }
        try {
            return MessageDigest.getInstance("SHA-512").digest(secretBytes);
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("JVM 不支持 SHA-512 算法", ex);
        }
    }
}

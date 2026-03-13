package com.erp.common.utils;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.Key;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * JWT 工具类
 */
public class JwtUtils {
    private static final String SECRET_ENV = "ERP_JWT_SECRET";
    private static final String SECRET_PROPERTY = "erp.jwt.secret";
    private static final String EXPIRE_ENV = "ERP_JWT_EXPIRE_MS";
    private static final String EXPIRE_PROPERTY = "erp.jwt.expire-ms";
    private static final long DEFAULT_EXPIRE = 86400000L;
    private static final Key SECRET_KEY = initSecretKey();
    private static final long EXPIRE = initExpireMillis();

    /**
     * 生成令牌
     */
    public static String createToken(String userName, String tenantId, Integer tokenVersion) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("userName", userName);
        claims.put("tenantId", tenantId);
        claims.put("tokenVersion", tokenVersion == null ? 0 : tokenVersion);
        return Jwts.builder()
                .setClaims(claims)
                .setSubject(userName)
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + EXPIRE))
                .signWith(SECRET_KEY, SignatureAlgorithm.HS512)
                .compact();
    }

    /**
     * 从令牌中获取数据声明
     */
    public static Claims parseToken(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(SECRET_KEY)
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    /**
     * 初始化 JWT 签名密钥。
     * 优先读取外部配置，未配置时生成进程内临时密钥，避免把固定密钥写入代码仓库。
     *
     * @return JWT 签名密钥
     */
    private static Key initSecretKey() {
        String configuredSecret = resolveConfiguredValue(SECRET_PROPERTY, SECRET_ENV);
        if (configuredSecret == null) {
            return Keys.secretKeyFor(SignatureAlgorithm.HS512);
        }
        return Keys.hmacShaKeyFor(toKeyBytes(configuredSecret));
    }

    /**
     * 初始化 JWT 过期时间。
     *
     * @return 过期毫秒数
     */
    private static long initExpireMillis() {
        String rawExpire = resolveConfiguredValue(EXPIRE_PROPERTY, EXPIRE_ENV);
        if (rawExpire == null) {
            return DEFAULT_EXPIRE;
        }
        try {
            long expireMillis = Long.parseLong(rawExpire);
            return expireMillis > 0 ? expireMillis : DEFAULT_EXPIRE;
        } catch (NumberFormatException ex) {
            return DEFAULT_EXPIRE;
        }
    }

    /**
     * 按系统属性、环境变量顺序读取外部配置。
     *
     * @param propertyName 系统属性名
     * @param envName      环境变量名
     * @return 配置值，未配置时返回 null
     */
    private static String resolveConfiguredValue(String propertyName, String envName) {
        String configuredValue = normalize(System.getProperty(propertyName));
        if (configuredValue != null) {
            return configuredValue;
        }
        return normalize(System.getenv(envName));
    }

    /**
     * 规范化外部配置值。
     *
     * @param value 原始配置值
     * @return 去空白后的配置值
     */
    private static String normalize(String value) {
        if (value == null) {
            return null;
        }
        String trimmedValue = value.trim();
        return trimmedValue.isEmpty() ? null : trimmedValue;
    }

    /**
     * 将外部配置密钥转换为满足 HS512 长度要求的字节数组。
     *
     * @param configuredSecret 外部配置密钥
     * @return 可用于签名的字节数组
     */
    private static byte[] toKeyBytes(String configuredSecret) {
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

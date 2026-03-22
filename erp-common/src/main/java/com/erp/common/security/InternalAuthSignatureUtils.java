package com.erp.common.security;

import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import java.util.Objects;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

/**
 * 内部身份透传签名工具。
 */
public final class InternalAuthSignatureUtils {
    private static final String HMAC_ALGORITHM = "HmacSHA256";

    private InternalAuthSignatureUtils() {
    }

    /**
     * 计算内部身份透传签名。
     *
     * @param secret    内部签名密钥
     * @param principal 已认证用户主体
     * @return 签名字符串
     */
    public static String sign(String secret, AuthenticatedUserPrincipal principal) {
        return sign(secret,
                principal == null ? null : principal.getUserId(),
                principal == null ? null : principal.getUserName(),
                principal == null ? null : principal.getTenantId(),
                principal == null ? null : principal.getTokenVersion(),
                principal == null ? null : principal.getExpiresAt());
    }

    /**
     * 计算内部身份透传签名。
     *
     * @param secret       内部签名密钥
     * @param userId       用户ID
     * @param userName     用户账号
     * @param tenantId     租户编号
     * @param tokenVersion Token 版本号
     * @param expiresAt    过期时间戳
     * @return 签名字符串
     */
    public static String sign(String secret, Long userId, String userName, String tenantId, Integer tokenVersion,
            Long expiresAt) {
        try {
            Mac mac = Mac.getInstance(HMAC_ALGORITHM);
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), HMAC_ALGORITHM));
            byte[] bytes = mac.doFinal(buildPayload(userId, userName, tenantId, tokenVersion, expiresAt)
                    .getBytes(StandardCharsets.UTF_8));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        } catch (NoSuchAlgorithmException | InvalidKeyException ex) {
            throw new IllegalStateException("计算内部认证签名失败", ex);
        }
    }

    /**
     * 校验签名是否一致。
     *
     * @param secret             内部签名密钥
     * @param principal          已认证用户主体
     * @param expectedSignature  待校验签名
     * @return true 表示校验通过
     */
    public static boolean matches(String secret, AuthenticatedUserPrincipal principal, String expectedSignature) {
        String actualSignature = sign(secret, principal);
        return Objects.equals(actualSignature, expectedSignature);
    }

    /**
     * 构建签名原文。
     *
     * @param userId       用户ID
     * @param userName     用户账号
     * @param tenantId     租户编号
     * @param tokenVersion Token 版本号
     * @param expiresAt    过期时间戳
     * @return 签名原文
     */
    private static String buildPayload(Long userId, String userName, String tenantId, Integer tokenVersion,
            Long expiresAt) {
        return normalize(userId)
                + "\n" + normalize(userName)
                + "\n" + normalize(tenantId)
                + "\n" + normalize(tokenVersion)
                + "\n" + normalize(expiresAt);
    }

    /**
     * 规范化签名字段，避免 null 参与拼接时出现不一致。
     *
     * @param value 原始值
     * @return 规范化字符串
     */
    private static String normalize(Object value) {
        return value == null ? "" : String.valueOf(value).trim();
    }
}

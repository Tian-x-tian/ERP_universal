package com.erp.ai.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.erp.common.client.internal.InternalSystemClientProperties;
import com.erp.ai.model.AiConfirmationPayload;
import com.erp.ai.service.AiConfirmationTokenService;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Base64;

/**
 * AI 动作确认票据服务实现。
 */
@Service
public class AiConfirmationTokenServiceImpl implements AiConfirmationTokenService {
    private static final String HMAC_ALGORITHM = "HmacSHA256";

    private final InternalSystemClientProperties internalSystemClientProperties;
    private final ObjectMapper objectMapper;

    public AiConfirmationTokenServiceImpl(InternalSystemClientProperties internalSystemClientProperties,
            ObjectMapper objectMapper) {
        this.internalSystemClientProperties = internalSystemClientProperties;
        this.objectMapper = objectMapper;
    }

    /**
     * 生成确认票据。
     *
     * @param payload 票据载荷
     * @return 签名票据
     */
    @Override
    public String createToken(AiConfirmationPayload payload) {
        if (payload == null || payload.getUserId() == null || !StringUtils.hasText(payload.getActionKey())
                || payload.getExpiresAt() == null) {
            throw new IllegalArgumentException("确认票据参数不完整");
        }
        try {
            String payloadJson = objectMapper.writeValueAsString(payload);
            String encodedPayload = Base64.getUrlEncoder().withoutPadding()
                    .encodeToString(payloadJson.getBytes(StandardCharsets.UTF_8));
            String signature = Base64.getUrlEncoder().withoutPadding()
                    .encodeToString(sign(encodedPayload));
            return encodedPayload + "." + signature;
        } catch (Exception ex) {
            throw new IllegalStateException("确认票据生成失败", ex);
        }
    }

    /**
     * 解析并校验确认票据。
     *
     * @param token 票据文本
     * @return 解析后的载荷
     */
    @Override
    public AiConfirmationPayload parseToken(String token) {
        if (!StringUtils.hasText(token)) {
            throw new IllegalArgumentException("确认票据不能为空");
        }
        String[] parts = token.trim().split("\\.");
        if (parts.length != 2) {
            throw new IllegalArgumentException("确认票据格式非法");
        }
        try {
            byte[] expectedSignature = sign(parts[0]);
            byte[] actualSignature = Base64.getUrlDecoder().decode(parts[1]);
            if (!MessageDigest.isEqual(expectedSignature, actualSignature)) {
                throw new IllegalArgumentException("确认票据签名无效");
            }
            byte[] payloadBytes = Base64.getUrlDecoder().decode(parts[0]);
            AiConfirmationPayload payload = objectMapper.readValue(payloadBytes, AiConfirmationPayload.class);
            if (payload.getExpiresAt() == null || payload.getExpiresAt() < System.currentTimeMillis()) {
                throw new IllegalArgumentException("确认票据已过期");
            }
            return payload;
        } catch (IllegalArgumentException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new IllegalArgumentException("确认票据解析失败", ex);
        }
    }

    /**
     * 对票据载荷执行 HMAC 签名。
     *
     * @param payloadText 载荷文本
     * @return 签名字节数组
     */
    private byte[] sign(String payloadText) {
        try {
            Mac mac = Mac.getInstance(HMAC_ALGORITHM);
            String secret = internalSystemClientProperties.resolveAuthSignatureSecret();
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), HMAC_ALGORITHM));
            return mac.doFinal(payloadText.getBytes(StandardCharsets.UTF_8));
        } catch (Exception ex) {
            throw new IllegalStateException("确认票据签名失败", ex);
        }
    }
}

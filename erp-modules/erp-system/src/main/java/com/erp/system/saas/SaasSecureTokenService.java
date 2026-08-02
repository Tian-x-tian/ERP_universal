package com.erp.system.saas;

import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.HexFormat;

@Component
public class SaasSecureTokenService {
    private final SecureRandom secureRandom = new SecureRandom();

    public SecureToken generate() {
        byte[] value = new byte[32];
        secureRandom.nextBytes(value);
        String rawToken = Base64.getUrlEncoder().withoutPadding().encodeToString(value);
        return new SecureToken(rawToken, sha256(rawToken));
    }

    public String sha256(String value) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is not available", exception);
        }
    }

    public record SecureToken(String rawToken, String tokenHash) { }
}

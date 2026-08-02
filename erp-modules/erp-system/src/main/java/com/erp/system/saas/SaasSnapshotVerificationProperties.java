package com.erp.system.saas;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Component
@ConfigurationProperties(prefix = "erp.saas.snapshot.verification")
public class SaasSnapshotVerificationProperties {
    private String keyId;
    private String secret;
    private Duration clockSkew = Duration.ofMinutes(1);

    public String getKeyId() { return keyId; }
    public void setKeyId(String keyId) { this.keyId = keyId; }
    public String getSecret() { return secret; }
    public void setSecret(String secret) { this.secret = secret; }
    public Duration getClockSkew() { return clockSkew; }
    public void setClockSkew(Duration clockSkew) { this.clockSkew = clockSkew; }
}

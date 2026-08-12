package com.erp.saas.control.service.snapshot;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Component
@ConfigurationProperties(prefix = "erp.saas.snapshot.signing")
public class SaasSnapshotSigningProperties {
    private String keyId;
    private String secret;
    private Duration validity = Duration.ofHours(24);
    private Duration renewBefore = Duration.ofMinutes(5);

    public String getKeyId() { return keyId; }
    public void setKeyId(String keyId) { this.keyId = keyId; }
    public String getSecret() { return secret; }
    public void setSecret(String secret) { this.secret = secret; }
    public Duration getValidity() { return validity; }
    public void setValidity(Duration validity) { this.validity = validity; }
    public Duration getRenewBefore() { return renewBefore; }
    public void setRenewBefore(Duration renewBefore) { this.renewBefore = renewBefore; }
}

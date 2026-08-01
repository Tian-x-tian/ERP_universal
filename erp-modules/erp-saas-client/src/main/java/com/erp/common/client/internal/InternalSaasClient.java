package com.erp.common.client.internal;

import com.erp.saas.contract.model.SaasEntitlementSnapshot;
import com.erp.saas.contract.model.SaasHostResolution;
import com.erp.saas.contract.model.SaasProvisioningResult;
import com.erp.saas.contract.model.SaasUsageEvent;
import com.erp.saas.contract.model.SaasUsageEventValidator;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;

/**
 * Typed transport client for internal SaaS control-plane endpoints.
 */
@Component
public class InternalSaasClient {
    private final RestTemplate restTemplate;
    private final InternalRequestHeaderFactory headerFactory;
    private final InternalSystemClientProperties properties;

    public InternalSaasClient(@Qualifier("saasInternalRestTemplate") RestTemplate restTemplate,
            InternalRequestHeaderFactory headerFactory, InternalSystemClientProperties properties) {
        this.restTemplate = restTemplate;
        this.headerFactory = headerFactory;
        this.properties = properties;
    }

    public SaasHostResolution resolveTenantByHost(String host) {
        requireText(host, "host");
        URI uri = UriComponentsBuilder.fromHttpUrl(properties.resolveSaasBaseUrl())
                .path("/internal/saas/hosts/resolve")
                .queryParam("host", "{host}")
                .encode()
                .buildAndExpand(host)
                .toUri();
        return exchangeRequired(uri, SaasHostResolution.class);
    }

    public SaasEntitlementSnapshot loadEntitlementSnapshot(String tenantId) {
        requireText(tenantId, "tenantId");
        URI uri = UriComponentsBuilder.fromHttpUrl(properties.resolveSaasBaseUrl())
                .path("/internal/saas/tenants/")
                .pathSegment(tenantId)
                .path("/entitlement-snapshot")
                .build()
                .encode()
                .toUri();
        return exchangeRequired(uri, SaasEntitlementSnapshot.class);
    }

    public void reportUsage(SaasUsageEvent event) {
        SaasUsageEventValidator.validate(event);
        post(buildUri("/internal/saas/usage-events"), event);
    }

    public void reportProvisioningResult(SaasProvisioningResult result) {
        validateProvisioningResult(result);
        post(buildUri("/internal/saas/provisioning/results"), result);
    }

    private <T> T exchangeRequired(URI uri, Class<T> responseType) {
        ResponseEntity<T> response = restTemplate.exchange(uri, HttpMethod.GET,
                new HttpEntity<>(headerFactory.buildHeaders()), responseType);
        if (response.getBody() == null) {
            throw new IllegalStateException("SaaS control-plane response body must not be empty");
        }
        return response.getBody();
    }

    private void post(URI uri, Object body) {
        restTemplate.exchange(uri, HttpMethod.POST, new HttpEntity<>(body, headerFactory.buildHeaders()), Void.class);
    }

    private URI buildUri(String path) {
        return UriComponentsBuilder.fromHttpUrl(properties.resolveSaasBaseUrl())
                .path(path)
                .build(true)
                .toUri();
    }

    private void requireText(String value, String field) {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException(field + " must not be blank");
        }
    }

    private void validateProvisioningResult(SaasProvisioningResult result) {
        if (result == null) {
            throw new IllegalArgumentException("result must not be null");
        }
        requireText(result.getRequestId(), "requestId");
        requireText(result.getTenantId(), "tenantId");
        if (!result.isSuccess()) {
            requireText(result.getMessage(), "message");
        }
        if (result.getCompletedAtEpochMs() <= 0) {
            throw new IllegalArgumentException("completedAtEpochMs must be positive");
        }
    }
}

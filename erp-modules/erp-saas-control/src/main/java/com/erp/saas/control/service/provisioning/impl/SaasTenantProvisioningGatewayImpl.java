package com.erp.saas.control.service.provisioning.impl;

import com.erp.common.client.internal.InternalRequestHeaderFactory;
import com.erp.common.client.internal.InternalSystemClient;
import com.erp.saas.contract.model.DeploymentMode;
import com.erp.saas.contract.model.SaasTenantActivationReissueRequest;
import com.erp.saas.contract.model.SaasTenantActivationReissueResult;
import com.erp.saas.contract.model.SaasTenantInitializationRequest;
import com.erp.saas.contract.model.SaasTenantInitializationResult;
import com.erp.saas.contract.model.SaasTenantPurgeRequest;
import com.erp.saas.contract.model.SaasTenantPurgeResult;
import com.erp.saas.control.domain.entity.SaasDeploymentEntity;
import com.erp.saas.control.service.provisioning.SaasProvisioningException;
import com.erp.saas.control.service.provisioning.SaasTenantProvisioningGateway;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.net.URI;
import java.net.http.HttpClient;
import java.time.Duration;

@Component
public class SaasTenantProvisioningGatewayImpl implements SaasTenantProvisioningGateway {
    private static final String INITIALIZE_PATH = "/system/internal/saas/tenants/initialize";
    private static final String REISSUE_ACTIVATION_PATH = "/system/internal/saas/tenants/activation/reissue";
    private static final String PURGE_PATH = "/system/internal/saas/tenants/purge";

    private final InternalSystemClient systemClient;
    private final InternalRequestHeaderFactory headerFactory;
    private final RestTemplate dedicatedRestTemplate;

    @Autowired
    public SaasTenantProvisioningGatewayImpl(InternalSystemClient systemClient,
            InternalRequestHeaderFactory headerFactory, RestTemplateBuilder restTemplateBuilder) {
        this(systemClient, headerFactory, dedicatedClient(restTemplateBuilder));
    }

    SaasTenantProvisioningGatewayImpl(InternalSystemClient systemClient,
            InternalRequestHeaderFactory headerFactory, RestTemplate dedicatedRestTemplate) {
        this.systemClient = systemClient;
        this.headerFactory = headerFactory;
        this.dedicatedRestTemplate = dedicatedRestTemplate;
    }

    @Override
    public SaasTenantInitializationResult initialize(SaasDeploymentEntity deployment,
            SaasTenantInitializationRequest request) {
        if (deployment == null || deployment.getMode() == null) {
            throw invalid("Deployment is not registered");
        }
        if (deployment.getMode() == DeploymentMode.SHARED) {
            return required(systemClient.initializeSaasTenant(request));
        }
        URI uri = endpoint(deployment.getDeploymentRef(), INITIALIZE_PATH);
        ResponseEntity<SaasTenantInitializationResult> response = dedicatedRestTemplate.exchange(
                uri, HttpMethod.POST,
                new HttpEntity<>(request, headerFactory.buildServiceHeaders()),
                SaasTenantInitializationResult.class);
        return required(response.getBody());
    }

    @Override
    public SaasTenantActivationReissueResult reissueActivation(SaasDeploymentEntity deployment,
            SaasTenantActivationReissueRequest request) {
        if (deployment == null || deployment.getMode() == null) {
            throw invalid("Deployment is not registered");
        }
        if (deployment.getMode() == DeploymentMode.SHARED) {
            return required(systemClient.reissueSaasTenantActivation(request));
        }
        URI uri = endpoint(deployment.getDeploymentRef(), REISSUE_ACTIVATION_PATH);
        ResponseEntity<SaasTenantActivationReissueResult> response = dedicatedRestTemplate.exchange(
                uri, HttpMethod.POST,
                new HttpEntity<>(request, headerFactory.buildServiceHeaders()),
                SaasTenantActivationReissueResult.class);
        return required(response.getBody());
    }

    @Override
    public SaasTenantPurgeResult purge(SaasDeploymentEntity deployment, SaasTenantPurgeRequest request) {
        if (deployment == null || deployment.getMode() == null) {
            throw invalid("Deployment is not registered");
        }
        if (deployment.getMode() == DeploymentMode.SHARED) {
            return required(systemClient.purgeSaasTenant(request));
        }
        URI uri = endpoint(deployment.getDeploymentRef(), PURGE_PATH);
        ResponseEntity<SaasTenantPurgeResult> response = dedicatedRestTemplate.exchange(
                uri, HttpMethod.POST,
                new HttpEntity<>(request, headerFactory.buildServiceHeaders()),
                SaasTenantPurgeResult.class);
        return required(response.getBody());
    }

    private <T> T required(T result) {
        if (result == null) throw invalid("System tenant initialization returned no result");
        return result;
    }

    private URI endpoint(String baseUrl, String path) {
        if (baseUrl == null || baseUrl.isBlank()) throw invalid("Deployment reference is missing");
        String normalized = baseUrl.trim();
        while (normalized.endsWith("/")) normalized = normalized.substring(0, normalized.length() - 1);
        try {
            return URI.create(normalized + path);
        } catch (IllegalArgumentException error) {
            throw new SaasProvisioningException(SaasProvisioningException.ErrorCode.CONFLICT,
                    "Deployment reference is invalid", error);
        }
    }

    private SaasProvisioningException invalid(String message) {
        return new SaasProvisioningException(SaasProvisioningException.ErrorCode.CONFLICT, message);
    }

    private static RestTemplate dedicatedClient(RestTemplateBuilder builder) {
        HttpClient httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(5))
                .followRedirects(HttpClient.Redirect.NEVER)
                .build();
        JdkClientHttpRequestFactory requestFactory = new JdkClientHttpRequestFactory(httpClient);
        requestFactory.setReadTimeout(Duration.ofSeconds(30));
        return builder.requestFactory(() -> requestFactory).build();
    }
}

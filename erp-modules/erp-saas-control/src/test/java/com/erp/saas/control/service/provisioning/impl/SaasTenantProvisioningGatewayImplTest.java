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
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import java.net.URI;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class SaasTenantProvisioningGatewayImplTest {
    @Test
    void shouldUseSharedSystemClientForSharedDeployment() {
        InternalSystemClient systemClient = mock(InternalSystemClient.class);
        InternalRequestHeaderFactory headerFactory = mock(InternalRequestHeaderFactory.class);
        RestTemplate restTemplate = mock(RestTemplate.class);
        SaasTenantProvisioningGatewayImpl gateway = new SaasTenantProvisioningGatewayImpl(
                systemClient, headerFactory, restTemplate);
        SaasTenantInitializationRequest request = new SaasTenantInitializationRequest();
        SaasTenantInitializationResult expected = new SaasTenantInitializationResult();
        when(systemClient.initializeSaasTenant(request)).thenReturn(expected);

        assertThat(gateway.initialize(deployment(DeploymentMode.SHARED), request)).isSameAs(expected);

        verify(restTemplate, never()).exchange(any(URI.class), any(), any(),
                eq(SaasTenantInitializationResult.class));
    }

    @Test
    void shouldUseDedicatedBaseUrlAndServicePrincipalHeaders() {
        InternalSystemClient systemClient = mock(InternalSystemClient.class);
        InternalRequestHeaderFactory headerFactory = mock(InternalRequestHeaderFactory.class);
        RestTemplate restTemplate = mock(RestTemplate.class);
        SaasTenantProvisioningGatewayImpl gateway = new SaasTenantProvisioningGatewayImpl(
                systemClient, headerFactory, restTemplate);
        HttpHeaders headers = new HttpHeaders();
        headers.set("X-Test", "signed");
        when(headerFactory.buildServiceHeaders()).thenReturn(headers);
        SaasTenantInitializationResult expected = new SaasTenantInitializationResult();
        when(restTemplate.exchange(any(URI.class), eq(HttpMethod.POST), any(HttpEntity.class),
                eq(SaasTenantInitializationResult.class))).thenReturn(ResponseEntity.ok(expected));
        ArgumentCaptor<URI> uri = ArgumentCaptor.forClass(URI.class);
        ArgumentCaptor<HttpEntity> entity = ArgumentCaptor.forClass(HttpEntity.class);

        assertThat(gateway.initialize(deployment(DeploymentMode.DEDICATED),
                new SaasTenantInitializationRequest())).isSameAs(expected);

        verify(restTemplate).exchange(uri.capture(), eq(HttpMethod.POST), entity.capture(),
                eq(SaasTenantInitializationResult.class));
        assertThat(uri.getValue().toString()).isEqualTo(
                "https://dedicated.example/erp/system/internal/saas/tenants/initialize");
        assertThat(entity.getValue().getHeaders().getFirst("X-Test")).isEqualTo("signed");
    }

    @Test
    void shouldRouteSharedActivationReissueThroughSystemClient() {
        InternalSystemClient systemClient = mock(InternalSystemClient.class);
        SaasTenantProvisioningGatewayImpl gateway = new SaasTenantProvisioningGatewayImpl(
                systemClient, mock(InternalRequestHeaderFactory.class), mock(RestTemplate.class));
        SaasTenantActivationReissueRequest request = new SaasTenantActivationReissueRequest(
                "req-1", "tenant-a");
        SaasTenantActivationReissueResult expected = new SaasTenantActivationReissueResult();
        when(systemClient.reissueSaasTenantActivation(request)).thenReturn(expected);

        assertThat(gateway.reissueActivation(deployment(DeploymentMode.SHARED), request)).isSameAs(expected);

        verify(systemClient).reissueSaasTenantActivation(request);
    }

    @Test
    void shouldUseDedicatedBaseUrlForActivationReissue() {
        InternalRequestHeaderFactory headerFactory = mock(InternalRequestHeaderFactory.class);
        RestTemplate restTemplate = mock(RestTemplate.class);
        SaasTenantProvisioningGatewayImpl gateway = new SaasTenantProvisioningGatewayImpl(
                mock(InternalSystemClient.class), headerFactory, restTemplate);
        when(headerFactory.buildServiceHeaders()).thenReturn(new HttpHeaders());
        SaasTenantActivationReissueResult expected = new SaasTenantActivationReissueResult();
        when(restTemplate.exchange(any(URI.class), eq(HttpMethod.POST), any(HttpEntity.class),
                eq(SaasTenantActivationReissueResult.class))).thenReturn(ResponseEntity.ok(expected));
        ArgumentCaptor<URI> uri = ArgumentCaptor.forClass(URI.class);

        assertThat(gateway.reissueActivation(deployment(DeploymentMode.DEDICATED),
                new SaasTenantActivationReissueRequest("req-1", "tenant-a"))).isSameAs(expected);

        verify(restTemplate).exchange(uri.capture(), eq(HttpMethod.POST), any(HttpEntity.class),
                eq(SaasTenantActivationReissueResult.class));
        assertThat(uri.getValue().toString()).isEqualTo(
                "https://dedicated.example/erp/system/internal/saas/tenants/activation/reissue");
    }

    @Test
    void shouldRouteConfirmedPurgeToRegisteredDeployment() {
        InternalSystemClient systemClient = mock(InternalSystemClient.class);
        SaasTenantProvisioningGatewayImpl gateway = new SaasTenantProvisioningGatewayImpl(
                systemClient, mock(InternalRequestHeaderFactory.class), mock(RestTemplate.class));
        SaasTenantPurgeRequest request = new SaasTenantPurgeRequest("purge-001", "tenant-a", "tenant-a");
        SaasTenantPurgeResult expected = new SaasTenantPurgeResult();
        when(systemClient.purgeSaasTenant(request)).thenReturn(expected);

        assertThat(gateway.purge(deployment(DeploymentMode.SHARED), request)).isSameAs(expected);

        verify(systemClient).purgeSaasTenant(request);
    }

    private SaasDeploymentEntity deployment(DeploymentMode mode) {
        SaasDeploymentEntity deployment = new SaasDeploymentEntity();
        deployment.setMode(mode);
        deployment.setDeploymentRef(mode == DeploymentMode.SHARED
                ? "http://erp-system" : "https://dedicated.example/erp");
        return deployment;
    }
}

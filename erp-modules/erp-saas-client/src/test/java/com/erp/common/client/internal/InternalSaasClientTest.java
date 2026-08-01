package com.erp.common.client.internal;

import com.erp.saas.contract.model.DeploymentMode;
import com.erp.saas.contract.model.SaasEntitlementSnapshot;
import com.erp.saas.contract.model.SaasHostResolution;
import com.erp.saas.contract.model.SaasProvisioningResult;
import com.erp.saas.contract.model.SaasQuotaKeys;
import com.erp.saas.contract.model.SaasUsageEvent;
import com.erp.saas.contract.model.SaasUsageOperation;
import com.erp.saas.contract.model.TenantLifecycleState;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestTemplate;

import java.net.URI;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class InternalSaasClientTest {
    @Mock
    private RestTemplate restTemplate;
    @Mock
    private InternalRequestHeaderFactory headerFactory;

    private InternalSaasClient client;
    private HttpHeaders headers;

    @BeforeEach
    void setUp() {
        headers = new HttpHeaders();
        org.mockito.Mockito.lenient().when(headerFactory.buildHeaders()).thenReturn(headers);
        client = new InternalSaasClient(restTemplate, headerFactory, new InternalSystemClientProperties());
    }

    @Test
    void shouldResolveTenantByHost() {
        SaasHostResolution expected = new SaasHostResolution("acme.example", "tenant-a", DeploymentMode.SHARED,
                TenantLifecycleState.ACTIVE, true);
        when(restTemplate.exchange(any(URI.class), eq(HttpMethod.GET), any(HttpEntity.class),
                eq(SaasHostResolution.class))).thenReturn(ResponseEntity.ok(expected));
        ArgumentCaptor<URI> uri = ArgumentCaptor.forClass(URI.class);
        ArgumentCaptor<HttpEntity<Void>> entity = httpEntityCaptor();

        SaasHostResolution actual = client.resolveTenantByHost("acme.example");

        Assertions.assertSame(expected, actual);
        verify(restTemplate).exchange(uri.capture(), eq(HttpMethod.GET), entity.capture(),
                eq(SaasHostResolution.class));
        Assertions.assertEquals("http://erp-saas-control/internal/saas/hosts/resolve?host=acme.example",
                uri.getValue().toASCIIString());
        Assertions.assertEquals(headers, entity.getValue().getHeaders());
        verify(headerFactory).buildHeaders();
    }

    @Test
    void shouldEncodeHostQuery() {
        when(restTemplate.exchange(any(URI.class), eq(HttpMethod.GET), any(HttpEntity.class),
                eq(SaasHostResolution.class))).thenReturn(ResponseEntity.ok(new SaasHostResolution()));
        ArgumentCaptor<URI> uri = ArgumentCaptor.forClass(URI.class);

        client.resolveTenantByHost("Acme + Sales.example");

        verify(restTemplate).exchange(uri.capture(), eq(HttpMethod.GET), any(HttpEntity.class),
                eq(SaasHostResolution.class));
        Assertions.assertEquals(
                "http://erp-saas-control/internal/saas/hosts/resolve?host=Acme%20%2B%20Sales.example",
                uri.getValue().toASCIIString());
    }

    @Test
    void shouldLoadEntitlementSnapshot() {
        SaasEntitlementSnapshot expected = new SaasEntitlementSnapshot();
        when(restTemplate.exchange(any(URI.class), eq(HttpMethod.GET), any(HttpEntity.class),
                eq(SaasEntitlementSnapshot.class))).thenReturn(ResponseEntity.ok(expected));
        ArgumentCaptor<URI> uri = ArgumentCaptor.forClass(URI.class);

        Assertions.assertSame(expected, client.loadEntitlementSnapshot("tenant/a"));

        verify(restTemplate).exchange(uri.capture(), eq(HttpMethod.GET), any(HttpEntity.class),
                eq(SaasEntitlementSnapshot.class));
        Assertions.assertEquals(
                "http://erp-saas-control/internal/saas/tenants/tenant%2Fa/entitlement-snapshot",
                uri.getValue().toASCIIString());
    }

    @Test
    void shouldReportUsageWithIdempotencyKey() {
        SaasUsageEvent event = usageEvent();
        when(restTemplate.exchange(any(URI.class), eq(HttpMethod.POST), any(HttpEntity.class), eq(Void.class)))
                .thenReturn(ResponseEntity.noContent().build());
        ArgumentCaptor<HttpEntity<SaasUsageEvent>> entity = httpEntityCaptor();

        client.reportUsage(event);

        verify(restTemplate).exchange(eq(URI.create("http://erp-saas-control/internal/saas/usage-events")),
                eq(HttpMethod.POST), entity.capture(), eq(Void.class));
        Assertions.assertSame(event, entity.getValue().getBody());
        Assertions.assertEquals("event-a", entity.getValue().getBody().getIdempotencyKey());
        Assertions.assertEquals(headers, entity.getValue().getHeaders());
    }

    @Test
    void shouldReportProvisioningResult() {
        SaasProvisioningResult result = new SaasProvisioningResult("request-a", "tenant-a", true, null, true, 1L);
        when(restTemplate.exchange(any(URI.class), eq(HttpMethod.POST), any(HttpEntity.class), eq(Void.class)))
                .thenReturn(ResponseEntity.ok().build());
        ArgumentCaptor<HttpEntity<SaasProvisioningResult>> entity = httpEntityCaptor();

        client.reportProvisioningResult(result);

        verify(restTemplate).exchange(eq(URI.create("http://erp-saas-control/internal/saas/provisioning/results")),
                eq(HttpMethod.POST), entity.capture(), eq(Void.class));
        Assertions.assertSame(result, entity.getValue().getBody());
    }

    @Test
    void shouldRejectBlankInputs() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> client.resolveTenantByHost("  "));
        Assertions.assertThrows(IllegalArgumentException.class, () -> client.loadEntitlementSnapshot(null));
        Assertions.assertThrows(IllegalArgumentException.class, () -> client.reportUsage(null));
        Assertions.assertThrows(IllegalArgumentException.class, () -> client.reportProvisioningResult(null));
        Assertions.assertThrows(IllegalArgumentException.class, () -> client.reportProvisioningResult(
                new SaasProvisioningResult("request-a", "tenant-a", false, " ", true, 1L)));
    }

    @Test
    void shouldRejectEmptyResponseBody() {
        when(restTemplate.exchange(any(URI.class), eq(HttpMethod.GET), any(HttpEntity.class),
                eq(SaasHostResolution.class))).thenReturn(ResponseEntity.ok().build());

        Assertions.assertThrows(IllegalStateException.class, () -> client.resolveTenantByHost("acme.example"));
    }

    @Test
    void shouldPropagateNotFound() {
        HttpClientErrorException exception = HttpClientErrorException.create(HttpStatus.NOT_FOUND, "missing",
                HttpHeaders.EMPTY, null, null);
        when(restTemplate.exchange(any(URI.class), eq(HttpMethod.GET), any(HttpEntity.class),
                eq(SaasHostResolution.class))).thenThrow(exception);

        Assertions.assertSame(exception, Assertions.assertThrows(HttpClientErrorException.class,
                () -> client.resolveTenantByHost("unknown.example")));
    }

    @Test
    void shouldPropagateServiceUnavailable() {
        HttpServerErrorException exception = HttpServerErrorException.create(HttpStatus.SERVICE_UNAVAILABLE,
                "down", HttpHeaders.EMPTY, null, null);
        when(restTemplate.exchange(any(URI.class), eq(HttpMethod.GET), any(HttpEntity.class),
                eq(SaasEntitlementSnapshot.class))).thenThrow(exception);

        Assertions.assertSame(exception, Assertions.assertThrows(HttpServerErrorException.class,
                () -> client.loadEntitlementSnapshot("tenant-a")));
    }

    @Test
    void shouldPropagateReportingFailure() {
        HttpServerErrorException exception = HttpServerErrorException.create(HttpStatus.INTERNAL_SERVER_ERROR,
                "failed", HttpHeaders.EMPTY, null, null);
        when(restTemplate.exchange(any(URI.class), eq(HttpMethod.POST), any(HttpEntity.class), eq(Void.class)))
                .thenThrow(exception);

        Assertions.assertSame(exception, Assertions.assertThrows(HttpServerErrorException.class,
                () -> client.reportUsage(usageEvent())));
    }

    private SaasUsageEvent usageEvent() {
        return new SaasUsageEvent("event-a", "tenant-a", SaasQuotaKeys.USER_COUNT, SaasUsageOperation.RESERVE,
                "user-a", 1L, null, 1L);
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private static <T> ArgumentCaptor<HttpEntity<T>> httpEntityCaptor() {
        return (ArgumentCaptor) ArgumentCaptor.forClass(HttpEntity.class);
    }
}

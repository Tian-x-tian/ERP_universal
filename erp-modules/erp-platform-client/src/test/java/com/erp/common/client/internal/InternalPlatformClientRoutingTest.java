package com.erp.common.client.internal;

import com.erp.platform.contract.model.PlatformRoleView;
import com.erp.saas.contract.model.SaasTenantActivationReissueRequest;
import com.erp.saas.contract.model.SaasTenantActivationReissueResult;
import com.erp.saas.contract.model.SaasQuotaUsage;
import com.erp.saas.contract.model.SaasTenantInitializationRequest;
import com.erp.saas.contract.model.SaasTenantInitializationResult;
import com.erp.saas.contract.model.SaasUsageEvent;
import com.erp.saas.contract.model.SaasUsageOperation;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import java.net.URI;
import java.util.Collections;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 平台内部客户端路由单元测试。
 */
@ExtendWith(MockitoExtension.class)
class InternalPlatformClientRoutingTest {

    @Mock
    private RestTemplate restTemplate;

    @Mock
    private InternalRequestHeaderFactory headerFactory;

    /**
     * 验证平台客户端默认调用 erp-system 服务。
     */
    @Test
    void shouldRoutePlatformClientToSystemServiceByDefault() {
        when(headerFactory.buildHeaders()).thenReturn(new HttpHeaders());
        when(restTemplate.exchange(any(URI.class),
                eq(HttpMethod.GET),
                any(HttpEntity.class),
                org.mockito.ArgumentMatchers.<ParameterizedTypeReference<List<PlatformRoleView>>>any()))
                .thenReturn(ResponseEntity.ok(Collections.emptyList()));
        InternalPlatformClient client = new InternalPlatformClient(restTemplate, headerFactory,
                new InternalSystemClientProperties());
        ArgumentCaptor<URI> uriCaptor = ArgumentCaptor.forClass(URI.class);

        client.listRoles();

        verify(restTemplate).exchange(uriCaptor.capture(),
                eq(HttpMethod.GET),
                any(HttpEntity.class),
                org.mockito.ArgumentMatchers.<ParameterizedTypeReference<List<PlatformRoleView>>>any());
        verify(headerFactory).buildHeaders();
        Assertions.assertEquals("http://erp-system/system/internal/platform/roles", uriCaptor.getValue().toString());
    }

    /**
     * 验证系统客户端默认调用 erp-system 服务。
     */
    @Test
    void shouldRouteSystemClientToSystemServiceByDefault() {
        when(headerFactory.buildHeaders()).thenReturn(new HttpHeaders());
        when(restTemplate.exchange(any(URI.class), eq(HttpMethod.GET), any(HttpEntity.class), eq(String.class)))
                .thenReturn(ResponseEntity.ok("enabled"));
        InternalSystemClient client = new InternalSystemClient(restTemplate, headerFactory,
                new InternalSystemClientProperties());
        ArgumentCaptor<URI> uriCaptor = ArgumentCaptor.forClass(URI.class);

        client.getConfigValue("erp.ai.enabled");

        verify(restTemplate).exchange(uriCaptor.capture(), eq(HttpMethod.GET), any(HttpEntity.class), eq(String.class));
        verify(headerFactory).buildHeaders();
        Assertions.assertEquals("http://erp-system/system/internal/config/erp.ai.enabled", uriCaptor.getValue().toString());
    }

    @Test
    void shouldRouteQuotaEventsToSystemService() {
        when(headerFactory.buildHeaders()).thenReturn(new HttpHeaders());
        SaasQuotaUsage response = new SaasQuotaUsage("storage_bytes", 0L, 64L, null);
        when(restTemplate.exchange(any(URI.class), eq(HttpMethod.POST), any(HttpEntity.class),
                eq(SaasQuotaUsage.class))).thenReturn(ResponseEntity.ok(response));
        InternalSystemClient client = new InternalSystemClient(restTemplate, headerFactory,
                new InternalSystemClientProperties());
        SaasUsageEvent event = new SaasUsageEvent("evt-1", "TENANT_A", "storage_bytes",
                SaasUsageOperation.RESERVE, "object-1", 64L, null, 1L);
        ArgumentCaptor<URI> uriCaptor = ArgumentCaptor.forClass(URI.class);
        ArgumentCaptor<HttpEntity> entityCaptor = ArgumentCaptor.forClass(HttpEntity.class);

        SaasQuotaUsage actual = client.applyQuotaEvent(event);

        verify(restTemplate).exchange(uriCaptor.capture(), eq(HttpMethod.POST), entityCaptor.capture(),
                eq(SaasQuotaUsage.class));
        Assertions.assertSame(response, actual);
        Assertions.assertSame(event, entityCaptor.getValue().getBody());
        Assertions.assertEquals("http://erp-system/system/internal/saas/quotas/events",
                uriCaptor.getValue().toString());
    }

    @Test
    void shouldRouteQuotaEventBatchToSystemService() {
        when(headerFactory.buildHeaders()).thenReturn(new HttpHeaders());
        List<SaasQuotaUsage> response = List.of(new SaasQuotaUsage("ai_input_tokens", 0L, 100L, 1L));
        when(restTemplate.exchange(any(URI.class), eq(HttpMethod.POST), any(HttpEntity.class),
                org.mockito.ArgumentMatchers.<ParameterizedTypeReference<List<SaasQuotaUsage>>>any()))
                .thenReturn(ResponseEntity.ok(response));
        InternalSystemClient client = new InternalSystemClient(restTemplate, headerFactory,
                new InternalSystemClientProperties());
        List<SaasUsageEvent> events = List.of(new SaasUsageEvent("evt", "TENANT_A", "ai_input_tokens",
                SaasUsageOperation.RESERVE, "ref", 100L, 1L, 1L));
        ArgumentCaptor<URI> uriCaptor = ArgumentCaptor.forClass(URI.class);

        List<SaasQuotaUsage> actual = client.applyQuotaEvents(events);

        verify(restTemplate).exchange(uriCaptor.capture(), eq(HttpMethod.POST), any(HttpEntity.class),
                org.mockito.ArgumentMatchers.<ParameterizedTypeReference<List<SaasQuotaUsage>>>any());
        Assertions.assertSame(response, actual);
        Assertions.assertEquals("http://erp-system/system/internal/saas/quotas/events/batch",
                uriCaptor.getValue().toString());
    }

    @Test
    void shouldRouteTenantInitializationToSystemService() {
        when(headerFactory.buildHeaders()).thenReturn(new HttpHeaders());
        SaasTenantInitializationResult response = new SaasTenantInitializationResult();
        when(restTemplate.exchange(any(URI.class), eq(HttpMethod.POST), any(HttpEntity.class),
                eq(SaasTenantInitializationResult.class))).thenReturn(ResponseEntity.ok(response));
        InternalSystemClient client = new InternalSystemClient(restTemplate, headerFactory,
                new InternalSystemClientProperties());
        SaasTenantInitializationRequest request = new SaasTenantInitializationRequest();
        ArgumentCaptor<URI> uriCaptor = ArgumentCaptor.forClass(URI.class);
        ArgumentCaptor<HttpEntity> entityCaptor = ArgumentCaptor.forClass(HttpEntity.class);

        SaasTenantInitializationResult actual = client.initializeSaasTenant(request);

        verify(restTemplate).exchange(uriCaptor.capture(), eq(HttpMethod.POST), entityCaptor.capture(),
                eq(SaasTenantInitializationResult.class));
        Assertions.assertSame(response, actual);
        Assertions.assertSame(request, entityCaptor.getValue().getBody());
        Assertions.assertEquals("http://erp-system/system/internal/saas/tenants/initialize",
                uriCaptor.getValue().toString());
    }

    @Test
    void shouldRouteActivationReissueToSystemService() {
        when(headerFactory.buildHeaders()).thenReturn(new HttpHeaders());
        SaasTenantActivationReissueResult response = new SaasTenantActivationReissueResult();
        when(restTemplate.exchange(any(URI.class), eq(HttpMethod.POST), any(HttpEntity.class),
                eq(SaasTenantActivationReissueResult.class))).thenReturn(ResponseEntity.ok(response));
        InternalSystemClient client = new InternalSystemClient(restTemplate, headerFactory,
                new InternalSystemClientProperties());
        SaasTenantActivationReissueRequest request = new SaasTenantActivationReissueRequest(
                "req-1", "tenant-a");
        ArgumentCaptor<URI> uriCaptor = ArgumentCaptor.forClass(URI.class);
        ArgumentCaptor<HttpEntity> entityCaptor = ArgumentCaptor.forClass(HttpEntity.class);

        SaasTenantActivationReissueResult actual = client.reissueSaasTenantActivation(request);

        verify(restTemplate).exchange(uriCaptor.capture(), eq(HttpMethod.POST), entityCaptor.capture(),
                eq(SaasTenantActivationReissueResult.class));
        Assertions.assertSame(response, actual);
        Assertions.assertSame(request, entityCaptor.getValue().getBody());
        Assertions.assertEquals("http://erp-system/system/internal/saas/tenants/activation/reissue",
                uriCaptor.getValue().toString());
    }
}

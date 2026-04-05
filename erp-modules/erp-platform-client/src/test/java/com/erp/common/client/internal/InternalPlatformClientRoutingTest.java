package com.erp.common.client.internal;

import com.erp.platform.contract.model.PlatformRoleView;
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
}

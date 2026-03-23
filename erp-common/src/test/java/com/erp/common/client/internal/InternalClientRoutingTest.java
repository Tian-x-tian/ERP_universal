package com.erp.common.client.internal;

import com.erp.platform.contract.model.PlatformRoleView;
import com.erp.workflow.contract.domain.vo.WorkflowCallbackEvent;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import java.lang.reflect.Method;
import java.net.URI;
import java.util.Collections;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

/**
 * 内部客户端默认路由配置单元测试。
 */
@ExtendWith(MockitoExtension.class)
class InternalClientRoutingTest {

    @Mock
    private RestTemplate restTemplate;

    @Mock
    private InternalRequestHeaderFactory headerFactory;

    /**
     * 验证空配置会回退到服务名地址。
     */
    @Test
    void shouldFallbackToServiceNameBaseUrlsWhenBlankConfigured() {
        InternalSystemClientProperties properties = new InternalSystemClientProperties();
        properties.setSystemBaseUrl("  ");
        properties.setWorkflowBaseUrl(null);
        properties.setBusinessBaseUrl("");

        Assertions.assertEquals("http://erp-system", properties.resolveSystemBaseUrl());
        Assertions.assertEquals("http://erp-workflow", properties.resolveWorkflowBaseUrl());
        Assertions.assertEquals("http://erp-business", properties.resolveBusinessBaseUrl());
    }

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
        InternalPlatformClient client = new InternalPlatformClient(restTemplate, headerFactory, new InternalSystemClientProperties());
        ArgumentCaptor<URI> uriCaptor = ArgumentCaptor.forClass(URI.class);

        client.listRoles();

        org.mockito.Mockito.verify(restTemplate).exchange(uriCaptor.capture(),
                eq(HttpMethod.GET),
                any(HttpEntity.class),
                org.mockito.ArgumentMatchers.<ParameterizedTypeReference<List<PlatformRoleView>>>any());
        Assertions.assertEquals("http://erp-system/system/internal/platform/roles", uriCaptor.getValue().toString());
    }

    /**
     * 验证工作流客户端默认调用 erp-workflow 服务。
     */
    @Test
    void shouldRouteWorkflowClientToWorkflowServiceByDefault() {
        when(headerFactory.buildHeaders()).thenReturn(new HttpHeaders());
        when(restTemplate.exchange(any(URI.class), eq(HttpMethod.POST), any(HttpEntity.class), eq(Boolean.class)))
                .thenReturn(ResponseEntity.ok(Boolean.TRUE));
        InternalWorkflowClient client = new InternalWorkflowClient(restTemplate, headerFactory, new InternalSystemClientProperties());
        ArgumentCaptor<URI> uriCaptor = ArgumentCaptor.forClass(URI.class);

        client.publishDefinition(12L);

        org.mockito.Mockito.verify(restTemplate).exchange(uriCaptor.capture(), eq(HttpMethod.POST), any(HttpEntity.class), eq(Boolean.class));
        Assertions.assertEquals("http://erp-workflow/workflow/internal/definitions/publish/12", uriCaptor.getValue().toString());
    }

    /**
     * 验证业务客户端默认调用 erp-business 服务。
     */
    @Test
    void shouldRouteBusinessClientToBusinessServiceByDefault() {
        when(headerFactory.buildHeaders()).thenReturn(new HttpHeaders());
        when(restTemplate.exchange(any(URI.class), eq(HttpMethod.POST), any(HttpEntity.class), eq(Void.class)))
                .thenReturn(ResponseEntity.ok().build());
        InternalBusinessClient client = new InternalBusinessClient(restTemplate, headerFactory, new InternalSystemClientProperties());
        ArgumentCaptor<URI> uriCaptor = ArgumentCaptor.forClass(URI.class);
        WorkflowCallbackEvent event = new WorkflowCallbackEvent();

        client.notifyWorkflowCallback(event);

        org.mockito.Mockito.verify(restTemplate).exchange(uriCaptor.capture(), eq(HttpMethod.POST), any(HttpEntity.class), eq(Void.class));
        Assertions.assertEquals("http://erp-business/business/internal/workflow/callbacks/terminal", uriCaptor.getValue().toString());
    }

    /**
     * 验证内部 RestTemplate 保留负载均衡标记。
     */
    @Test
    void shouldMarkInternalRestTemplateAsLoadBalanced() throws NoSuchMethodException {
        Method method = InternalSystemClientConfig.class.getMethod("internalSystemRestTemplate");

        Assertions.assertTrue(method.isAnnotationPresent(LoadBalanced.class));
    }
}

package com.erp.common.client.internal;

import com.erp.workflow.contract.domain.vo.WorkflowCallbackEvent;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import java.net.URI;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 业务内部客户端路由单元测试。
 */
@ExtendWith(MockitoExtension.class)
class InternalBusinessClientRoutingTest {

    @Mock
    private RestTemplate restTemplate;

    @Mock
    private InternalRequestHeaderFactory headerFactory;

    /**
     * 验证业务客户端默认调用 erp-business 服务。
     */
    @Test
    void shouldRouteBusinessClientToBusinessServiceByDefault() {
        when(headerFactory.buildHeaders()).thenReturn(new HttpHeaders());
        when(restTemplate.exchange(any(URI.class), eq(HttpMethod.POST), any(HttpEntity.class), eq(Void.class)))
                .thenReturn(ResponseEntity.ok().build());
        InternalBusinessClient client = new InternalBusinessClient(restTemplate, headerFactory,
                new InternalSystemClientProperties());
        ArgumentCaptor<URI> uriCaptor = ArgumentCaptor.forClass(URI.class);
        WorkflowCallbackEvent event = new WorkflowCallbackEvent();

        client.notifyWorkflowCallback(event);

        verify(restTemplate).exchange(uriCaptor.capture(), eq(HttpMethod.POST), any(HttpEntity.class), eq(Void.class));
        verify(headerFactory).buildHeaders();
        Assertions.assertEquals("http://erp-business/business/internal/workflow/callbacks/terminal",
                uriCaptor.getValue().toString());
    }
}

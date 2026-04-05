package com.erp.common.client.internal;

import com.erp.workflow.contract.domain.vo.WorkflowProcessOptionVO;
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
 * 工作流内部客户端路由单元测试。
 */
@ExtendWith(MockitoExtension.class)
class InternalWorkflowClientRoutingTest {

    @Mock
    private RestTemplate restTemplate;

    @Mock
    private InternalRequestHeaderFactory headerFactory;

    /**
     * 验证工作流客户端默认调用 erp-workflow 服务并正确拼接查询参数。
     */
    @Test
    void shouldRouteWorkflowClientToWorkflowServiceByDefault() {
        when(headerFactory.buildHeaders()).thenReturn(new HttpHeaders());
        when(restTemplate.exchange(any(URI.class),
                eq(HttpMethod.GET),
                any(HttpEntity.class),
                org.mockito.ArgumentMatchers.<ParameterizedTypeReference<List<WorkflowProcessOptionVO>>>any()))
                .thenReturn(ResponseEntity.ok(Collections.emptyList()));
        InternalWorkflowClient client = new InternalWorkflowClient(restTemplate, headerFactory,
                new InternalSystemClientProperties());
        ArgumentCaptor<URI> uriCaptor = ArgumentCaptor.forClass(URI.class);

        client.listProcessOptions("HR_EMPLOYEE", "SUBMIT");

        verify(restTemplate).exchange(uriCaptor.capture(),
                eq(HttpMethod.GET),
                any(HttpEntity.class),
                org.mockito.ArgumentMatchers.<ParameterizedTypeReference<List<WorkflowProcessOptionVO>>>any());
        verify(headerFactory).buildHeaders();
        Assertions.assertEquals(
                "http://erp-workflow/workflow/internal/bindings/options?domainType=HR_EMPLOYEE&actionCode=SUBMIT",
                uriCaptor.getValue().toString());
    }
}

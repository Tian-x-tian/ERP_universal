package com.erp.common.client.internal;

import com.erp.workflow.contract.domain.vo.WorkflowCallbackEvent;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;

/**
 * 业务内部接口客户端。
 */
@Component
public class InternalBusinessClient {
    private final RestTemplate restTemplate;
    private final InternalRequestHeaderFactory headerFactory;
    private final InternalSystemClientProperties properties;

    public InternalBusinessClient(RestTemplate internalSystemRestTemplate,
            InternalRequestHeaderFactory headerFactory,
            InternalSystemClientProperties properties) {
        this.restTemplate = internalSystemRestTemplate;
        this.headerFactory = headerFactory;
        this.properties = properties;
    }

    /**
     * 推送工作流终态回调事件。
     *
     * @param event 回调事件
     */
    public void notifyWorkflowCallback(WorkflowCallbackEvent event) {
        exchange(buildUri("/business/internal/workflow/callbacks/terminal"),
                HttpMethod.POST,
                event,
                Void.class);
    }

    /**
     * 发起内部 HTTP 调用。
     *
     * @param uri 目标地址
     * @param method 请求方法
     * @param body 请求体
     * @param responseType 响应类型
     * @param <T> 响应泛型
     * @return 响应对象
     */
    private <T> T exchange(URI uri, HttpMethod method, Object body, Class<T> responseType) {
        HttpHeaders headers = headerFactory.buildHeaders();
        ResponseEntity<T> response = restTemplate.exchange(uri, method, new HttpEntity<>(body, headers), responseType);
        return response.getBody();
    }

    /**
     * 构建完整内部调用地址。
     *
     * @param path 接口路径
     * @return URI
     */
    private URI buildUri(String path) {
        return UriComponentsBuilder.fromHttpUrl(properties.resolveBusinessBaseUrl())
                .path(path)
                .build(true)
                .toUri();
    }
}

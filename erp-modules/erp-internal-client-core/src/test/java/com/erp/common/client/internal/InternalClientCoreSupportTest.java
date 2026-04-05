package com.erp.common.client.internal;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.cloud.client.loadbalancer.LoadBalanced;

import java.lang.reflect.Method;

/**
 * 内部客户端基础装配单元测试。
 */
class InternalClientCoreSupportTest {

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
     * 验证内部 RestTemplate 保留负载均衡标记。
     *
     * @throws NoSuchMethodException 反射方法不存在时抛出
     */
    @Test
    void shouldMarkInternalRestTemplateAsLoadBalanced() throws NoSuchMethodException {
        Method method = InternalSystemClientConfig.class.getMethod("internalSystemRestTemplate");

        Assertions.assertTrue(method.isAnnotationPresent(LoadBalanced.class));
    }
}

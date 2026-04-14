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
     * 验证内部签名密钥默认不再提供仓库内保底值，避免把固定密钥带入运行环境。
     */
    @Test
    void shouldNotProvideDefaultInternalSignatureSecret() {
        InternalSystemClientProperties properties = new InternalSystemClientProperties();

        Assertions.assertNull(properties.getAuthSignatureSecret());
    }

    /**
     * 验证构建服务端内部调用请求头时，缺少签名密钥会直接失败而不是继续使用空签名。
     */
    @Test
    void shouldRejectBlankInternalSignatureSecretWhenBuildingHeaders() {
        InternalSystemClientProperties properties = new InternalSystemClientProperties();
        properties.setAuthSignatureSecret("   ");
        InternalRequestHeaderFactory headerFactory = new InternalRequestHeaderFactory(properties);

        IllegalStateException exception = Assertions.assertThrows(IllegalStateException.class, headerFactory::buildHeaders);

        Assertions.assertTrue(exception.getMessage().contains("内部签名密钥"));
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

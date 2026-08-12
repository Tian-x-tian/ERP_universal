package com.erp.common.client.internal;

import com.erp.common.core.context.TenantContextHolder;
import com.erp.common.security.AuthHeaders;
import com.erp.common.security.AuthenticatedUserPrincipal;
import com.erp.common.security.InternalAuthSignatureUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.context.annotation.Primary;
import org.springframework.http.HttpHeaders;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

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
        properties.setSaasBaseUrl("  ");

        Assertions.assertEquals("http://erp-system", properties.resolveSystemBaseUrl());
        Assertions.assertEquals("http://erp-workflow", properties.resolveWorkflowBaseUrl());
        Assertions.assertEquals("http://erp-business", properties.resolveBusinessBaseUrl());
        Assertions.assertEquals("http://erp-saas-control", properties.resolveSaasBaseUrl());
    }

    @Test
    void shouldUseFiniteSaasTimeoutDefaults() {
        InternalSystemClientProperties properties = new InternalSystemClientProperties();

        Assertions.assertEquals(2000, properties.resolveSaasConnectTimeoutMs());
        Assertions.assertEquals(5000, properties.resolveSaasReadTimeoutMs());
    }

    @Test
    void shouldRejectNonPositiveSaasTimeouts() {
        InternalSystemClientProperties properties = new InternalSystemClientProperties();
        properties.setSaasConnectTimeoutMs(0);
        properties.setSaasReadTimeoutMs(-1);

        Assertions.assertThrows(IllegalArgumentException.class, properties::resolveSaasConnectTimeoutMs);
        Assertions.assertThrows(IllegalArgumentException.class, properties::resolveSaasReadTimeoutMs);
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
        Assertions.assertTrue(method.isAnnotationPresent(Primary.class));
    }

    @Test
    void shouldBuildConfiguredServicePrincipalHeadersIgnoringRequestAndTenantContexts() {
        InternalSystemClientProperties properties = new InternalSystemClientProperties();
        properties.setAuthSignatureSecret("service-signature-secret");
        properties.setServiceUserId(99L);
        properties.setServiceUserName("saas-service");
        properties.setServiceTenantId("000000");
        properties.setServiceTokenVersion(7);
        properties.setServiceExpiresAt(4_102_444_800_000L);
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader(AuthHeaders.USER_ID, "123");
        request.addHeader(AuthHeaders.USER_NAME, "interactive-user");
        request.addHeader(AuthHeaders.TENANT_ID, "attacker-tenant");
        request.addHeader(AuthHeaders.EXPIRES_AT, "9999999999999");
        request.addHeader(AuthHeaders.SIGNATURE, "attacker-signature");
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));
        TenantContextHolder.setTenantId("thread-tenant");

        try {
            HttpHeaders headers = new InternalRequestHeaderFactory(properties).buildServiceHeaders();
            AuthenticatedUserPrincipal expected = new AuthenticatedUserPrincipal(99L, "saas-service", "000000", 7,
                    4_102_444_800_000L);

            Assertions.assertEquals("99", headers.getFirst(AuthHeaders.USER_ID));
            Assertions.assertEquals("saas-service", headers.getFirst(AuthHeaders.USER_NAME));
            Assertions.assertEquals("000000", headers.getFirst(AuthHeaders.TENANT_ID));
            Assertions.assertEquals("000000", headers.getFirst("tenantId"));
            Assertions.assertEquals("7", headers.getFirst(AuthHeaders.TOKEN_VERSION));
            Assertions.assertEquals("4102444800000", headers.getFirst(AuthHeaders.EXPIRES_AT));
            Assertions.assertTrue(InternalAuthSignatureUtils.matches("service-signature-secret", expected,
                    headers.getFirst(AuthHeaders.SIGNATURE)));
        } finally {
            RequestContextHolder.resetRequestAttributes();
            TenantContextHolder.clear();
        }
    }
}

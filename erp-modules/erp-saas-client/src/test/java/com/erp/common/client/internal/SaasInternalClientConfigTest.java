package com.erp.common.client.internal;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.web.client.RestTemplate;

import java.lang.reflect.Method;
import java.time.Duration;

class SaasInternalClientConfigTest {
    @Test
    void shouldBuildDedicatedLoadBalancedRestTemplateWithFiniteTimeouts() throws Exception {
        RestTemplateBuilder builder = Mockito.mock(RestTemplateBuilder.class, Mockito.RETURNS_SELF);
        RestTemplate template = new RestTemplate();
        Mockito.when(builder.build()).thenReturn(template);
        InternalSystemClientProperties properties = new InternalSystemClientProperties();

        RestTemplate actual = new SaasInternalClientConfig().saasInternalRestTemplate(builder, properties);

        Assertions.assertSame(template, actual);
        Mockito.verify(builder).setConnectTimeout(Duration.ofSeconds(2));
        Mockito.verify(builder).setReadTimeout(Duration.ofSeconds(5));
        Method method = SaasInternalClientConfig.class.getMethod("saasInternalRestTemplate",
                RestTemplateBuilder.class, InternalSystemClientProperties.class);
        Assertions.assertTrue(method.isAnnotationPresent(LoadBalanced.class));
    }
}

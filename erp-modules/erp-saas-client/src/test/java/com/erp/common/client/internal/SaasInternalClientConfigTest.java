package com.erp.common.client.internal;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.http.HttpStatus;
import org.springframework.http.client.ClientHttpResponse;
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

    @Test
    void shouldTreatEveryNonSuccessfulStatusAsError() throws Exception {
        RestTemplate template = new SaasInternalClientConfig().saasInternalRestTemplate(
                new RestTemplateBuilder(), new InternalSystemClientProperties());
        ClientHttpResponse redirect = Mockito.mock(ClientHttpResponse.class);
        ClientHttpResponse success = Mockito.mock(ClientHttpResponse.class);
        Mockito.when(redirect.getStatusCode()).thenReturn(HttpStatus.FOUND);
        Mockito.when(success.getStatusCode()).thenReturn(HttpStatus.NO_CONTENT);

        Assertions.assertTrue(template.getErrorHandler().hasError(redirect));
        Assertions.assertFalse(template.getErrorHandler().hasError(success));
    }

    @Test
    void shouldKeepSystemTemplatePrimaryWhenBothTemplatesExist() {
        try (AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext()) {
            context.registerBean(RestTemplateBuilder.class, () -> new RestTemplateBuilder());
            context.register(InternalSystemClientConfig.class, SaasInternalClientConfig.class);
            context.refresh();

            Assertions.assertEquals(2, context.getBeansOfType(RestTemplate.class).size());
            Assertions.assertSame(context.getBean("internalSystemRestTemplate"), context.getBean(RestTemplate.class));
        }
    }
}

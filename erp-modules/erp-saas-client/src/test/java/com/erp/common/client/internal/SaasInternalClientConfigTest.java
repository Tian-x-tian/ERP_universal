package com.erp.common.client.internal;

import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.http.HttpStatus;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.client.RestTemplate;

import java.lang.reflect.Method;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.time.Duration;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;

class SaasInternalClientConfigTest {
    @Test
    void shouldBuildDedicatedLoadBalancedRestTemplateWithFiniteTimeouts() throws Exception {
        RestTemplateBuilder builder = Mockito.mock(RestTemplateBuilder.class, Mockito.RETURNS_SELF);
        RestTemplate template = new RestTemplate();
        Mockito.when(builder.build()).thenReturn(template);
        InternalSystemClientProperties properties = new InternalSystemClientProperties();

        RestTemplate actual = new SaasInternalClientConfig().saasInternalRestTemplate(builder, properties);

        Assertions.assertSame(template, actual);
        @SuppressWarnings({"rawtypes", "unchecked"})
        ArgumentCaptor<Supplier<ClientHttpRequestFactory>> requestFactoryCaptor = (ArgumentCaptor) ArgumentCaptor
                .forClass(Supplier.class);
        Mockito.verify(builder).requestFactory(requestFactoryCaptor.capture());
        Assertions.assertInstanceOf(JdkClientHttpRequestFactory.class, requestFactoryCaptor.getValue().get());
        Mockito.verify(builder, Mockito.never()).setConnectTimeout(Mockito.any(Duration.class));
        Mockito.verify(builder, Mockito.never()).setReadTimeout(Mockito.any(Duration.class));
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

    @Test
    void shouldNeverFollowRedirectsWithRealHttpTransport() throws Exception {
        AtomicInteger targetHits = new AtomicInteger();
        HttpServer server = HttpServer.create(new InetSocketAddress(InetAddress.getLoopbackAddress(), 0), 0);
        server.createContext("/redirect", exchange -> {
            exchange.getResponseHeaders().add("Location", "/target");
            exchange.sendResponseHeaders(302, -1);
            exchange.close();
        });
        server.createContext("/target", exchange -> {
            targetHits.incrementAndGet();
            exchange.sendResponseHeaders(200, -1);
            exchange.close();
        });
        server.start();

        try {
            RestTemplate template = new SaasInternalClientConfig().saasInternalRestTemplate(
                    new RestTemplateBuilder(), new InternalSystemClientProperties());
            String redirectUrl = "http://" + InetAddress.getLoopbackAddress().getHostAddress() + ":"
                    + server.getAddress().getPort() + "/redirect";

            Assertions.assertThrows(RestClientResponseException.class,
                    () -> template.getForEntity(redirectUrl, Void.class));
            Assertions.assertEquals(0, targetHits.get());
        } finally {
            server.stop(0);
        }
    }
}

package com.erp.saas.control;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.core.annotation.AnnotatedElementUtils;

import javax.sql.DataSource;
import java.time.Clock;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(properties = {
        "spring.cloud.nacos.discovery.enabled=false",
        "spring.cloud.nacos.discovery.register-enabled=false",
        "spring.cloud.nacos.config.enabled=false",
        "spring.cloud.discovery.enabled=false",
        "erp.saas.sql.upgrade.enabled=false",
        "erp.saas.schema-validation.enabled=false",
        "erp.internal.auth-signature-secret=test-only-internal-secret"
})
class SaasControlApplicationTest {

    @MockBean
    private DataSource dataSource;

    @Autowired
    private Clock clock;

    @Test
    void shouldBeSpringBootDiscoveryApplication() {
        assertThat(AnnotatedElementUtils.hasAnnotation(SaasControlApplication.class, SpringBootApplication.class))
                .isTrue();
        assertThat(AnnotatedElementUtils.hasAnnotation(SaasControlApplication.class, EnableDiscoveryClient.class))
                .isTrue();
        assertThat(AnnotatedElementUtils.hasAnnotation(SaasControlApplication.class, SpringBootConfiguration.class))
                .isTrue();
    }

    @Test
    void shouldExposeUtcClock() {
        assertThat(clock.getZone()).isEqualTo(Clock.systemUTC().getZone());
    }
}

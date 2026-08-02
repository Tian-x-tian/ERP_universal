package com.erp.saas.control.mapper;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.ApplicationContext;

import javax.sql.DataSource;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(properties = {
        "spring.cloud.nacos.discovery.enabled=false", "spring.cloud.nacos.discovery.register-enabled=false",
        "spring.cloud.nacos.config.enabled=false", "spring.cloud.discovery.enabled=false",
        "erp.saas.sql.upgrade.enabled=false", "erp.saas.schema-validation.enabled=false",
        "erp.internal.auth-signature-secret=test-only-internal-secret",
        "erp.saas.snapshot.signing.key-id=test-primary",
        "erp.saas.snapshot.signing.secret=0123456789abcdef0123456789abcdef"
})
class SaasControlMapperRegistrationTest {
    @MockBean DataSource dataSource;
    @Autowired ApplicationContext context;

    @Test void shouldRegisterAllCatalogMappers() {
        assertThat(new Class<?>[]{SaasTenantMapper.class, SaasDomainMapper.class, SaasPlanMapper.class,
                SaasFeatureMapper.class, SaasPlanFeatureMapper.class, SaasPlanQuotaMapper.class,
                SaasSubscriptionMapper.class, SaasTenantFeatureOverrideMapper.class,
                SaasTenantQuotaOverrideMapper.class, SaasDeploymentMapper.class,
                SaasEntitlementSnapshotMapper.class})
                .allSatisfy(type -> assertThat(context.getBean(type)).isNotNull());
    }
}

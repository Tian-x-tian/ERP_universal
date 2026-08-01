package com.erp.system.config;

import com.erp.common.mybatis.TenantGlobalTables;
import com.erp.common.mybatis.TenantSchemaReadinessFilter;
import com.erp.common.mybatis.TenantSchemaReadinessGate;
import com.erp.common.mybatis.TenantSchemaValidationRunner;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import javax.sql.DataSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

/**
 * Tenant schema startup bean configuration tests.
 */
class MyBatisPlusConfigTest {
    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withUserConfiguration(MyBatisPlusConfig.class)
            .withBean(DataSource.class, () -> mock(DataSource.class));

    @Test
    void shouldRegisterSchemaValidationByDefault() {
        contextRunner.run(context -> {
            assertThat(context).hasSingleBean(TenantSchemaReadinessGate.class);
            assertThat(context).hasSingleBean(TenantSchemaReadinessFilter.class);
            assertThat(context).hasSingleBean(TenantSchemaValidationRunner.class);
        });
    }

    @Test
    void shouldDisableSchemaValidationWhenExplicitlyConfigured() {
        contextRunner
                .withPropertyValues("erp.tenant.schema-validation.enabled=false")
                .run(context -> {
                    assertThat(context).doesNotHaveBean(TenantSchemaReadinessGate.class);
                    assertThat(context).doesNotHaveBean(TenantSchemaReadinessFilter.class);
                    assertThat(context).doesNotHaveBean(TenantSchemaValidationRunner.class);
                });
    }

    @Test
    void shouldUseSharedGlobalTableAllowlist() {
        MyBatisPlusConfig configuration = new MyBatisPlusConfig(mock(DataSource.class));

        assertThat(configuration.globalTableCandidates()).isSameAs(TenantGlobalTables.TABLES);
    }
}

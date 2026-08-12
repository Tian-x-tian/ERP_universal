package com.erp.system.config;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.core.Ordered;

import static org.mockito.Mockito.mock;

/**
 * System schema upgrade runner tests.
 */
class SystemSqlUpgradeRunnerTest {

    @Test
    void shouldRunBeforeTenantSchemaValidation() {
        SystemSqlUpgradeRunner runner = new SystemSqlUpgradeRunner(mock(javax.sql.DataSource.class));

        Assertions.assertEquals(100, ((Ordered) runner).getOrder());
    }
}

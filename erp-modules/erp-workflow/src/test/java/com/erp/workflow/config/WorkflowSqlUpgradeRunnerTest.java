package com.erp.workflow.config;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.core.Ordered;

import static org.mockito.Mockito.mock;

/**
 * Workflow schema upgrade runner tests.
 */
class WorkflowSqlUpgradeRunnerTest {

    @Test
    void shouldRunBeforeTenantSchemaValidation() {
        WorkflowSqlUpgradeRunner runner = new WorkflowSqlUpgradeRunner(mock(javax.sql.DataSource.class));

        Assertions.assertEquals(100, ((Ordered) runner).getOrder());
    }
}

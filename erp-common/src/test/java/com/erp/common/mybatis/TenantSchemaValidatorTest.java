package com.erp.common.mybatis;

import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Tenant schema validator tests.
 */
class TenantSchemaValidatorTest {
    private static final Set<String> GLOBAL_TABLES = Set.of("sys_tenant", "sys_menu", "sys_config");

    @Test
    void shouldAcceptTenantTablesAndExplicitGlobalTables() {
        JdbcTemplate jdbcTemplate = mock(JdbcTemplate.class);
        when(jdbcTemplate.queryForList(org.mockito.ArgumentMatchers.anyString())).thenReturn(List.of(
                row("biz_order", 1),
                row("sys_tenant", 0)));

        TenantSchemaValidator validator = new TenantSchemaValidator(jdbcTemplate, GLOBAL_TABLES);

        assertDoesNotThrow(validator::validate);
        verify(jdbcTemplate).queryForList(contains("information_schema.TABLES"));
    }

    @Test
    void shouldRejectEveryUnexpectedTableWithoutTenantColumn() {
        JdbcTemplate jdbcTemplate = mock(JdbcTemplate.class);
        when(jdbcTemplate.queryForList(org.mockito.ArgumentMatchers.anyString())).thenReturn(List.of(
                row("z_audit_log", 0),
                row("sys_tenant", 0),
                row("a_business_log", 0)));
        TenantSchemaValidator validator = new TenantSchemaValidator(jdbcTemplate, GLOBAL_TABLES);

        IllegalStateException exception = assertThrows(IllegalStateException.class, validator::validate);

        assertEquals("Tenant schema validation failed. Missing tenant_id column: a_business_log, z_audit_log.",
                exception.getMessage());
    }

    @Test
    void shouldNormalizeMetadataTableNames() {
        JdbcTemplate jdbcTemplate = mock(JdbcTemplate.class);
        when(jdbcTemplate.queryForList(org.mockito.ArgumentMatchers.anyString())).thenReturn(List.of(
                row("`erp_system`.`SYS_CONFIG`", 0),
                row("ERP_SYSTEM.BIZ_ORDER", 1)));
        TenantSchemaValidator validator = new TenantSchemaValidator(jdbcTemplate, GLOBAL_TABLES);

        assertDoesNotThrow(validator::validate);
    }

    @Test
    void shouldExposeValidationOrderAfterUpgradeRunners() {
        TenantSchemaValidationRunner runner = new TenantSchemaValidationRunner(mock(TenantSchemaValidator.class));

        assertEquals(200, runner.getOrder());
    }

    private Map<String, Object> row(String tableName, int hasTenantId) {
        return Map.of("table_name", tableName, "has_tenant_id", hasTenantId);
    }
}

package com.erp.common.mybatis;

import org.junit.jupiter.api.Test;
import org.springframework.boot.DefaultApplicationArguments;
import org.springframework.core.Ordered;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import jakarta.servlet.FilterChain;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
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
        TenantSchemaValidationRunner runner = new TenantSchemaValidationRunner(
                mock(TenantSchemaValidator.class), new TenantSchemaReadinessGate());

        assertEquals(200, runner.getOrder());
    }

    @Test
    void shouldKeepTrafficClosedUntilValidationSucceeds() throws Exception {
        TenantSchemaValidator validator = mock(TenantSchemaValidator.class);
        TenantSchemaReadinessGate gate = new TenantSchemaReadinessGate();
        TenantSchemaValidationRunner runner = new TenantSchemaValidationRunner(validator, gate);
        doThrow(new IllegalStateException("invalid schema"))
                .doNothing()
                .when(validator).validate();

        assertThrows(IllegalStateException.class,
                () -> runner.run(new DefaultApplicationArguments(new String[0])));
        assertFalse(gate.isOpen());

        runner.run(new DefaultApplicationArguments(new String[0]));

        assertTrue(gate.isOpen());
    }

    @Test
    void shouldRejectTrafficWhileSchemaIsUnvalidated() throws Exception {
        TenantSchemaReadinessGate gate = new TenantSchemaReadinessGate();
        TenantSchemaReadinessFilter filter = new TenantSchemaReadinessFilter(gate);
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/orders");
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain filterChain = mock(FilterChain.class);

        filter.doFilter(request, response, filterChain);

        assertEquals(Ordered.HIGHEST_PRECEDENCE, filter.getOrder());
        assertEquals(503, response.getStatus());
        verifyNoInteractions(filterChain);
    }

    @Test
    void shouldAllowOnlyLivenessWhileSchemaIsUnvalidated() throws Exception {
        TenantSchemaReadinessGate gate = new TenantSchemaReadinessGate();
        TenantSchemaReadinessFilter filter = new TenantSchemaReadinessFilter(gate);
        FilterChain livenessChain = mock(FilterChain.class);
        MockHttpServletRequest livenessRequest = new MockHttpServletRequest(
                "GET", "/actuator/health/liveness");
        MockHttpServletResponse livenessResponse = new MockHttpServletResponse();

        filter.doFilter(livenessRequest, livenessResponse, livenessChain);

        verify(livenessChain).doFilter(livenessRequest, livenessResponse);

        FilterChain readinessChain = mock(FilterChain.class);
        MockHttpServletRequest readinessRequest = new MockHttpServletRequest(
                "GET", "/actuator/health/readiness");
        MockHttpServletResponse readinessResponse = new MockHttpServletResponse();

        filter.doFilter(readinessRequest, readinessResponse, readinessChain);

        assertEquals(503, readinessResponse.getStatus());
        verifyNoInteractions(readinessChain);
    }

    private Map<String, Object> row(String tableName, int hasTenantId) {
        return Map.of("table_name", tableName, "has_tenant_id", hasTenantId);
    }
}

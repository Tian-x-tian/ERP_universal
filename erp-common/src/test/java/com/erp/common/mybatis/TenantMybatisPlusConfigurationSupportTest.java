package com.erp.common.mybatis;

import com.baomidou.mybatisplus.extension.plugins.handler.TenantLineHandler;
import com.baomidou.mybatisplus.extension.plugins.inner.TenantLineInnerInterceptor;
import com.erp.common.core.context.TenantContextHolder;
import org.junit.jupiter.api.Test;

import javax.sql.DataSource;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;

/**
 * Tenant MyBatis Plus configuration tests.
 */
class TenantMybatisPlusConfigurationSupportTest {

    @Test
    void shouldIgnoreOnlyExplicitGlobalTables() {
        DataSource dataSource = mock(DataSource.class);
        TenantLineHandler handler = new TestConfiguration(dataSource).tenantLineHandler();

        assertTrue(handler.ignoreTable("sys_tenant"));
        assertTrue(handler.ignoreTable("sys_menu"));
        assertTrue(handler.ignoreTable("`SYS_MENU`"));
        assertFalse(handler.ignoreTable("biz_order"));

        verifyNoInteractions(dataSource);
    }

    @Test
    void shouldRejectQualifiedGlobalTableNames() {
        DataSource dataSource = mock(DataSource.class);
        TenantLineHandler handler = new TestConfiguration(dataSource).tenantLineHandler();

        assertFalse(handler.ignoreTable("other_schema.sys_menu"));
        assertFalse(handler.ignoreTable("`other_schema`.`sys_menu`"));

        verifyNoInteractions(dataSource);
    }

    @Test
    void shouldRejectQualifiedSelectThroughRealParser() {
        assertQualifiedSqlRejected("SELECT * FROM other_schema.sys_menu");
    }

    @Test
    void shouldRejectQualifiedInsertThroughRealParser() {
        assertQualifiedSqlRejected(
                "INSERT INTO other_schema.sys_menu (menu_id) VALUES (1)");
    }

    @Test
    void shouldRejectQualifiedUpdateThroughRealParser() {
        assertQualifiedSqlRejected(
                "UPDATE other_schema.sys_menu SET menu_name = 'updated'");
    }

    @Test
    void shouldRejectQualifiedDeleteThroughRealParser() {
        assertQualifiedSqlRejected(
                "DELETE FROM other_schema.sys_menu WHERE menu_id = 1");
    }

    @Test
    void shouldRejectQualifiedTableInJoinOnSubquery() {
        assertQualifiedSqlRejected(
                "SELECT m.menu_id FROM sys_menu m JOIN sys_tenant t "
                        + "ON EXISTS (SELECT 1 FROM other_schema.sys_menu q "
                        + "WHERE q.menu_id = m.menu_id)");
    }

    @Test
    void shouldRejectQualifiedTableInNestedCteAndUnion() {
        assertQualifiedSqlRejected(
                "WITH tenant_menu AS ("
                        + "SELECT m.menu_id FROM sys_menu m JOIN sys_tenant t "
                        + "ON EXISTS (SELECT 1 FROM other_schema.sys_menu q "
                        + "WHERE q.menu_id = m.menu_id)) "
                        + "SELECT menu_id FROM tenant_menu "
                        + "UNION ALL SELECT menu_id FROM sys_menu");
    }

    @Test
    void shouldRejectQualifiedTableInInsertSelectAndDuplicateUpdate() {
        assertQualifiedSqlRejected(
                "INSERT INTO sys_menu (menu_id, menu_name) "
                        + "SELECT menu_id, menu_name FROM sys_menu "
                        + "ON DUPLICATE KEY UPDATE menu_name = "
                        + "(SELECT menu_name FROM other_schema.sys_menu LIMIT 1)");
    }

    @Test
    void shouldRejectQualifiedTableInUpdateAndDeleteJoinOn() {
        assertQualifiedSqlRejected(
                "UPDATE sys_menu m JOIN sys_tenant t "
                        + "ON EXISTS (SELECT 1 FROM other_schema.sys_menu q "
                        + "WHERE q.menu_id = m.menu_id) "
                        + "SET m.menu_name = 'updated'");
        assertQualifiedSqlRejected(
                "DELETE m FROM sys_menu m JOIN sys_tenant t "
                        + "ON EXISTS (SELECT 1 FROM other_schema.sys_menu q "
                        + "WHERE q.menu_id = m.menu_id)");
    }

    @Test
    void shouldAllowQuotedLocalTableNameContainingDot() {
        DataSource dataSource = mock(DataSource.class);
        TenantLineInnerInterceptor interceptor = new TestConfiguration(dataSource).tenantLineInterceptor();

        TenantContextHolder.setTenantId("tenant-a");
        try {
            assertDoesNotThrow(() -> interceptor.parserSingle("SELECT * FROM `local.table`", null));
        } finally {
            TenantContextHolder.clear();
        }

        verifyNoInteractions(dataSource);
    }

    @Test
    void shouldExposeExactImmutableGlobalTableAllowlist() {
        Set<String> expectedTables = Set.of(
                "sys_tenant",
                "sys_menu",
                "sys_dict_type",
                "sys_dict_data",
                "sys_config",
                "sys_sql_upgrade_log",
                "biz_sql_upgrade_log");

        assertEquals(expectedTables, TenantGlobalTables.TABLES);
        assertThrows(UnsupportedOperationException.class,
                () -> TenantGlobalTables.TABLES.add("unexpected_table"));
    }

    @Test
    void shouldApplyTenantRuleToUnknownTableWithoutQueryingMetadata() {
        DataSource dataSource = mock(DataSource.class);
        TenantLineHandler handler = new TestConfiguration(dataSource).tenantLineHandler();

        assertFalse(handler.ignoreTable("unexpected_table"));

        verifyNoInteractions(dataSource);
    }

    @Test
    void shouldNotIgnoreNullOrBlankTableNames() {
        DataSource dataSource = mock(DataSource.class);
        TenantLineHandler handler = new TestConfiguration(dataSource).tenantLineHandler();

        assertFalse(handler.ignoreTable(null));
        assertFalse(handler.ignoreTable(""));
        assertFalse(handler.ignoreTable("   "));

        verifyNoInteractions(dataSource);
    }

    private void assertQualifiedSqlRejected(String sql) {
        DataSource dataSource = mock(DataSource.class);
        TenantLineInnerInterceptor interceptor = new TestConfiguration(dataSource).tenantLineInterceptor();

        IllegalStateException exception = assertThrows(
                IllegalStateException.class, () -> interceptor.parserSingle(sql, null));

        assertTrue(exception.getMessage().contains("other_schema.sys_menu"));
        verifyNoInteractions(dataSource);
    }

    private static final class TestConfiguration extends TenantMybatisPlusConfigurationSupport {
        private TestConfiguration(DataSource dataSource) {
            super(dataSource);
        }

        private TenantLineHandler tenantLineHandler() {
            return buildTenantLineHandler();
        }

        private TenantLineInnerInterceptor tenantLineInterceptor() {
            return buildTenantLineInnerInterceptor();
        }

        @Override
        protected Set<String> globalTableCandidates() {
            return Set.of("sys_tenant", "sys_menu");
        }
    }
}

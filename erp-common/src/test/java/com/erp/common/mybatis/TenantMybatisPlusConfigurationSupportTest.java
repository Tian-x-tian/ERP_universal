package com.erp.common.mybatis;

import com.baomidou.mybatisplus.extension.plugins.handler.TenantLineHandler;
import org.junit.jupiter.api.Test;

import javax.sql.DataSource;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
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

    private static final class TestConfiguration extends TenantMybatisPlusConfigurationSupport {
        private TestConfiguration(DataSource dataSource) {
            super(dataSource);
        }

        private TenantLineHandler tenantLineHandler() {
            return buildTenantLineHandler();
        }

        @Override
        protected Set<String> globalTableCandidates() {
            return Set.of("sys_tenant", "sys_menu");
        }
    }
}

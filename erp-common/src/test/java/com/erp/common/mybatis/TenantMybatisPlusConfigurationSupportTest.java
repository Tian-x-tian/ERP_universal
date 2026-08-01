package com.erp.common.mybatis;

import com.baomidou.mybatisplus.extension.plugins.handler.TenantLineHandler;
import org.junit.jupiter.api.Test;

import javax.sql.DataSource;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertFalse;
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
        assertFalse(handler.ignoreTable("biz_order"));

        verifyNoInteractions(dataSource);
    }

    @Test
    void shouldNormalizeQualifiedGlobalTableNames() {
        DataSource dataSource = mock(DataSource.class);
        TenantLineHandler handler = new TestConfiguration(dataSource).tenantLineHandler();

        assertTrue(handler.ignoreTable("`erp_system`.`sys_tenant`"));
        assertTrue(handler.ignoreTable("ERP_SYSTEM.SYS_MENU"));

        verifyNoInteractions(dataSource);
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

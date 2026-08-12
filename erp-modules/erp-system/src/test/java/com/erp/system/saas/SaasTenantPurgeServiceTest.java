package com.erp.system.saas;

import com.erp.common.core.context.TenantContextHolder;
import com.erp.common.client.internal.InternalBusinessClient;
import com.erp.saas.contract.model.SaasTenantPurgeRequest;
import com.erp.saas.contract.model.SaasTenantPurgeResult;
import com.erp.saas.contract.model.SaasTenantStoragePurgeResult;
import com.erp.system.saas.impl.SaasTenantDatabasePurgeExecutorImpl;
import com.erp.system.saas.impl.SaasTenantPurgeServiceImpl;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class SaasTenantPurgeServiceTest {
    private final JdbcTemplate jdbcTemplate = mock(JdbcTemplate.class);
    private final SaasTenantDatabasePurgeExecutor databaseExecutor =
            new SaasTenantDatabasePurgeExecutorImpl(jdbcTemplate);

    @AfterEach
    void clearContext() {
        TenantContextHolder.clear();
    }

    @Test
    void shouldDeleteEveryTenantScopedTableWithTenantRegistryLast() {
        when(jdbcTemplate.queryForList(SaasTenantDatabasePurgeExecutorImpl.TENANT_TABLES_SQL, String.class))
                .thenReturn(List.of("sys_tenant", "biz_order", "sys_user"));
        when(jdbcTemplate.update("DELETE FROM `biz_order` WHERE tenant_id = ?", "tenant-a")).thenReturn(3);
        when(jdbcTemplate.update("DELETE FROM `sys_user` WHERE tenant_id = ?", "tenant-a")).thenReturn(2);
        when(jdbcTemplate.update("DELETE FROM `sys_tenant` WHERE tenant_id = ?", "tenant-a")).thenReturn(1);
        var request = new SaasTenantPurgeRequest("purge-001", "tenant-a", "tenant-a");

        var result = databaseExecutor.purgeDatabase(request);

        assertThat(result.getRowsDeleted()).isEqualTo(6);
        assertThat(result.getTablesProcessed()).isEqualTo(3);
        var ordered = inOrder(jdbcTemplate);
        ordered.verify(jdbcTemplate).execute("SET FOREIGN_KEY_CHECKS = 0");
        ordered.verify(jdbcTemplate).update("DELETE FROM `biz_order` WHERE tenant_id = ?", "tenant-a");
        ordered.verify(jdbcTemplate).update("DELETE FROM `sys_user` WHERE tenant_id = ?", "tenant-a");
        ordered.verify(jdbcTemplate).update("DELETE FROM `sys_tenant` WHERE tenant_id = ?", "tenant-a");
        ordered.verify(jdbcTemplate).execute("SET FOREIGN_KEY_CHECKS = 1");
    }

    @Test
    void shouldProtectPlatformTenantAndRequireTypedConfirmation() {
        assertThatThrownBy(() -> databaseExecutor.purgeDatabase(new SaasTenantPurgeRequest(
                "purge-001", "000000", "000000"))).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> databaseExecutor.purgeDatabase(new SaasTenantPurgeRequest(
                "purge-001", "tenant-a", "tenant-b"))).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void shouldDeleteStorageBeforeStartingDatabasePurge() {
        InternalBusinessClient businessClient = mock(InternalBusinessClient.class);
        SaasTenantDatabasePurgeExecutor executor = mock(SaasTenantDatabasePurgeExecutor.class);
        SaasTenantPurgeService service = new SaasTenantPurgeServiceImpl(businessClient, executor);
        SaasTenantPurgeRequest request = new SaasTenantPurgeRequest(
                "purge-001", "tenant-a", "tenant-a");
        when(businessClient.purgeSaasTenantStorage(request)).thenReturn(
                new SaasTenantStoragePurgeResult("purge-001", "tenant-a", 2, false));
        when(executor.purgeDatabase(request)).thenReturn(
                new SaasTenantPurgeResult("purge-001", "tenant-a", 5, 10L, false));

        SaasTenantPurgeResult result = service.purge(request);

        assertThat(result.getObjectsDeleted()).isEqualTo(2);
        var ordered = inOrder(businessClient, executor);
        ordered.verify(businessClient).purgeSaasTenantStorage(request);
        ordered.verify(executor).purgeDatabase(request);
    }

    @Test
    void shouldRestoreTenantContextEvenWhenForeignKeyChecksCannotBeRestored() {
        TenantContextHolder.setTenantId("platform-context");
        when(jdbcTemplate.queryForList(SaasTenantDatabasePurgeExecutorImpl.TENANT_TABLES_SQL, String.class))
                .thenReturn(List.of());
        doThrow(new IllegalStateException("connection lost"))
                .when(jdbcTemplate).execute("SET FOREIGN_KEY_CHECKS = 1");

        assertThatThrownBy(() -> databaseExecutor.purgeDatabase(new SaasTenantPurgeRequest(
                "purge-001", "tenant-a", "tenant-a")))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("connection lost");

        assertThat(TenantContextHolder.getTenantId()).isEqualTo("platform-context");
    }
}

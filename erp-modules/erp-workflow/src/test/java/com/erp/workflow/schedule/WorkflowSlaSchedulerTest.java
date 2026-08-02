package com.erp.workflow.schedule;

import com.erp.common.client.internal.InternalSystemClient;
import com.erp.saas.contract.model.SaasRuntimeAccess;
import com.erp.saas.contract.model.TenantLifecycleState;
import com.erp.workflow.service.ISysWorkflowEngineService;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.List;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class WorkflowSlaSchedulerTest {
    @Test
    void shouldSkipReadOnlyTenantWorkflowMutations() {
        JdbcTemplate jdbcTemplate = mock(JdbcTemplate.class);
        ISysWorkflowEngineService engineService = mock(ISysWorkflowEngineService.class);
        InternalSystemClient systemClient = mock(InternalSystemClient.class);
        when(jdbcTemplate.queryForList(
                "SELECT DISTINCT tenant_id FROM sys_wf_task WHERE due_time IS NOT NULL AND status IN ('0','1')",
                String.class)).thenReturn(List.of("tenant-a"));
        when(systemClient.getSaasRuntimeAccess()).thenReturn(new SaasRuntimeAccess(
                "tenant-a", TenantLifecycleState.READ_ONLY, false, true, false));
        WorkflowSlaScheduler scheduler = new WorkflowSlaScheduler(jdbcTemplate, engineService, systemClient);

        scheduler.scanAllTenantTasks();

        verify(engineService, never()).scanTimeoutTasks();
    }
}

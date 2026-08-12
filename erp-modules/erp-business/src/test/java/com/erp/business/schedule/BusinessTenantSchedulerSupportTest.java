package com.erp.business.schedule;

import com.erp.common.client.internal.InternalSystemClient;
import com.erp.common.core.context.TenantContextHolder;
import com.erp.platform.contract.model.PlatformTenantView;
import com.erp.saas.contract.model.SaasRuntimeAccess;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.List;

import static org.mockito.Mockito.when;

/**
 * 业务模块租户调度支持单元测试。
 */
@ExtendWith(MockitoExtension.class)
class BusinessTenantSchedulerSupportTest {

    @Mock
    private InternalSystemClient internalSystemClient;

    /**
     * 清理租户上下文。
     */
    @AfterEach
    void tearDown() {
        TenantContextHolder.clear();
    }

    /**
     * 验证调度支持会按活动租户逐个设置租户上下文执行。
     */
    @Test
    void shouldExecuteTaskWithTenantContextForEachActiveTenant() {
        List<PlatformTenantView> tenantList = Arrays.asList(buildTenant(" 000001 "), buildTenant(""), buildTenant("000002"));
        when(internalSystemClient.listActiveTenants()).thenReturn(tenantList);
        when(internalSystemClient.getSaasRuntimeAccess()).thenReturn(writeAllowed("000001"), writeAllowed("000002"));
        BusinessTenantSchedulerSupport schedulerSupport = new BusinessTenantSchedulerSupport(internalSystemClient);
        StringBuilder executionLog = new StringBuilder();

        schedulerSupport.executeForEachActiveTenant("库存任务", tenantId -> executionLog
                .append(tenantId)
                .append(':')
                .append(TenantContextHolder.getTenantId())
                .append(';'));

        Assertions.assertEquals("000001:000001;000002:000002;", executionLog.toString());
        Assertions.assertNull(TenantContextHolder.getTenantId());
    }

    /**
     * 验证调度支持会在执行结束后恢复原始租户上下文。
     */
    @Test
    void shouldRestoreOriginalTenantContextAfterExecution() {
        when(internalSystemClient.listActiveTenants()).thenReturn(List.of(buildTenant("000003")));
        when(internalSystemClient.getSaasRuntimeAccess()).thenReturn(writeAllowed("000003"));
        BusinessTenantSchedulerSupport schedulerSupport = new BusinessTenantSchedulerSupport(internalSystemClient);
        TenantContextHolder.setTenantId("ORIGINAL");

        schedulerSupport.executeForEachActiveTenant("库存任务", tenantId ->
                Assertions.assertEquals("000003", TenantContextHolder.getTenantId()));

        Assertions.assertEquals("ORIGINAL", TenantContextHolder.getTenantId());
    }

    @Test
    void shouldSkipReadOnlyTenantTasks() {
        when(internalSystemClient.listActiveTenants()).thenReturn(
                List.of(buildTenant("active"), buildTenant("read-only")));
        when(internalSystemClient.getSaasRuntimeAccess()).thenReturn(
                writeAllowed("active"), new SaasRuntimeAccess(
                        "read-only", com.erp.saas.contract.model.TenantLifecycleState.READ_ONLY,
                        false, true, false));
        BusinessTenantSchedulerSupport schedulerSupport = new BusinessTenantSchedulerSupport(internalSystemClient);
        StringBuilder executionLog = new StringBuilder();

        schedulerSupport.executeForEachActiveTenant("库存任务", executionLog::append);

        Assertions.assertEquals("active", executionLog.toString());
    }

    /**
     * 构造租户只读投影。
     *
     * @param tenantId 租户编号
     * @return 租户投影
     */
    private PlatformTenantView buildTenant(String tenantId) {
        PlatformTenantView tenantView = new PlatformTenantView();
        tenantView.setTenantId(tenantId);
        return tenantView;
    }

    private SaasRuntimeAccess writeAllowed(String tenantId) {
        return new SaasRuntimeAccess(tenantId,
                com.erp.saas.contract.model.TenantLifecycleState.ACTIVE, false, true, true);
    }
}


package com.erp.business.schedule;

import com.erp.common.core.context.TenantContextHolder;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.Arrays;
import java.util.List;

import static org.mockito.Mockito.when;

/**
 * 业务模块租户调度支持单元测试。
 */
@ExtendWith(MockitoExtension.class)
class BusinessTenantSchedulerSupportTest {

    @Mock
    private JdbcTemplate jdbcTemplate;

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
        List<String> tenantIdList = Arrays.asList(" 000001 ", "", "000002");
        when(jdbcTemplate.queryForList(
                "SELECT tenant_id FROM sys_tenant WHERE status = '0' AND del_flag = '0' ORDER BY id",
                String.class)).thenReturn(tenantIdList);
        BusinessTenantSchedulerSupport schedulerSupport = new BusinessTenantSchedulerSupport(jdbcTemplate);
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
        when(jdbcTemplate.queryForList(
                "SELECT tenant_id FROM sys_tenant WHERE status = '0' AND del_flag = '0' ORDER BY id",
                String.class)).thenReturn(List.of("000003"));
        BusinessTenantSchedulerSupport schedulerSupport = new BusinessTenantSchedulerSupport(jdbcTemplate);
        TenantContextHolder.setTenantId("ORIGINAL");

        schedulerSupport.executeForEachActiveTenant("库存任务", tenantId ->
                Assertions.assertEquals("000003", TenantContextHolder.getTenantId()));

        Assertions.assertEquals("ORIGINAL", TenantContextHolder.getTenantId());
    }
}

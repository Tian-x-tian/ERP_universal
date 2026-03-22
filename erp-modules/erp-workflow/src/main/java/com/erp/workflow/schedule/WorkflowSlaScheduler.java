package com.erp.workflow.schedule;

import com.erp.common.core.context.TenantContextHolder;
import com.erp.workflow.service.ISysWorkflowEngineService;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.List;

/**
 * 流程 SLA 定时扫描任务。
 */
@Component
@ConditionalOnProperty(name = "erp.workflow.scheduler-enabled", havingValue = "true")
public class WorkflowSlaScheduler {

    private final JdbcTemplate jdbcTemplate;
    private final ISysWorkflowEngineService workflowEngineService;

    public WorkflowSlaScheduler(JdbcTemplate jdbcTemplate, ISysWorkflowEngineService workflowEngineService) {
        this.jdbcTemplate = jdbcTemplate;
        this.workflowEngineService = workflowEngineService;
    }

    /**
     * 每 5 分钟扫描一次存在待处理流程任务的租户。
     */
    @Scheduled(cron = "0 */5 * * * ?")
    public void scanAllTenantTasks() {
        List<String> tenantIdList = jdbcTemplate.queryForList(
                "SELECT DISTINCT tenant_id FROM sys_wf_task WHERE due_time IS NOT NULL AND status IN ('0','1')",
                String.class);
        for (String tenantId : tenantIdList) {
            if (!StringUtils.hasText(tenantId)) {
                continue;
            }
            try {
                TenantContextHolder.setTenantId(tenantId.trim());
                workflowEngineService.scanTimeoutTasks();
            } finally {
                TenantContextHolder.clear();
            }
        }
    }
}


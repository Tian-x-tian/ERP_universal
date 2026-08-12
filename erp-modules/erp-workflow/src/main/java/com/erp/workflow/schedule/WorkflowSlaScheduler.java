package com.erp.workflow.schedule;

import com.erp.common.core.context.TenantContextHolder;
import com.erp.common.client.internal.InternalSystemClient;
import com.erp.saas.contract.model.SaasRuntimeAccess;
import com.erp.workflow.service.ISysWorkflowEngineService;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * 流程 SLA 定时扫描任务。
 */
@Component
@ConditionalOnProperty(name = "erp.workflow.scheduler-enabled", havingValue = "true")
public class WorkflowSlaScheduler {

    private static final Logger log = LoggerFactory.getLogger(WorkflowSlaScheduler.class);

    private final JdbcTemplate jdbcTemplate;
    private final ISysWorkflowEngineService workflowEngineService;
    private final InternalSystemClient internalSystemClient;

    public WorkflowSlaScheduler(JdbcTemplate jdbcTemplate, ISysWorkflowEngineService workflowEngineService,
            InternalSystemClient internalSystemClient) {
        this.jdbcTemplate = jdbcTemplate;
        this.workflowEngineService = workflowEngineService;
        this.internalSystemClient = internalSystemClient;
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
                SaasRuntimeAccess access = internalSystemClient.getSaasRuntimeAccess();
                if (access == null || !tenantId.trim().equals(access.getTenantId())
                        || !access.isWriteAllowed()) {
                    log.info("流程 SLA 扫描任务已跳过，租户当前不允许业务写入，tenantId={}", tenantId.trim());
                    continue;
                }
                workflowEngineService.scanTimeoutTasks();
            } finally {
                TenantContextHolder.clear();
            }
        }
    }
}


package com.erp.business.schedule;

import com.erp.common.core.context.TenantContextHolder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.function.Consumer;

/**
 * 业务模块定时任务租户执行支持。
 */
@Component
public class BusinessTenantSchedulerSupport {

    private static final Logger log = LoggerFactory.getLogger(BusinessTenantSchedulerSupport.class);

    private final JdbcTemplate jdbcTemplate;

    public BusinessTenantSchedulerSupport(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    /**
     * 按活动租户逐个执行后台任务。
     *
     * @param taskName 任务名称
     * @param tenantAction 租户执行动作
     */
    public void executeForEachActiveTenant(String taskName, Consumer<String> tenantAction) {
        if (tenantAction == null) {
            return;
        }
        List<String> tenantIdList = jdbcTemplate.queryForList(
                "SELECT tenant_id FROM sys_tenant WHERE status = '0' AND del_flag = '0' ORDER BY id",
                String.class);
        String originalTenantId = TenantContextHolder.getTenantId();
        try {
            for (String tenantIdValue : tenantIdList) {
                if (!StringUtils.hasText(tenantIdValue)) {
                    continue;
                }
                String tenantId = tenantIdValue.trim();
                try {
                    TenantContextHolder.setTenantId(tenantId);
                    tenantAction.accept(tenantId);
                } catch (Exception ex) {
                    log.error("{} 执行失败，tenantId={}", taskName, tenantId, ex);
                } finally {
                    TenantContextHolder.clear();
                }
            }
        } finally {
            if (StringUtils.hasText(originalTenantId)) {
                TenantContextHolder.setTenantId(originalTenantId.trim());
                return;
            }
            TenantContextHolder.clear();
        }
    }
}

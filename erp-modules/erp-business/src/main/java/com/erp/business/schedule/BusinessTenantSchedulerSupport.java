package com.erp.business.schedule;

import com.erp.common.client.internal.InternalSystemClient;
import com.erp.common.core.context.TenantContextHolder;
import com.erp.platform.contract.model.PlatformTenantView;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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

    private final InternalSystemClient internalSystemClient;

    public BusinessTenantSchedulerSupport(InternalSystemClient internalSystemClient) {
        this.internalSystemClient = internalSystemClient;
    }

    /**
     * 按活动租户逐个执行后台任务。
     *
     * @param taskName     任务名称
     * @param tenantAction 租户执行动作
     */
    public void executeForEachActiveTenant(String taskName, Consumer<String> tenantAction) {
        if (tenantAction == null) {
            return;
        }
        List<PlatformTenantView> tenantList = internalSystemClient.listActiveTenants();
        String originalTenantId = TenantContextHolder.getTenantId();
        try {
            for (PlatformTenantView tenant : tenantList) {
                String tenantIdValue = tenant == null ? null : tenant.getTenantId();
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


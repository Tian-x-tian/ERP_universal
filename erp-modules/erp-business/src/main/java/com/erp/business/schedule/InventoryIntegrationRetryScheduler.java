package com.erp.business.schedule;

import com.erp.business.inventory.service.IInventoryIntegrationEventService;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 库存集成事件重试调度器。
 */
@Component
public class InventoryIntegrationRetryScheduler {

    private final IInventoryIntegrationEventService integrationEventService;
    private final BusinessTenantSchedulerSupport tenantSchedulerSupport;

    public InventoryIntegrationRetryScheduler(IInventoryIntegrationEventService integrationEventService,
            BusinessTenantSchedulerSupport tenantSchedulerSupport) {
        this.integrationEventService = integrationEventService;
        this.tenantSchedulerSupport = tenantSchedulerSupport;
    }

    /**
     * 定时重试待处理集成事件。
     */
    @Scheduled(cron = "0 0/15 * * * ?")
    public void retryPendingEvents() {
        tenantSchedulerSupport.executeForEachActiveTenant("库存集成事件重试任务",
                tenantId -> integrationEventService.retryPendingEvents());
    }
}

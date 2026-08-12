package com.erp.business.schedule;

import com.erp.business.inventory.service.IInventoryWarningService;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 库存预警扫描调度器。
 */
@Component
public class InventoryWarningScheduler {

    private final IInventoryWarningService warningService;
    private final BusinessTenantSchedulerSupport tenantSchedulerSupport;

    public InventoryWarningScheduler(IInventoryWarningService warningService,
            BusinessTenantSchedulerSupport tenantSchedulerSupport) {
        this.warningService = warningService;
        this.tenantSchedulerSupport = tenantSchedulerSupport;
    }

    /**
     * 定时扫描库存预警。
     */
    @Scheduled(cron = "0 0/30 * * * ?")
    public void scanWarnings() {
        tenantSchedulerSupport.executeForEachActiveTenant("库存预警扫描任务",
                tenantId -> warningService.scanWarnings());
    }
}

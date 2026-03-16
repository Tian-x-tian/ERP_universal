package com.erp.business.inventory.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.erp.business.inventory.domain.InventoryIntegrationEvent;
import com.erp.business.inventory.service.IInventoryIntegrationEventService;
import com.erp.common.core.domain.PageData;
import com.erp.common.core.domain.R;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 集成事件控制器。
 */
@RestController
@RequestMapping("/business/inventory/integration/event")
public class InventoryIntegrationEventController {

    private final IInventoryIntegrationEventService integrationEventService;

    public InventoryIntegrationEventController(IInventoryIntegrationEventService integrationEventService) {
        this.integrationEventService = integrationEventService;
    }

    /**
     * 查询集成事件分页。
     *
     * @param eventType 事件类型
     * @param eventStatus 事件状态
     * @param billNo 单据编号
     * @param pageNum 页码
     * @param pageSize 页长
     * @return 分页结果
     */
    @GetMapping("/list")
    @PreAuthorize("@ss.hasPermi('business:inventory:integration:list')")
    public R<PageData<InventoryIntegrationEvent>> list(@RequestParam(value = "eventType", required = false) String eventType,
            @RequestParam(value = "eventStatus", required = false) String eventStatus,
            @RequestParam(value = "billNo", required = false) String billNo,
            @RequestParam(value = "pageNum", required = false) Long pageNum,
            @RequestParam(value = "pageSize", required = false) Long pageSize) {
        Page<InventoryIntegrationEvent> page = integrationEventService.selectPage(eventType, eventStatus, billNo, pageNum, pageSize);
        return R.page(page.getRecords(), page.getCurrent(), page.getSize(), page.getTotal());
    }

    /**
     * 重放集成事件。
     *
     * @param eventId 事件ID
     * @return 处理结果
     */
    @PostMapping("/replay/{eventId}")
    @PreAuthorize("@ss.hasPermi('business:inventory:integration:replay')")
    public R<Boolean> replay(@PathVariable("eventId") Long eventId) {
        return integrationEventService.replay(eventId) ? R.success(true) : R.failed("重放集成事件失败");
    }
}

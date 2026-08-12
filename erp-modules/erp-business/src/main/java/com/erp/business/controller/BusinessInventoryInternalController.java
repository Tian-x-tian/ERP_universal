package com.erp.business.controller;

import com.erp.business.inventory.service.IInventoryWarehouseUsageService;
import com.erp.platform.contract.model.WarehouseReferenceUsage;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 业务模块库存内部接口控制层。
 *
 * <p>供其他服务通过 {@code erp-business-client} 调用，
 * 避免跨服务直接读取库存表。
 */
@RestController
@RequestMapping("/business/internal/inventory")
public class BusinessInventoryInternalController {

    private final IInventoryWarehouseUsageService warehouseUsageService;

    public BusinessInventoryInternalController(IInventoryWarehouseUsageService warehouseUsageService) {
        this.warehouseUsageService = warehouseUsageService;
    }

    /**
     * 查询仓库在库存模块中的引用占用情况。
     *
     * @param warehouseId 仓库ID
     * @return 占用情况
     */
    @GetMapping("/warehouses/{warehouseId}/reference-usage")
    public WarehouseReferenceUsage getWarehouseReferenceUsage(@PathVariable("warehouseId") Long warehouseId) {
        return warehouseUsageService.getWarehouseUsage(warehouseId);
    }
}

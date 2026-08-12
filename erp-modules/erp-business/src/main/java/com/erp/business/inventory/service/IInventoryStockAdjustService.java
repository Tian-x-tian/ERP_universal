package com.erp.business.inventory.service;

import com.erp.business.inventory.domain.InventoryStockAdjustOrder;
import com.erp.business.inventory.domain.InventoryStockAdjustOrderLine;

/**
 * 库存调整服务接口。
 */
public interface IInventoryStockAdjustService extends IInventoryOrderService<InventoryStockAdjustOrder, InventoryStockAdjustOrderLine> {
}

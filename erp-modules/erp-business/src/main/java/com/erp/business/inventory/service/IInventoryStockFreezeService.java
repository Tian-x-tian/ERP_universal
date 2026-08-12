package com.erp.business.inventory.service;

import com.erp.business.inventory.domain.InventoryStockFreezeOrder;
import com.erp.business.inventory.domain.InventoryStockFreezeOrderLine;

/**
 * 冻结解冻服务接口。
 */
public interface IInventoryStockFreezeService extends IInventoryOrderService<InventoryStockFreezeOrder, InventoryStockFreezeOrderLine> {
}

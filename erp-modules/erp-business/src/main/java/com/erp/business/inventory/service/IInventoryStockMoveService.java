package com.erp.business.inventory.service;

import com.erp.business.inventory.domain.InventoryStockMoveOrder;
import com.erp.business.inventory.domain.InventoryStockMoveOrderLine;

/**
 * 移库服务接口。
 */
public interface IInventoryStockMoveService extends IInventoryOrderService<InventoryStockMoveOrder, InventoryStockMoveOrderLine> {
}

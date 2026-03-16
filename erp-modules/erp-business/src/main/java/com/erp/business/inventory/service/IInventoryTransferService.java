package com.erp.business.inventory.service;

import com.erp.business.inventory.domain.InventoryTransferOrder;
import com.erp.business.inventory.domain.InventoryTransferOrderLine;

/**
 * 调拨服务接口。
 */
public interface IInventoryTransferService extends IInventoryOrderService<InventoryTransferOrder, InventoryTransferOrderLine> {
}

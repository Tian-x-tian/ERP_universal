package com.erp.business.inventory.service;

import com.erp.business.inventory.domain.InventoryStocktakeOrder;
import com.erp.business.inventory.domain.InventoryStocktakeOrderLine;

/**
 * 盘点服务接口。
 */
public interface IInventoryStocktakeService extends IInventoryOrderService<InventoryStocktakeOrder, InventoryStocktakeOrderLine> {

    /**
     * 确认盘点差异。
     *
     * @param orderId 单据ID
     * @return true 表示成功
     */
    boolean confirm(Long orderId);
}

package com.erp.business.inventory.domain;

import com.baomidou.mybatisplus.annotation.TableName;

/**
 * 库存调整单头。
 */
@TableName("inv_stock_adjust_order")
public class InventoryStockAdjustOrder extends AbstractInventoryOrder<InventoryStockAdjustOrderLine> {
    private static final long serialVersionUID = 1L;
}

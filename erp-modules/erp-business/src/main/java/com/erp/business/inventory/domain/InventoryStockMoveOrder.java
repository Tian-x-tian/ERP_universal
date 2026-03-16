package com.erp.business.inventory.domain;

import com.baomidou.mybatisplus.annotation.TableName;

/**
 * 移库单头。
 */
@TableName("inv_stock_move_order")
public class InventoryStockMoveOrder extends AbstractInventoryOrder<InventoryStockMoveOrderLine> {
    private static final long serialVersionUID = 1L;
}

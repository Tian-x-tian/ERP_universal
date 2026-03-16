package com.erp.business.inventory.domain;

import com.baomidou.mybatisplus.annotation.TableName;

/**
 * 冻结解冻单行。
 */
@TableName("inv_stock_freeze_order_line")
public class InventoryStockFreezeOrderLine extends AbstractInventoryOrderLine {
    private static final long serialVersionUID = 1L;
}

package com.erp.business.inventory.domain;

import com.baomidou.mybatisplus.annotation.TableName;

/**
 * 库存调整单行。
 */
@TableName("inv_stock_adjust_order_line")
public class InventoryStockAdjustOrderLine extends AbstractInventoryOrderLine {
    private static final long serialVersionUID = 1L;

    private String adjustType;

    public String getAdjustType() {
        return adjustType;
    }

    public void setAdjustType(String adjustType) {
        this.adjustType = adjustType;
    }
}

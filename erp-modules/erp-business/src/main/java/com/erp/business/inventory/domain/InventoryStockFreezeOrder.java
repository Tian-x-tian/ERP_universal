package com.erp.business.inventory.domain;

import com.baomidou.mybatisplus.annotation.TableName;

/**
 * 冻结解冻单头。
 */
@TableName("inv_stock_freeze_order")
public class InventoryStockFreezeOrder extends AbstractInventoryOrder<InventoryStockFreezeOrderLine> {
    private static final long serialVersionUID = 1L;

    private String operationType;

    public String getOperationType() {
        return operationType;
    }

    public void setOperationType(String operationType) {
        this.operationType = operationType;
    }
}

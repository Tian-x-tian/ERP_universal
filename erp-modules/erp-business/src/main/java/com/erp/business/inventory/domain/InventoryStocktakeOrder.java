package com.erp.business.inventory.domain;

import com.baomidou.mybatisplus.annotation.TableName;

/**
 * 盘点单头。
 */
@TableName("inv_stocktake_order")
public class InventoryStocktakeOrder extends AbstractInventoryOrder<InventoryStocktakeOrderLine> {
    private static final long serialVersionUID = 1L;

    private String stocktakeStage;

    public String getStocktakeStage() {
        return stocktakeStage;
    }

    public void setStocktakeStage(String stocktakeStage) {
        this.stocktakeStage = stocktakeStage;
    }
}

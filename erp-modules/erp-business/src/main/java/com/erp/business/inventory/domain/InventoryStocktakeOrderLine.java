package com.erp.business.inventory.domain;

import com.baomidou.mybatisplus.annotation.TableName;

import java.math.BigDecimal;

/**
 * 盘点单行。
 */
@TableName("inv_stocktake_order_line")
public class InventoryStocktakeOrderLine extends AbstractInventoryOrderLine {
    private static final long serialVersionUID = 1L;

    private BigDecimal snapshotQty;
    private BigDecimal countedQty;
    private BigDecimal diffQty;

    public BigDecimal getSnapshotQty() {
        return snapshotQty;
    }

    public void setSnapshotQty(BigDecimal snapshotQty) {
        this.snapshotQty = snapshotQty;
    }

    public BigDecimal getCountedQty() {
        return countedQty;
    }

    public void setCountedQty(BigDecimal countedQty) {
        this.countedQty = countedQty;
    }

    public BigDecimal getDiffQty() {
        return diffQty;
    }

    public void setDiffQty(BigDecimal diffQty) {
        this.diffQty = diffQty;
    }
}

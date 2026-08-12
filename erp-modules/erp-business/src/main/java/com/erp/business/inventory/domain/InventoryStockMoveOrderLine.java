package com.erp.business.inventory.domain;

import com.baomidou.mybatisplus.annotation.TableName;

/**
 * 移库单行。
 */
@TableName("inv_stock_move_order_line")
public class InventoryStockMoveOrderLine extends AbstractInventoryOrderLine {
    private static final long serialVersionUID = 1L;

    private Long targetAreaId;
    private Long targetLocationId;

    public Long getTargetAreaId() {
        return targetAreaId;
    }

    public void setTargetAreaId(Long targetAreaId) {
        this.targetAreaId = targetAreaId;
    }

    public Long getTargetLocationId() {
        return targetLocationId;
    }

    public void setTargetLocationId(Long targetLocationId) {
        this.targetLocationId = targetLocationId;
    }
}

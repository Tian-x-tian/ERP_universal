package com.erp.business.inventory.domain;

import com.baomidou.mybatisplus.annotation.TableName;

/**
 * 调拨单行。
 */
@TableName("inv_transfer_order_line")
public class InventoryTransferOrderLine extends AbstractInventoryOrderLine {
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

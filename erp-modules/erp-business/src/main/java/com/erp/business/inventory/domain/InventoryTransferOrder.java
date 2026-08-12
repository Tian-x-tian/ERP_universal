package com.erp.business.inventory.domain;

import com.baomidou.mybatisplus.annotation.TableName;

/**
 * 调拨单头。
 */
@TableName("inv_transfer_order")
public class InventoryTransferOrder extends AbstractInventoryOrder<InventoryTransferOrderLine> {
    private static final long serialVersionUID = 1L;

    private Long targetWarehouseId;

    public Long getTargetWarehouseId() {
        return targetWarehouseId;
    }

    public void setTargetWarehouseId(Long targetWarehouseId) {
        this.targetWarehouseId = targetWarehouseId;
    }
}

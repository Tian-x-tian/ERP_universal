package com.erp.platform.contract.model;

import java.io.Serializable;

/**
 * 仓库在库存模块中的引用占用情况。
 *
 * <p>主数据模块停用/删除仓库前需要确认库存侧没有残留占用。
 * 库存表由 erp-business 拥有，因此这些计数必须通过内部接口获取，
 * 不允许 erp-system 直接查询 {@code inv_*} 表。
 */
public class WarehouseReferenceUsage implements Serializable {
    private static final long serialVersionUID = 1L;

    /** 仓库ID */
    private Long warehouseId;

    /** 可用库存余额记录数 */
    private Long availableStockCount;

    /** 未完成入库单数量 */
    private Long openInboundOrderCount;

    /** 未完成出库单数量 */
    private Long openOutboundOrderCount;

    public Long getWarehouseId() {
        return warehouseId;
    }

    public void setWarehouseId(Long warehouseId) {
        this.warehouseId = warehouseId;
    }

    public Long getAvailableStockCount() {
        return availableStockCount;
    }

    public void setAvailableStockCount(Long availableStockCount) {
        this.availableStockCount = availableStockCount;
    }

    public Long getOpenInboundOrderCount() {
        return openInboundOrderCount;
    }

    public void setOpenInboundOrderCount(Long openInboundOrderCount) {
        this.openInboundOrderCount = openInboundOrderCount;
    }

    public Long getOpenOutboundOrderCount() {
        return openOutboundOrderCount;
    }

    public void setOpenOutboundOrderCount(Long openOutboundOrderCount) {
        this.openOutboundOrderCount = openOutboundOrderCount;
    }
}

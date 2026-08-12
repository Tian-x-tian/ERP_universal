package com.erp.business.inventory.domain.vo;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;

/**
 * 盘点差异报表行对象。
 */
public class InventoryStocktakeDiffReportRow implements Serializable {
    private static final long serialVersionUID = 1L;

    private Long orderId;
    private String billNo;
    private String status;
    private Long warehouseId;
    private Long itemId;
    private Long areaId;
    private Long locationId;
    private BigDecimal snapshotQty;
    private BigDecimal countedQty;
    private BigDecimal diffQty;
    private Date createTime;

    public Long getOrderId() {
        return orderId;
    }

    public void setOrderId(Long orderId) {
        this.orderId = orderId;
    }

    public String getBillNo() {
        return billNo;
    }

    public void setBillNo(String billNo) {
        this.billNo = billNo;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Long getWarehouseId() {
        return warehouseId;
    }

    public void setWarehouseId(Long warehouseId) {
        this.warehouseId = warehouseId;
    }

    public Long getItemId() {
        return itemId;
    }

    public void setItemId(Long itemId) {
        this.itemId = itemId;
    }

    public Long getAreaId() {
        return areaId;
    }

    public void setAreaId(Long areaId) {
        this.areaId = areaId;
    }

    public Long getLocationId() {
        return locationId;
    }

    public void setLocationId(Long locationId) {
        this.locationId = locationId;
    }

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

    public Date getCreateTime() {
        return createTime;
    }

    public void setCreateTime(Date createTime) {
        this.createTime = createTime;
    }
}

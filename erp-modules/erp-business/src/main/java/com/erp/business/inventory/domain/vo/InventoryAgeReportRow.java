package com.erp.business.inventory.domain.vo;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;

/**
 * 库龄报表行对象。
 */
public class InventoryAgeReportRow implements Serializable {
    private static final long serialVersionUID = 1L;

    private Long batchId;
    private Long warehouseId;
    private Long itemId;
    private String batchNo;
    private BigDecimal currentQty;
    private Date lastTxnTime;
    private Long ageDays;
    private String status;

    public Long getBatchId() {
        return batchId;
    }

    public void setBatchId(Long batchId) {
        this.batchId = batchId;
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

    public String getBatchNo() {
        return batchNo;
    }

    public void setBatchNo(String batchNo) {
        this.batchNo = batchNo;
    }

    public BigDecimal getCurrentQty() {
        return currentQty;
    }

    public void setCurrentQty(BigDecimal currentQty) {
        this.currentQty = currentQty;
    }

    public Date getLastTxnTime() {
        return lastTxnTime;
    }

    public void setLastTxnTime(Date lastTxnTime) {
        this.lastTxnTime = lastTxnTime;
    }

    public Long getAgeDays() {
        return ageDays;
    }

    public void setAgeDays(Long ageDays) {
        this.ageDays = ageDays;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}

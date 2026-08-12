package com.erp.business.inventory.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;

/**
 * 库存流水对象。
 */
@TableName("inv_stock_txn")
public class InventoryStockTxn implements Serializable {
    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long txnId;
    private String tenantId;
    private Long orgId;
    private Long warehouseId;
    private Long fromAreaId;
    private Long fromLocationId;
    private Long areaId;
    private Long locationId;
    private Long toAreaId;
    private Long toLocationId;
    private Long itemId;
    private String batchNo;
    private String serialNo;
    private String actionType;
    private String billType;
    private Long billId;
    private String billNo;
    private Integer lineNo;
    private String idempotencyNo;
    private String traceId;
    private BigDecimal beforeOnHandQty;
    private BigDecimal afterOnHandQty;
    private BigDecimal beforeAvailableQty;
    private BigDecimal afterAvailableQty;
    private BigDecimal changeQty;
    private String operator;
    private Date createTime;

    public Long getTxnId() {
        return txnId;
    }

    public void setTxnId(Long txnId) {
        this.txnId = txnId;
    }

    public String getTenantId() {
        return tenantId;
    }

    public void setTenantId(String tenantId) {
        this.tenantId = tenantId;
    }

    public Long getOrgId() {
        return orgId;
    }

    public void setOrgId(Long orgId) {
        this.orgId = orgId;
    }

    public Long getWarehouseId() {
        return warehouseId;
    }

    public void setWarehouseId(Long warehouseId) {
        this.warehouseId = warehouseId;
    }

    public Long getFromAreaId() {
        return fromAreaId;
    }

    public void setFromAreaId(Long fromAreaId) {
        this.fromAreaId = fromAreaId;
    }

    public Long getFromLocationId() {
        return fromLocationId;
    }

    public void setFromLocationId(Long fromLocationId) {
        this.fromLocationId = fromLocationId;
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

    public Long getToAreaId() {
        return toAreaId;
    }

    public void setToAreaId(Long toAreaId) {
        this.toAreaId = toAreaId;
    }

    public Long getToLocationId() {
        return toLocationId;
    }

    public void setToLocationId(Long toLocationId) {
        this.toLocationId = toLocationId;
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

    public String getSerialNo() {
        return serialNo;
    }

    public void setSerialNo(String serialNo) {
        this.serialNo = serialNo;
    }

    public String getActionType() {
        return actionType;
    }

    public void setActionType(String actionType) {
        this.actionType = actionType;
    }

    public String getBillType() {
        return billType;
    }

    public void setBillType(String billType) {
        this.billType = billType;
    }

    public Long getBillId() {
        return billId;
    }

    public void setBillId(Long billId) {
        this.billId = billId;
    }

    public String getBillNo() {
        return billNo;
    }

    public void setBillNo(String billNo) {
        this.billNo = billNo;
    }

    public Integer getLineNo() {
        return lineNo;
    }

    public void setLineNo(Integer lineNo) {
        this.lineNo = lineNo;
    }

    public String getIdempotencyNo() {
        return idempotencyNo;
    }

    public void setIdempotencyNo(String idempotencyNo) {
        this.idempotencyNo = idempotencyNo;
    }

    public String getTraceId() {
        return traceId;
    }

    public void setTraceId(String traceId) {
        this.traceId = traceId;
    }

    public BigDecimal getBeforeOnHandQty() {
        return beforeOnHandQty;
    }

    public void setBeforeOnHandQty(BigDecimal beforeOnHandQty) {
        this.beforeOnHandQty = beforeOnHandQty;
    }

    public BigDecimal getAfterOnHandQty() {
        return afterOnHandQty;
    }

    public void setAfterOnHandQty(BigDecimal afterOnHandQty) {
        this.afterOnHandQty = afterOnHandQty;
    }

    public BigDecimal getBeforeAvailableQty() {
        return beforeAvailableQty;
    }

    public void setBeforeAvailableQty(BigDecimal beforeAvailableQty) {
        this.beforeAvailableQty = beforeAvailableQty;
    }

    public BigDecimal getAfterAvailableQty() {
        return afterAvailableQty;
    }

    public void setAfterAvailableQty(BigDecimal afterAvailableQty) {
        this.afterAvailableQty = afterAvailableQty;
    }

    public BigDecimal getChangeQty() {
        return changeQty;
    }

    public void setChangeQty(BigDecimal changeQty) {
        this.changeQty = changeQty;
    }

    public String getOperator() {
        return operator;
    }

    public void setOperator(String operator) {
        this.operator = operator;
    }

    public Date getCreateTime() {
        return createTime;
    }

    public void setCreateTime(Date createTime) {
        this.createTime = createTime;
    }
}

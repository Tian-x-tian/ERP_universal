package com.erp.business.inventory.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.erp.common.mybatis.BaseAuditEntity;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * 入库单对象。
 */
@TableName("inv_inbound_order")
public class InventoryInboundOrder extends BaseAuditEntity implements Serializable {
    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long orderId;
    private String tenantId;
    private String billNo;
    private String billType;
    private String status;
    private Long orgId;
    private Long warehouseId;
    private String sourceOrderType;
    private Long sourceOrderId;
    private String sourceOrderNo;
    private String processKey;
    private String idempotencyNo;
    private Integer versionNo;
    private String remark;

    @TableField(exist = false)
    private List<InventoryInboundOrderLine> lines = new ArrayList<>();

    public Long getOrderId() {
        return orderId;
    }

    public void setOrderId(Long orderId) {
        this.orderId = orderId;
    }

    public String getTenantId() {
        return tenantId;
    }

    public void setTenantId(String tenantId) {
        this.tenantId = tenantId;
    }

    public String getBillNo() {
        return billNo;
    }

    public void setBillNo(String billNo) {
        this.billNo = billNo;
    }

    public String getBillType() {
        return billType;
    }

    public void setBillType(String billType) {
        this.billType = billType;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
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

    public String getSourceOrderType() {
        return sourceOrderType;
    }

    public void setSourceOrderType(String sourceOrderType) {
        this.sourceOrderType = sourceOrderType;
    }

    public Long getSourceOrderId() {
        return sourceOrderId;
    }

    public void setSourceOrderId(Long sourceOrderId) {
        this.sourceOrderId = sourceOrderId;
    }

    public String getSourceOrderNo() {
        return sourceOrderNo;
    }

    public void setSourceOrderNo(String sourceOrderNo) {
        this.sourceOrderNo = sourceOrderNo;
    }

    public String getProcessKey() {
        return processKey;
    }

    public void setProcessKey(String processKey) {
        this.processKey = processKey;
    }

    public String getIdempotencyNo() {
        return idempotencyNo;
    }

    public void setIdempotencyNo(String idempotencyNo) {
        this.idempotencyNo = idempotencyNo;
    }

    public Integer getVersionNo() {
        return versionNo;
    }

    public void setVersionNo(Integer versionNo) {
        this.versionNo = versionNo;
    }

    public String getRemark() {
        return remark;
    }

    public void setRemark(String remark) {
        this.remark = remark;
    }

    public List<InventoryInboundOrderLine> getLines() {
        return lines;
    }

    public void setLines(List<InventoryInboundOrderLine> lines) {
        this.lines = lines;
    }
}

package com.erp.business.inventory.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;

/**
 * 库存策略对象。
 */
@TableName("inv_stock_policy")
public class InventoryStockPolicy implements Serializable {
    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long policyId;
    private String tenantId;
    private Long orgId;
    private Long warehouseId;
    private Long itemId;
    private BigDecimal minQty;
    private BigDecimal maxQty;
    private BigDecimal safetyQty;
    private Integer expiryWarnDays;
    private String allowNegative;
    private String allowExpiredOutbound;
    private Integer stagnantDays;
    private String remark;
    private String createBy;
    private Date createTime;
    private String updateBy;
    private Date updateTime;

    public Long getPolicyId() {
        return policyId;
    }

    public void setPolicyId(Long policyId) {
        this.policyId = policyId;
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

    public Long getItemId() {
        return itemId;
    }

    public void setItemId(Long itemId) {
        this.itemId = itemId;
    }

    public BigDecimal getMinQty() {
        return minQty;
    }

    public void setMinQty(BigDecimal minQty) {
        this.minQty = minQty;
    }

    public BigDecimal getMaxQty() {
        return maxQty;
    }

    public void setMaxQty(BigDecimal maxQty) {
        this.maxQty = maxQty;
    }

    public BigDecimal getSafetyQty() {
        return safetyQty;
    }

    public void setSafetyQty(BigDecimal safetyQty) {
        this.safetyQty = safetyQty;
    }

    public Integer getExpiryWarnDays() {
        return expiryWarnDays;
    }

    public void setExpiryWarnDays(Integer expiryWarnDays) {
        this.expiryWarnDays = expiryWarnDays;
    }

    public String getAllowNegative() {
        return allowNegative;
    }

    public void setAllowNegative(String allowNegative) {
        this.allowNegative = allowNegative;
    }

    public String getAllowExpiredOutbound() {
        return allowExpiredOutbound;
    }

    public void setAllowExpiredOutbound(String allowExpiredOutbound) {
        this.allowExpiredOutbound = allowExpiredOutbound;
    }

    public Integer getStagnantDays() {
        return stagnantDays;
    }

    public void setStagnantDays(Integer stagnantDays) {
        this.stagnantDays = stagnantDays;
    }

    public String getRemark() {
        return remark;
    }

    public void setRemark(String remark) {
        this.remark = remark;
    }

    public String getCreateBy() {
        return createBy;
    }

    public void setCreateBy(String createBy) {
        this.createBy = createBy;
    }

    public Date getCreateTime() {
        return createTime;
    }

    public void setCreateTime(Date createTime) {
        this.createTime = createTime;
    }

    public String getUpdateBy() {
        return updateBy;
    }

    public void setUpdateBy(String updateBy) {
        this.updateBy = updateBy;
    }

    public Date getUpdateTime() {
        return updateTime;
    }

    public void setUpdateTime(Date updateTime) {
        this.updateTime = updateTime;
    }
}

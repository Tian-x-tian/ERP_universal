package com.erp.business.inventory.domain;

import java.io.Serializable;

/**
 * 物料主数据只读 DTO。
 */
public class MdmItem implements Serializable {
    private static final long serialVersionUID = 1L;

    private Long itemId;
    private String tenantId;
    private String itemCode;
    private String itemName;
    private Integer shelfLifeDays;
    private Integer defaultExpiryWarnDays;
    private String batchControl;
    private String serialControl;
    private String status;

    public Long getItemId() {
        return itemId;
    }

    public void setItemId(Long itemId) {
        this.itemId = itemId;
    }

    public String getTenantId() {
        return tenantId;
    }

    public void setTenantId(String tenantId) {
        this.tenantId = tenantId;
    }

    public String getItemCode() {
        return itemCode;
    }

    public void setItemCode(String itemCode) {
        this.itemCode = itemCode;
    }

    public String getItemName() {
        return itemName;
    }

    public void setItemName(String itemName) {
        this.itemName = itemName;
    }

    public Integer getShelfLifeDays() {
        return shelfLifeDays;
    }

    public void setShelfLifeDays(Integer shelfLifeDays) {
        this.shelfLifeDays = shelfLifeDays;
    }

    public Integer getDefaultExpiryWarnDays() {
        return defaultExpiryWarnDays;
    }

    public void setDefaultExpiryWarnDays(Integer defaultExpiryWarnDays) {
        this.defaultExpiryWarnDays = defaultExpiryWarnDays;
    }

    public String getBatchControl() {
        return batchControl;
    }

    public void setBatchControl(String batchControl) {
        this.batchControl = batchControl;
    }

    public String getSerialControl() {
        return serialControl;
    }

    public void setSerialControl(String serialControl) {
        this.serialControl = serialControl;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}

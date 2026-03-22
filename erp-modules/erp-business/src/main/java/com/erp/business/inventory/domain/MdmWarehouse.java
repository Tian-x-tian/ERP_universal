package com.erp.business.inventory.domain;

import java.io.Serializable;

/**
 * 仓库主数据只读 DTO。
 */
public class MdmWarehouse implements Serializable {
    private static final long serialVersionUID = 1L;

    private Long warehouseId;
    private Long accountingOrgId;
    private String allowNegativeStock;

    public Long getWarehouseId() {
        return warehouseId;
    }

    public void setWarehouseId(Long warehouseId) {
        this.warehouseId = warehouseId;
    }

    public Long getAccountingOrgId() {
        return accountingOrgId;
    }

    public void setAccountingOrgId(Long accountingOrgId) {
        this.accountingOrgId = accountingOrgId;
    }

    public String getAllowNegativeStock() {
        return allowNegativeStock;
    }

    public void setAllowNegativeStock(String allowNegativeStock) {
        this.allowNegativeStock = allowNegativeStock;
    }
}

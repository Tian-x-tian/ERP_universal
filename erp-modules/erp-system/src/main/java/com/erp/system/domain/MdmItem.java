package com.erp.system.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.erp.common.mybatis.BaseAuditEntity;

import java.io.Serializable;
import java.util.Date;

/**
 * MDM 物料主数据对象 mdm_item。
 */
@TableName("mdm_item")
public class MdmItem extends BaseAuditEntity implements Serializable {
    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long itemId;
    private String tenantId;
    private String itemCode;
    private String itemName;
    private String specModel;
    private String brand;
    private String itemType;
    private Long categoryId;
    private Long unitId;
    private String unitConvert;
    private Long taxRateId;
    private String barcode;
    private Integer shelfLifeDays;
    private Integer defaultExpiryWarnDays;
    private String batchControl;
    private String serialControl;
    private String costingMethod;
    private String status;
    private Date effectiveTime;
    private Integer versionNo;
    private String delFlag;
    private String remark;


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

    public String getSpecModel() {
        return specModel;
    }

    public void setSpecModel(String specModel) {
        this.specModel = specModel;
    }

    public String getBrand() {
        return brand;
    }

    public void setBrand(String brand) {
        this.brand = brand;
    }

    public String getItemType() {
        return itemType;
    }

    public void setItemType(String itemType) {
        this.itemType = itemType;
    }

    public Long getCategoryId() {
        return categoryId;
    }

    public void setCategoryId(Long categoryId) {
        this.categoryId = categoryId;
    }

    public Long getUnitId() {
        return unitId;
    }

    public void setUnitId(Long unitId) {
        this.unitId = unitId;
    }

    public String getUnitConvert() {
        return unitConvert;
    }

    public void setUnitConvert(String unitConvert) {
        this.unitConvert = unitConvert;
    }

    public Long getTaxRateId() {
        return taxRateId;
    }

    public void setTaxRateId(Long taxRateId) {
        this.taxRateId = taxRateId;
    }

    public String getBarcode() {
        return barcode;
    }

    public void setBarcode(String barcode) {
        this.barcode = barcode;
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

    public String getCostingMethod() {
        return costingMethod;
    }

    public void setCostingMethod(String costingMethod) {
        this.costingMethod = costingMethod;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Date getEffectiveTime() {
        return effectiveTime;
    }

    public void setEffectiveTime(Date effectiveTime) {
        this.effectiveTime = effectiveTime;
    }

    public Integer getVersionNo() {
        return versionNo;
    }

    public void setVersionNo(Integer versionNo) {
        this.versionNo = versionNo;
    }

    public String getDelFlag() {
        return delFlag;
    }

    public void setDelFlag(String delFlag) {
        this.delFlag = delFlag;
    }

    public String getRemark() {
        return remark;
    }

    public void setRemark(String remark) {
        this.remark = remark;
    }

}

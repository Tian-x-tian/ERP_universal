package com.erp.system.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.erp.system.sensitive.SensitiveField;
import com.erp.system.sensitive.SensitiveType;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;

/**
 * MDM 供应商主数据对象 mdm_supplier。
 */
@TableName("mdm_supplier")
public class MdmSupplier implements Serializable {
    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long supplierId;
    private String tenantId;
    private String supplierCode;
    private String supplierName;
    private String shortName;
    private String supplyCategory;
    @SensitiveField(SensitiveType.TAX_NO)
    private String taxNo;
    private String defaultCurrency;
    private BigDecimal defaultTaxRate;
    private Integer leadTimeDays;
    private String qualityLevel;
    @SensitiveField(SensitiveType.BANK_ACCOUNT)
    private String bankAccountInfo;
    private String contactName;
    @SensitiveField(SensitiveType.PHONE)
    private String contactPhone;
    @SensitiveField(SensitiveType.EMAIL)
    private String contactEmail;
    private String address;
    private String status;
    private Date effectiveTime;
    private Integer versionNo;
    private String delFlag;
    private String remark;
    private String createBy;
    private Date createTime;
    private String updateBy;
    private Date updateTime;


    public Long getSupplierId() {
        return supplierId;
    }

    public void setSupplierId(Long supplierId) {
        this.supplierId = supplierId;
    }

    public String getTenantId() {
        return tenantId;
    }

    public void setTenantId(String tenantId) {
        this.tenantId = tenantId;
    }

    public String getSupplierCode() {
        return supplierCode;
    }

    public void setSupplierCode(String supplierCode) {
        this.supplierCode = supplierCode;
    }

    public String getSupplierName() {
        return supplierName;
    }

    public void setSupplierName(String supplierName) {
        this.supplierName = supplierName;
    }

    public String getShortName() {
        return shortName;
    }

    public void setShortName(String shortName) {
        this.shortName = shortName;
    }

    public String getSupplyCategory() {
        return supplyCategory;
    }

    public void setSupplyCategory(String supplyCategory) {
        this.supplyCategory = supplyCategory;
    }

    public String getTaxNo() {
        return taxNo;
    }

    public void setTaxNo(String taxNo) {
        this.taxNo = taxNo;
    }

    public String getDefaultCurrency() {
        return defaultCurrency;
    }

    public void setDefaultCurrency(String defaultCurrency) {
        this.defaultCurrency = defaultCurrency;
    }

    public BigDecimal getDefaultTaxRate() {
        return defaultTaxRate;
    }

    public void setDefaultTaxRate(BigDecimal defaultTaxRate) {
        this.defaultTaxRate = defaultTaxRate;
    }

    public Integer getLeadTimeDays() {
        return leadTimeDays;
    }

    public void setLeadTimeDays(Integer leadTimeDays) {
        this.leadTimeDays = leadTimeDays;
    }

    public String getQualityLevel() {
        return qualityLevel;
    }

    public void setQualityLevel(String qualityLevel) {
        this.qualityLevel = qualityLevel;
    }

    public String getBankAccountInfo() {
        return bankAccountInfo;
    }

    public void setBankAccountInfo(String bankAccountInfo) {
        this.bankAccountInfo = bankAccountInfo;
    }

    public String getContactName() {
        return contactName;
    }

    public void setContactName(String contactName) {
        this.contactName = contactName;
    }

    public String getContactPhone() {
        return contactPhone;
    }

    public void setContactPhone(String contactPhone) {
        this.contactPhone = contactPhone;
    }

    public String getContactEmail() {
        return contactEmail;
    }

    public void setContactEmail(String contactEmail) {
        this.contactEmail = contactEmail;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
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

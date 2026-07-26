package com.erp.system.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.erp.common.mybatis.BaseAuditEntity;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;

/**
 * MDM 仓库主数据对象 mdm_warehouse。
 */
@TableName("mdm_warehouse")
public class MdmWarehouse extends BaseAuditEntity implements Serializable {
    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long warehouseId;
    private String tenantId;
    private String whCode;
    private String whName;
    private String whType;
    private Long orgId;
    private Long accountingOrgId;
    private String address;
    private Long managerEmpId;
    private String allowNegativeStock;
    private BigDecimal volumeCapacity;
    private BigDecimal weightCapacity;
    private String temperatureZone;
    private String hazardousFlag;
    private String locationCodePrefix;
    private String status;
    private Date effectiveTime;
    private Integer versionNo;
    private String delFlag;
    private String remark;


    public Long getWarehouseId() {
        return warehouseId;
    }

    public void setWarehouseId(Long warehouseId) {
        this.warehouseId = warehouseId;
    }

    public String getTenantId() {
        return tenantId;
    }

    public void setTenantId(String tenantId) {
        this.tenantId = tenantId;
    }

    public String getWhCode() {
        return whCode;
    }

    public void setWhCode(String whCode) {
        this.whCode = whCode;
    }

    public String getWhName() {
        return whName;
    }

    public void setWhName(String whName) {
        this.whName = whName;
    }

    public String getWhType() {
        return whType;
    }

    public void setWhType(String whType) {
        this.whType = whType;
    }

    public Long getOrgId() {
        return orgId;
    }

    public void setOrgId(Long orgId) {
        this.orgId = orgId;
    }

    public Long getAccountingOrgId() {
        return accountingOrgId;
    }

    public void setAccountingOrgId(Long accountingOrgId) {
        this.accountingOrgId = accountingOrgId;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public Long getManagerEmpId() {
        return managerEmpId;
    }

    public void setManagerEmpId(Long managerEmpId) {
        this.managerEmpId = managerEmpId;
    }

    public String getAllowNegativeStock() {
        return allowNegativeStock;
    }

    public void setAllowNegativeStock(String allowNegativeStock) {
        this.allowNegativeStock = allowNegativeStock;
    }

    public BigDecimal getVolumeCapacity() {
        return volumeCapacity;
    }

    public void setVolumeCapacity(BigDecimal volumeCapacity) {
        this.volumeCapacity = volumeCapacity;
    }

    public BigDecimal getWeightCapacity() {
        return weightCapacity;
    }

    public void setWeightCapacity(BigDecimal weightCapacity) {
        this.weightCapacity = weightCapacity;
    }

    public String getTemperatureZone() {
        return temperatureZone;
    }

    public void setTemperatureZone(String temperatureZone) {
        this.temperatureZone = temperatureZone;
    }

    public String getHazardousFlag() {
        return hazardousFlag;
    }

    public void setHazardousFlag(String hazardousFlag) {
        this.hazardousFlag = hazardousFlag;
    }

    public String getLocationCodePrefix() {
        return locationCodePrefix;
    }

    public void setLocationCodePrefix(String locationCodePrefix) {
        this.locationCodePrefix = locationCodePrefix;
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

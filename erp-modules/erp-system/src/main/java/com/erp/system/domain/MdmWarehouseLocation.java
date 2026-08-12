package com.erp.system.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.erp.common.mybatis.BaseAuditEntity;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * MDM 仓库库位对象。
 */
@TableName("mdm_warehouse_location")
public class MdmWarehouseLocation extends BaseAuditEntity implements Serializable {
    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long locationId;
    private String tenantId;
    private Long warehouseId;
    private Long areaId;
    private String locationCode;
    private String locationName;
    private BigDecimal volumeCapacity;
    private BigDecimal weightCapacity;
    private String temperatureZone;
    private String hazardousFlag;
    private String status;
    private Integer versionNo;
    private String delFlag;
    private String remark;

    public Long getLocationId() {
        return locationId;
    }

    public void setLocationId(Long locationId) {
        this.locationId = locationId;
    }

    public String getTenantId() {
        return tenantId;
    }

    public void setTenantId(String tenantId) {
        this.tenantId = tenantId;
    }

    public Long getWarehouseId() {
        return warehouseId;
    }

    public void setWarehouseId(Long warehouseId) {
        this.warehouseId = warehouseId;
    }

    public Long getAreaId() {
        return areaId;
    }

    public void setAreaId(Long areaId) {
        this.areaId = areaId;
    }

    public String getLocationCode() {
        return locationCode;
    }

    public void setLocationCode(String locationCode) {
        this.locationCode = locationCode;
    }

    public String getLocationName() {
        return locationName;
    }

    public void setLocationName(String locationName) {
        this.locationName = locationName;
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

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
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

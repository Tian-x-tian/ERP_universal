package com.erp.system.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;

/**
 * MDM 供应商主数据对象 mdm_supplier。
 */
@Data
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
    private String taxNo;
    private String defaultCurrency;
    private BigDecimal defaultTaxRate;
    private Integer leadTimeDays;
    private String qualityLevel;
    private String bankAccountInfo;
    private String contactName;
    private String contactPhone;
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
}

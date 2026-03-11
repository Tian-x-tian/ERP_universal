package com.erp.system.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;

/**
 * MDM 客户主数据对象 mdm_customer。
 */
@Data
@TableName("mdm_customer")
public class MdmCustomer implements Serializable {
    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long customerId;
    private String tenantId;
    private String customerCode;
    private String customerName;
    private String shortName;
    private String customerType;
    private String taxNo;
    private String invoiceTitle;
    private String defaultCurrency;
    private BigDecimal defaultTaxRate;
    private BigDecimal creditLimit;
    private Integer creditDays;
    private String contactName;
    private String contactPhone;
    private String contactEmail;
    private String province;
    private String city;
    private String district;
    private String detailAddress;
    private Long settleMethodId;
    private Long orgId;
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

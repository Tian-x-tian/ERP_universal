package com.erp.system.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;

/**
 * MDM 税率字典对象 mdm_tax_rate。
 */
@Data
@TableName("mdm_tax_rate")
public class MdmTaxRate implements Serializable {
    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long taxRateId;
    private String tenantId;
    private String taxCode;
    private String taxName;
    private BigDecimal taxRate;
    private Date effectiveFrom;
    private Date effectiveTo;
    private String status;
    private Integer versionNo;
    private String delFlag;
    private String remark;
    private String createBy;
    private Date createTime;
    private String updateBy;
    private Date updateTime;
}

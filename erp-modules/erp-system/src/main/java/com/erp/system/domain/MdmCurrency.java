package com.erp.system.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * MDM 币种字典对象 mdm_currency。
 */
@Data
@TableName("mdm_currency")
public class MdmCurrency implements Serializable {
    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long currencyId;
    private String tenantId;
    private String currencyCode;
    private String currencyName;
    private String symbol;
    private Integer precisionScale;
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

package com.erp.system.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;

/**
 * MDM 计量单位字典对象 mdm_uom。
 */
@Data
@TableName("mdm_uom")
public class MdmUom implements Serializable {
    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long uomId;
    private String tenantId;
    private String uomCode;
    private String uomName;
    private String baseUomCode;
    private BigDecimal convertRate;
    private String status;
    private Integer versionNo;
    private String delFlag;
    private String remark;
    private String createBy;
    private Date createTime;
    private String updateBy;
    private Date updateTime;
}

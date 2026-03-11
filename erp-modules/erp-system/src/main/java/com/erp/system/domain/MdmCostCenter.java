package com.erp.system.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * MDM 成本中心主数据对象 mdm_cost_center。
 */
@Data
@TableName("mdm_cost_center")
public class MdmCostCenter implements Serializable {
    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long ccId;
    private String tenantId;
    private String ccCode;
    private String ccName;
    private Long orgId;
    private Long parentId;
    private String status;
    private Integer versionNo;
    private String delFlag;
    private String remark;
    private String createBy;
    private Date createTime;
    private String updateBy;
    private Date updateTime;
}

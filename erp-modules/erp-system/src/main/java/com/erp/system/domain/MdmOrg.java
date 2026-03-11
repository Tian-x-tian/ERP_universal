package com.erp.system.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * MDM 组织主数据对象 mdm_org。
 */
@Data
@TableName("mdm_org")
public class MdmOrg implements Serializable {
    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long orgId;
    private String tenantId;
    private String orgCode;
    private String orgName;
    private String orgType;
    private Long parentId;
    private String ancestors;
    private String status;
    private Integer versionNo;
    private String delFlag;
    private String remark;
    private String createBy;
    private Date createTime;
    private String updateBy;
    private Date updateTime;
}

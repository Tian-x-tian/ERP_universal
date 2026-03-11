package com.erp.system.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * MDM 项目主数据对象 mdm_project。
 */
@Data
@TableName("mdm_project")
public class MdmProject implements Serializable {
    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long projectId;
    private String tenantId;
    private String projectCode;
    private String projectName;
    private Long managerEmpId;
    private Long customerId;
    private Long orgId;
    private Date startDate;
    private Date endDate;
    private String status;
    private Integer versionNo;
    private String delFlag;
    private String remark;
    private String createBy;
    private Date createTime;
    private String updateBy;
    private Date updateTime;
}

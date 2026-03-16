package com.erp.business.hr.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * HR 预警记录对象。
 */
@Data
@TableName("hr_warning_record")
public class HrWarningRecord implements Serializable {
    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long warningId;
    private String tenantId;
    private Long employeeId;
    private String warningType;
    private String warningKey;
    private String warningTitle;
    private String warningContent;
    private Date expireDate;
    private String status;
    private String readBy;
    private Date readTime;
    private String handledBy;
    private Date handledTime;
    private String remark;
    private String createBy;
    private Date createTime;
    private String updateBy;
    private Date updateTime;
}

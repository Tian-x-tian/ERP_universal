package com.erp.business.hr.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.erp.common.mybatis.BaseAuditEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;
import java.util.Date;

/**
 * 出勤失败重试任务对象。
 */
@Data
@EqualsAndHashCode(callSuper = false)
@TableName("hr_attendance_retry_task")
public class HrAttendanceRetryTask extends BaseAuditEntity implements Serializable {
    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long taskId;
    private String tenantId;
    private Long logId;
    private String taskStatus;
    private Integer retryCount;
    private Date nextRetryTime;
    private String lastError;
}


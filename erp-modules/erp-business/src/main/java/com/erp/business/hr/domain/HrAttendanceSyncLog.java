package com.erp.business.hr.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 出勤同步日志对象。
 */
@Data
@TableName("hr_attendance_sync_log")
public class HrAttendanceSyncLog implements Serializable {
    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long logId;
    private String tenantId;
    private Long employeeId;
    private String direction;
    private String periodCode;
    private String syncStatus;
    private String requestNo;
    private String payloadJson;
    private String responseJson;
    private String externalStatus;
    private Integer retryCount;
    private String lastError;
    private String createBy;
    private Date createTime;
    private String updateBy;
    private Date updateTime;
}


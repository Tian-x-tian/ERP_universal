package com.erp.business.hr.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 绩效失败重试任务对象。
 */
@Data
@TableName("hr_performance_retry_task")
public class HrPerformanceRetryTask implements Serializable {
    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long taskId;
    private String tenantId;
    private Long logId;
    private String taskStatus;
    private Integer retryCount;
    private Date nextRetryTime;
    private String lastError;
    private String createBy;
    private Date createTime;
    private String updateBy;
    private Date updateTime;
}


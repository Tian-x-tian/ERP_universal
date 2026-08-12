package com.erp.business.hr.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.erp.common.mybatis.BaseAuditEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;

/**
 * 绩效同步日志对象。
 */
@Data
@EqualsAndHashCode(callSuper = false)
@TableName("hr_performance_sync_log")
public class HrPerformanceSyncLog extends BaseAuditEntity implements Serializable {
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
}


package com.erp.saas.control.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("saas_usage_summary")
public class SaasUsageSummaryEntity {
    @TableId(value = "usage_summary_id", type = IdType.ASSIGN_ID)
    private Long usageSummaryId;

    @TableField("tenant_id")
    private String tenantId;

    @TableField("metric_key")
    private String metricKey;

    @TableField("period_start")
    private LocalDateTime periodStart;

    @TableField("used_amount")
    private Long usedAmount;

    @TableField("last_event_key")
    private String lastEventKey;

    @TableField("last_occurred_at")
    private LocalDateTime lastOccurredAt;

    @TableField("create_by")
    private String createBy;

    @TableField("create_time")
    private LocalDateTime createTime;

    @TableField("update_by")
    private String updateBy;

    @TableField("update_time")
    private LocalDateTime updateTime;

    @TableField("version_no")
    private Long versionNo;
}

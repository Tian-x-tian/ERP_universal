package com.erp.saas.control.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.erp.saas.contract.model.SaasUsageOperation;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("saas_usage_event")
public class SaasUsageEventEntity {
    @TableId(value = "usage_event_id", type = IdType.ASSIGN_ID)
    private Long usageEventId;

    @TableField("idempotency_key")
    private String idempotencyKey;

    @TableField("tenant_id")
    private String tenantId;

    @TableField("metric_key")
    private String metricKey;

    @TableField("operation")
    private SaasUsageOperation operation;

    @TableField("amount")
    private Long amount;

    @TableField("period_start")
    private LocalDateTime periodStart;

    @TableField("occurred_at")
    private LocalDateTime occurredAt;

    @TableField("create_by")
    private String createBy;

    @TableField("create_time")
    private LocalDateTime createTime;

    @TableField("update_by")
    private String updateBy;

    @TableField("update_time")
    private LocalDateTime updateTime;
}

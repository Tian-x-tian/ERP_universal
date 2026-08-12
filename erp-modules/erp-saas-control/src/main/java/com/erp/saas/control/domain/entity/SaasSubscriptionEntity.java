package com.erp.saas.control.domain.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.erp.saas.contract.model.SubscriptionState;
import com.erp.saas.control.domain.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("saas_subscription")
public class SaasSubscriptionEntity {
    @TableId(value = "subscription_id", type = IdType.ASSIGN_ID)
    private Long subscriptionId;

    @TableField(value = "tenant_id")
    private String tenantId;

    @TableField(value = "plan_id")
    private Long planId;

    @TableField(value = "state")
    private SubscriptionState state;

    @TableField(value = "start_at")
    private LocalDateTime startAt;

    @TableField(value = "end_at")
    private LocalDateTime endAt;

    @TableField(value = "grace_end_at")
    private LocalDateTime graceEndAt;

    @TableField(value = "non_expiring")
    private Boolean nonExpiring;

    @TableField(value = "current_slot", insertStrategy = FieldStrategy.NEVER, updateStrategy = FieldStrategy.NEVER)
    private String currentSlot;

    @TableField(value = "create_by")
    private String createBy;

    @TableField(value = "create_time")
    private LocalDateTime createTime;

    @TableField(value = "update_by")
    private String updateBy;

    @TableField(value = "update_time")
    private LocalDateTime updateTime;

    @TableField(value = "version_no")
    private Long versionNo;
}

package com.erp.saas.control.domain.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.erp.saas.control.domain.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("saas_plan")
public class SaasPlanEntity {
    @TableId(value = "plan_id", type = IdType.ASSIGN_ID)
    private Long planId;

    @TableField(value = "plan_code")
    private String planCode;

    @TableField(value = "plan_version")
    private Integer planVersion;

    @TableField(value = "plan_name")
    private String planName;

    @TableField(value = "status")
    private PlanStatus status;

    @TableField(value = "trial_days")
    private Integer trialDays;

    @TableField(value = "grace_days")
    private Integer graceDays;

    @TableField(value = "description")
    private String description;

    @TableField(value = "active_slot", insertStrategy = FieldStrategy.NEVER, updateStrategy = FieldStrategy.NEVER)
    private String activeSlot;

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

package com.erp.saas.control.domain.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.erp.saas.control.domain.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("saas_plan_feature")
public class SaasPlanFeatureEntity {
    @TableId(value = "plan_feature_id", type = IdType.ASSIGN_ID)
    private Long planFeatureId;

    @TableField(value = "plan_id")
    private Long planId;

    @TableField(value = "feature_id")
    private Long featureId;

    @TableField(value = "granted")
    private Boolean granted;

    @TableField(value = "create_by")
    private String createBy;

    @TableField(value = "create_time")
    private LocalDateTime createTime;

    @TableField(value = "update_by")
    private String updateBy;

    @TableField(value = "update_time")
    private LocalDateTime updateTime;
}

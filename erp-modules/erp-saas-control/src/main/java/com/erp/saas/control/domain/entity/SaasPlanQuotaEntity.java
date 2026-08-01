package com.erp.saas.control.domain.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.erp.saas.control.domain.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("saas_plan_quota")
public class SaasPlanQuotaEntity {
    @TableId(value = "plan_quota_id", type = IdType.ASSIGN_ID)
    private Long planQuotaId;

    @TableField(value = "plan_id")
    private Long planId;

    @TableField(value = "quota_key")
    private String quotaKey;

    @TableField(value = "limit_value")
    private Long limitValue;

    @TableField(value = "period_type")
    private QuotaPeriodType periodType;

    @TableField(value = "create_by")
    private String createBy;

    @TableField(value = "create_time")
    private LocalDateTime createTime;

    @TableField(value = "update_by")
    private String updateBy;

    @TableField(value = "update_time")
    private LocalDateTime updateTime;
}

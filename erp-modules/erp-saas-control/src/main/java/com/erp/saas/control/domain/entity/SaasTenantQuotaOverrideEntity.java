package com.erp.saas.control.domain.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.erp.saas.control.domain.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("saas_tenant_quota_override")
public class SaasTenantQuotaOverrideEntity {
    @TableId(value = "override_id", type = IdType.ASSIGN_ID)
    private Long overrideId;

    @TableField(value = "tenant_id")
    private String tenantId;

    @TableField(value = "quota_key")
    private String quotaKey;

    @TableField(value = "limit_value")
    private Long limitValue;

    @TableField(value = "effective_from")
    private LocalDateTime effectiveFrom;

    @TableField(value = "effective_until")
    private LocalDateTime effectiveUntil;

    @TableField(value = "reason")
    private String reason;

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

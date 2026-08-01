package com.erp.saas.control.domain.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.erp.saas.contract.model.TenantLifecycleState;
import com.erp.saas.control.domain.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("saas_tenant")
public class SaasTenantEntity {
    @TableId(value = "id", type = IdType.ASSIGN_ID)
    private Long id;

    @TableField(value = "tenant_id")
    private String tenantId;

    @TableField(value = "slug")
    private String slug;

    @TableField(value = "tenant_name")
    private String tenantName;

    @TableField(value = "lifecycle_state")
    private TenantLifecycleState lifecycleState;

    @TableField(value = "suspended_from_state")
    private TenantLifecycleState suspendedFromState;

    @TableField(value = "archived_at")
    private LocalDateTime archivedAt;

    @TableField(value = "purge_eligible_at")
    private LocalDateTime purgeEligibleAt;

    @TableField(value = "purged_at")
    private LocalDateTime purgedAt;

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

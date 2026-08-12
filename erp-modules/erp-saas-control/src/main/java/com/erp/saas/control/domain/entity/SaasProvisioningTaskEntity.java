package com.erp.saas.control.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.erp.saas.control.domain.SaasProvisioningStatus;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("saas_provisioning_task")
public class SaasProvisioningTaskEntity {
    @TableId(value = "task_id", type = IdType.ASSIGN_ID)
    private Long taskId;
    @TableField("request_id")
    private String requestId;
    @TableField("request_hash")
    private String requestHash;
    @TableField("tenant_id")
    private String tenantId;
    @TableField("plan_id")
    private Long planId;
    @TableField("status")
    private SaasProvisioningStatus status;
    @TableField("attempt_count")
    private Integer attemptCount;
    @TableField("lease_until")
    private LocalDateTime leaseUntil;
    @TableField("tenant_record_id")
    private Long tenantRecordId;
    @TableField("company_id")
    private Long companyId;
    @TableField("dept_id")
    private Long deptId;
    @TableField("role_id")
    private Long roleId;
    @TableField("user_id")
    private Long userId;
    @TableField("activation_expires_at")
    private LocalDateTime activationExpiresAt;
    @TableField("last_error_type")
    private String lastErrorType;
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

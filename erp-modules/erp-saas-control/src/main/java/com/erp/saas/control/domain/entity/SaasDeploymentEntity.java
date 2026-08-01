package com.erp.saas.control.domain.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.erp.saas.contract.model.DeploymentMode;
import com.erp.saas.control.domain.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("saas_deployment")
public class SaasDeploymentEntity {
    @TableId(value = "deployment_id", type = IdType.ASSIGN_ID)
    private Long deploymentId;

    @TableField(value = "tenant_id")
    private String tenantId;

    @TableField(value = "mode")
    private DeploymentMode mode;

    @TableField(value = "status")
    private DeploymentStatus status;

    @TableField(value = "deployment_ref")
    private String deploymentRef;

    @TableField(value = "secret_ref")
    private String secretRef;

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

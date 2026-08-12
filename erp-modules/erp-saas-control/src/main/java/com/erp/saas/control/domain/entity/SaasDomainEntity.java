package com.erp.saas.control.domain.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.erp.saas.control.domain.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("saas_domain")
public class SaasDomainEntity {
    @TableId(value = "domain_id", type = IdType.ASSIGN_ID)
    private Long domainId;

    @TableField(value = "tenant_id")
    private String tenantId;

    @TableField(value = "host")
    private String host;

    @TableField(value = "verification_state")
    private DomainVerificationState verificationState;

    @TableField(value = "verification_method")
    private DomainVerificationMethod verificationMethod;

    @TableField(value = "verified_at")
    private LocalDateTime verifiedAt;

    @TableField(value = "revoked_at")
    private LocalDateTime revokedAt;

    @TableField(value = "owned_host", insertStrategy = FieldStrategy.NEVER, updateStrategy = FieldStrategy.NEVER)
    private String ownedHost;

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

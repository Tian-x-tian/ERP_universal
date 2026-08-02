package com.erp.saas.control.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("saas_entitlement_snapshot")
public class SaasEntitlementSnapshotEntity {
    @TableId(value = "tenant_id", type = IdType.INPUT)
    private String tenantId;

    @TableField("snapshot_version")
    private Long snapshotVersion;

    @TableField("payload_hash")
    private String payloadHash;

    @TableField("snapshot_json")
    private String snapshotJson;

    @TableField("issued_at")
    private LocalDateTime issuedAt;

    @TableField("expires_at")
    private LocalDateTime expiresAt;

    @TableField("signature_key_id")
    private String signatureKeyId;

    @TableField("signature")
    private String signature;

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

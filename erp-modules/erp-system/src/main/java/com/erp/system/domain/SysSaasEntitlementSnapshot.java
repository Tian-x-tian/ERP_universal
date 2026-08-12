package com.erp.system.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.erp.common.mybatis.BaseAuditEntity;

import java.time.LocalDateTime;

@TableName("sys_saas_entitlement_snapshot")
public class SysSaasEntitlementSnapshot extends BaseAuditEntity {
    @TableId(value = "tenant_id", type = IdType.INPUT)
    private String tenantId;
    @TableField("snapshot_version")
    private Long snapshotVersion;
    @TableField("snapshot_json")
    private String snapshotJson;
    @TableField("issued_at")
    private LocalDateTime issuedAt;
    @TableField("expires_at")
    private LocalDateTime expiresAt;
    @TableField("signature_key_id")
    private String signatureKeyId;
    private String signature;
    @TableField("version_no")
    private Long versionNo;

    public String getTenantId() { return tenantId; }
    public void setTenantId(String tenantId) { this.tenantId = tenantId; }
    public Long getSnapshotVersion() { return snapshotVersion; }
    public void setSnapshotVersion(Long snapshotVersion) { this.snapshotVersion = snapshotVersion; }
    public String getSnapshotJson() { return snapshotJson; }
    public void setSnapshotJson(String snapshotJson) { this.snapshotJson = snapshotJson; }
    public LocalDateTime getIssuedAt() { return issuedAt; }
    public void setIssuedAt(LocalDateTime issuedAt) { this.issuedAt = issuedAt; }
    public LocalDateTime getExpiresAt() { return expiresAt; }
    public void setExpiresAt(LocalDateTime expiresAt) { this.expiresAt = expiresAt; }
    public String getSignatureKeyId() { return signatureKeyId; }
    public void setSignatureKeyId(String signatureKeyId) { this.signatureKeyId = signatureKeyId; }
    public String getSignature() { return signature; }
    public void setSignature(String signature) { this.signature = signature; }
    public Long getVersionNo() { return versionNo; }
    public void setVersionNo(Long versionNo) { this.versionNo = versionNo; }
}

package com.erp.saas.control.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.erp.saas.control.domain.entity.SaasEntitlementSnapshotEntity;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.time.LocalDateTime;

public interface SaasEntitlementSnapshotMapper extends BaseMapper<SaasEntitlementSnapshotEntity> {
    @Select("SELECT * FROM saas_entitlement_snapshot WHERE tenant_id = #{tenantId} FOR UPDATE")
    SaasEntitlementSnapshotEntity findForUpdate(@Param("tenantId") String tenantId);

    @Update("UPDATE saas_entitlement_snapshot SET snapshot_version = #{row.snapshotVersion}, "
            + "payload_hash = #{row.payloadHash}, snapshot_json = #{row.snapshotJson}, "
            + "issued_at = #{row.issuedAt}, expires_at = #{row.expiresAt}, "
            + "signature_key_id = #{row.signatureKeyId}, signature = #{row.signature}, "
            + "update_by = #{operator}, update_time = #{now}, version_no = version_no + 1 "
            + "WHERE tenant_id = #{row.tenantId} AND version_no = #{expectedVersion}")
    int updateVersioned(@Param("row") SaasEntitlementSnapshotEntity row,
            @Param("expectedVersion") Long expectedVersion,
            @Param("operator") String operator, @Param("now") LocalDateTime now);
}

package com.erp.system.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.erp.system.domain.SysSaasEntitlementSnapshot;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.time.LocalDateTime;

@Mapper
public interface SysSaasEntitlementSnapshotMapper extends BaseMapper<SysSaasEntitlementSnapshot> {
    @Select("SELECT * FROM sys_saas_entitlement_snapshot WHERE tenant_id = #{tenantId}")
    SysSaasEntitlementSnapshot findByTenantId(@Param("tenantId") String tenantId);

    @Select("SELECT * FROM sys_saas_entitlement_snapshot WHERE tenant_id = #{tenantId} FOR UPDATE")
    SysSaasEntitlementSnapshot findForUpdate(@Param("tenantId") String tenantId);

    @Update("UPDATE sys_saas_entitlement_snapshot SET snapshot_version = #{row.snapshotVersion}, "
            + "snapshot_json = #{row.snapshotJson}, issued_at = #{row.issuedAt}, expires_at = #{row.expiresAt}, "
            + "signature_key_id = #{row.signatureKeyId}, signature = #{row.signature}, "
            + "update_by = #{operator}, update_time = #{now}, version_no = version_no + 1 "
            + "WHERE tenant_id = #{row.tenantId} AND version_no = #{expectedVersion}")
    int updateVersioned(@Param("row") SysSaasEntitlementSnapshot row,
            @Param("expectedVersion") Long expectedVersion,
            @Param("operator") String operator, @Param("now") LocalDateTime now);
}

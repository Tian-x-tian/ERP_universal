package com.erp.saas.control.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.erp.saas.control.domain.entity.SaasDomainEntity;
import com.erp.saas.control.service.domain.ResolvedTenantDomainRow;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.time.LocalDateTime;

public interface SaasDomainMapper extends BaseMapper<SaasDomainEntity> {
    @Select("SELECT * FROM saas_domain WHERE owned_host = #{host} "
            + "AND verification_state <> 'REVOKED' FOR UPDATE")
    SaasDomainEntity findOwnedHostForUpdate(@Param("host") String host);

    @Select("SELECT * FROM saas_domain WHERE domain_id = #{domainId} FOR UPDATE")
    SaasDomainEntity findByIdForUpdate(@Param("domainId") Long domainId);

    @Update("UPDATE saas_domain SET verification_state = 'VERIFIED', verified_at = #{now}, "
            + "revoked_at = NULL, update_by = #{operator}, update_time = #{now}, "
            + "version_no = version_no + 1 WHERE domain_id = #{domainId} "
            + "AND version_no = #{expectedVersion} AND verification_state = 'PENDING'")
    int markVerified(@Param("domainId") Long domainId, @Param("expectedVersion") Long expectedVersion,
            @Param("operator") String operator, @Param("now") LocalDateTime now);

    @Update("UPDATE saas_domain SET verification_state = 'REVOKED', revoked_at = #{now}, "
            + "update_by = #{operator}, update_time = #{now}, version_no = version_no + 1 "
            + "WHERE domain_id = #{domainId} AND version_no = #{expectedVersion} "
            + "AND verification_state IN ('PENDING','VERIFIED')")
    int markRevoked(@Param("domainId") Long domainId, @Param("expectedVersion") Long expectedVersion,
            @Param("operator") String operator, @Param("now") LocalDateTime now);

    @Select("SELECT d.domain_id, d.tenant_id, d.host, t.lifecycle_state FROM saas_domain d "
            + "JOIN saas_tenant t ON t.tenant_id = d.tenant_id WHERE d.host = #{host} "
            + "AND d.verification_state = 'VERIFIED' "
            + "AND t.lifecycle_state NOT IN ('ARCHIVED','PURGE_PENDING','PURGED') LIMIT 1")
    ResolvedTenantDomainRow resolveVerified(@Param("host") String host);
}

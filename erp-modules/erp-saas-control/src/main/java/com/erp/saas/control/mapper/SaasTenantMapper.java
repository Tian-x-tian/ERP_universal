package com.erp.saas.control.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.erp.saas.contract.model.TenantLifecycleState;
import com.erp.saas.control.domain.entity.SaasTenantEntity;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.time.LocalDateTime;

public interface SaasTenantMapper extends BaseMapper<SaasTenantEntity> {
    @Select("SELECT * FROM saas_tenant WHERE tenant_id = #{tenantId}")
    SaasTenantEntity findByTenantId(@Param("tenantId") String tenantId);

    @Select("SELECT * FROM saas_tenant WHERE tenant_id = #{tenantId} FOR UPDATE")
    SaasTenantEntity lockByTenantId(@Param("tenantId") String tenantId);

    @Update("UPDATE saas_tenant SET lifecycle_state = #{nextState}, "
            + "suspended_from_state = #{suspendedFromState}, update_by = #{operator}, update_time = #{now}, "
            + "version_no = version_no + 1 WHERE tenant_id = #{tenantId} "
            + "AND lifecycle_state = #{expectedState} AND version_no = #{expectedVersion}")
    int transitionLifecycle(@Param("tenantId") String tenantId,
            @Param("expectedState") TenantLifecycleState expectedState,
            @Param("nextState") TenantLifecycleState nextState,
            @Param("suspendedFromState") TenantLifecycleState suspendedFromState,
            @Param("expectedVersion") Long expectedVersion,
            @Param("operator") String operator, @Param("now") LocalDateTime now);

    @Update("UPDATE saas_tenant SET lifecycle_state = 'ARCHIVED', suspended_from_state = NULL, "
            + "archived_at = #{now}, purge_eligible_at = #{purgeEligibleAt}, update_by = #{operator}, "
            + "update_time = #{now}, version_no = version_no + 1 WHERE tenant_id = #{tenantId} "
            + "AND lifecycle_state = #{expectedState} AND version_no = #{expectedVersion}")
    int archive(@Param("tenantId") String tenantId,
            @Param("expectedState") TenantLifecycleState expectedState,
            @Param("expectedVersion") Long expectedVersion,
            @Param("purgeEligibleAt") LocalDateTime purgeEligibleAt,
            @Param("operator") String operator, @Param("now") LocalDateTime now);

    @Update("UPDATE saas_tenant SET lifecycle_state = 'PURGE_PENDING', update_by = #{operator}, "
            + "update_time = #{now}, version_no = version_no + 1 WHERE tenant_id = #{tenantId} "
            + "AND lifecycle_state = 'ARCHIVED' AND version_no = #{expectedVersion} "
            + "AND purge_eligible_at <= #{now}")
    int markPurgePending(@Param("tenantId") String tenantId,
            @Param("expectedVersion") Long expectedVersion,
            @Param("operator") String operator, @Param("now") LocalDateTime now);
}

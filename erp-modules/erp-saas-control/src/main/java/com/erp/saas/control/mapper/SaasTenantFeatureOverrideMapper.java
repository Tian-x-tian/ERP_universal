package com.erp.saas.control.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.erp.saas.control.domain.entity.SaasTenantFeatureOverrideEntity;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDateTime;
import java.util.List;

public interface SaasTenantFeatureOverrideMapper extends BaseMapper<SaasTenantFeatureOverrideEntity> {
    @Select("SELECT * FROM saas_tenant_feature_override WHERE tenant_id = #{tenantId} "
            + "AND feature_id = #{featureId} ORDER BY effective_from, override_id FOR UPDATE")
    List<SaasTenantFeatureOverrideEntity> findWindowsForUpdate(@Param("tenantId") String tenantId,
            @Param("featureId") Long featureId);

    @Select("SELECT * FROM saas_tenant_feature_override WHERE override_id = #{overrideId} FOR UPDATE")
    SaasTenantFeatureOverrideEntity findByIdForUpdate(@Param("overrideId") Long overrideId);

    @Delete("DELETE FROM saas_tenant_feature_override WHERE override_id = #{overrideId} "
            + "AND version_no = #{expectedVersion} AND effective_from > #{now}")
    int deleteFutureVersioned(@Param("overrideId") Long overrideId,
            @Param("expectedVersion") Long expectedVersion, @Param("now") LocalDateTime now);

    @Select("SELECT * FROM saas_tenant_feature_override WHERE tenant_id = #{tenantId} "
            + "ORDER BY feature_id, effective_from, override_id")
    List<SaasTenantFeatureOverrideEntity> findByTenantId(@Param("tenantId") String tenantId);
}

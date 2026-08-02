package com.erp.saas.control.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.erp.saas.control.domain.entity.SaasTenantEntity;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

public interface SaasTenantMapper extends BaseMapper<SaasTenantEntity> {
    @Select("SELECT * FROM saas_tenant WHERE tenant_id = #{tenantId}")
    SaasTenantEntity findByTenantId(@Param("tenantId") String tenantId);

    @Select("SELECT * FROM saas_tenant WHERE tenant_id = #{tenantId} FOR UPDATE")
    SaasTenantEntity lockByTenantId(@Param("tenantId") String tenantId);
}

package com.erp.saas.control.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.erp.saas.control.domain.entity.SaasDeploymentEntity;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

public interface SaasDeploymentMapper extends BaseMapper<SaasDeploymentEntity> {
    @Select("SELECT * FROM saas_deployment WHERE tenant_id = #{tenantId}")
    SaasDeploymentEntity findByTenantId(@Param("tenantId") String tenantId);
}

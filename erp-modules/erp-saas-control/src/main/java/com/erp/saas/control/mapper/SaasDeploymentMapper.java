package com.erp.saas.control.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.erp.saas.control.domain.DeploymentStatus;
import com.erp.saas.control.domain.entity.SaasDeploymentEntity;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

public interface SaasDeploymentMapper extends BaseMapper<SaasDeploymentEntity> {
    @Select("SELECT * FROM saas_deployment WHERE tenant_id = #{tenantId}")
    SaasDeploymentEntity findByTenantId(@Param("tenantId") String tenantId);

    @Select("SELECT * FROM saas_deployment WHERE tenant_id = #{tenantId} FOR UPDATE")
    SaasDeploymentEntity lockByTenantId(@Param("tenantId") String tenantId);

    @Update("UPDATE saas_deployment SET status = #{status}, update_by = #{operator}, "
            + "update_time = #{now}, version_no = version_no + 1 "
            + "WHERE tenant_id = #{tenantId} AND version_no = #{expectedVersion}")
    int updateStatus(@Param("tenantId") String tenantId,
            @Param("expectedVersion") Long expectedVersion,
            @Param("status") DeploymentStatus status,
            @Param("operator") String operator,
            @Param("now") java.time.LocalDateTime now);
}

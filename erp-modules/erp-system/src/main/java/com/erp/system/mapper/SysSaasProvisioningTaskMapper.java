package com.erp.system.mapper;

import com.erp.system.domain.SysSaasProvisioningTask;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.time.LocalDateTime;

@Mapper
public interface SysSaasProvisioningTaskMapper {
    @Insert("INSERT IGNORE INTO sys_saas_provisioning_task (tenant_id, request_id, request_hash, status, "
            + "create_by, create_time, update_by, update_time) VALUES (#{tenantId}, #{requestId}, "
            + "#{requestHash}, #{status}, #{createBy}, #{createTime}, #{updateBy}, #{updateTime})")
    int insertProcessing(SysSaasProvisioningTask task);

    @Select("SELECT task_id, tenant_id, request_id, request_hash, status, tenant_record_id, company_id, "
            + "dept_id, role_id, user_id, activation_expires_at, create_by, create_time, update_by, "
            + "update_time FROM sys_saas_provisioning_task WHERE tenant_id = #{tenantId} "
            + "AND request_id = #{requestId} FOR UPDATE")
    SysSaasProvisioningTask lock(@Param("tenantId") String tenantId,
            @Param("requestId") String requestId);

    @Update("UPDATE sys_saas_provisioning_task SET status = 'SUCCEEDED', "
            + "tenant_record_id = #{tenantRecordId}, company_id = #{companyId}, dept_id = #{deptId}, "
            + "role_id = #{roleId}, user_id = #{userId}, activation_expires_at = #{activationExpiresAt}, "
            + "update_by = 'saas-provisioning', update_time = #{now} WHERE tenant_id = #{tenantId} "
            + "AND request_id = #{requestId} AND status = 'PROCESSING'")
    int markSucceeded(@Param("tenantId") String tenantId, @Param("requestId") String requestId,
            @Param("tenantRecordId") Long tenantRecordId, @Param("companyId") Long companyId,
            @Param("deptId") Long deptId, @Param("roleId") Long roleId,
            @Param("userId") Long userId, @Param("activationExpiresAt") LocalDateTime activationExpiresAt,
            @Param("now") LocalDateTime now);

    @Update("UPDATE sys_saas_provisioning_task SET activation_expires_at = #{activationExpiresAt}, "
            + "update_by = 'saas-provisioning', update_time = #{now} WHERE tenant_id = #{tenantId} "
            + "AND request_id = #{requestId} AND user_id = #{userId} AND status = 'SUCCEEDED'")
    int updateActivationExpiry(@Param("tenantId") String tenantId,
            @Param("requestId") String requestId, @Param("userId") Long userId,
            @Param("activationExpiresAt") LocalDateTime activationExpiresAt,
            @Param("now") LocalDateTime now);
}

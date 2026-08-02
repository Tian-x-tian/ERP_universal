package com.erp.saas.control.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.erp.saas.control.domain.entity.SaasProvisioningTaskEntity;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.time.LocalDateTime;

public interface SaasProvisioningTaskMapper extends BaseMapper<SaasProvisioningTaskEntity> {
    @Select("SELECT * FROM saas_provisioning_task WHERE tenant_id = #{tenantId}")
    SaasProvisioningTaskEntity findByTenantId(@Param("tenantId") String tenantId);

    @Select("SELECT * FROM saas_provisioning_task WHERE request_id = #{requestId} FOR UPDATE")
    SaasProvisioningTaskEntity lockByRequestId(@Param("requestId") String requestId);

    @Update("UPDATE saas_provisioning_task SET status = 'PROVISIONING', "
            + "attempt_count = attempt_count + 1, lease_until = #{leaseUntil}, last_error_type = NULL, "
            + "tenant_record_id = NULL, company_id = NULL, dept_id = NULL, role_id = NULL, user_id = NULL, "
            + "activation_expires_at = NULL, "
            + "update_by = #{operator}, update_time = #{now}, version_no = version_no + 1 "
            + "WHERE request_id = #{requestId} AND version_no = #{expectedVersion} "
            + "AND status IN ('PENDING', 'FAILED')")
    int markProcessing(@Param("requestId") String requestId,
            @Param("expectedVersion") Long expectedVersion,
            @Param("leaseUntil") LocalDateTime leaseUntil,
            @Param("operator") String operator,
            @Param("now") LocalDateTime now);

    @Update("UPDATE saas_provisioning_task SET status = 'FAILED', lease_until = NULL, "
            + "last_error_type = 'ProvisioningLeaseExpired', update_by = #{operator}, "
            + "update_time = #{now}, version_no = version_no + 1 WHERE request_id = #{requestId} "
            + "AND version_no = #{expectedVersion} AND status = 'PROVISIONING' "
            + "AND lease_until <= #{now}")
    int reclaimExpired(@Param("requestId") String requestId,
            @Param("expectedVersion") Long expectedVersion,
            @Param("now") LocalDateTime now,
            @Param("operator") String operator);

    @Update("UPDATE saas_provisioning_task SET status = 'INITIALIZED', lease_until = NULL, "
            + "tenant_record_id = #{tenantRecordId}, company_id = #{companyId}, dept_id = #{deptId}, "
            + "role_id = #{roleId}, user_id = #{userId}, activation_expires_at = #{activationExpiresAt}, "
            + "update_by = #{operator}, update_time = #{now}, version_no = version_no + 1 "
            + "WHERE request_id = #{requestId} AND version_no = #{expectedVersion} "
            + "AND status = 'PROVISIONING'")
    int markInitialized(@Param("requestId") String requestId,
            @Param("expectedVersion") Long expectedVersion,
            @Param("tenantRecordId") Long tenantRecordId,
            @Param("companyId") Long companyId,
            @Param("deptId") Long deptId,
            @Param("roleId") Long roleId,
            @Param("userId") Long userId,
            @Param("activationExpiresAt") LocalDateTime activationExpiresAt,
            @Param("operator") String operator,
            @Param("now") LocalDateTime now);

    @Update("UPDATE saas_provisioning_task SET status = 'SUCCEEDED', lease_until = NULL, "
            + "last_error_type = NULL, update_by = #{operator}, update_time = #{now}, "
            + "version_no = version_no + 1 WHERE request_id = #{requestId} "
            + "AND version_no = #{expectedVersion} AND status = 'INITIALIZED'")
    int markSucceeded(@Param("requestId") String requestId,
            @Param("expectedVersion") Long expectedVersion,
            @Param("operator") String operator,
            @Param("now") LocalDateTime now);

    @Update("UPDATE saas_provisioning_task SET status = 'FAILED', lease_until = NULL, "
            + "last_error_type = #{errorType}, update_by = #{operator}, update_time = #{now}, "
            + "version_no = version_no + 1 WHERE request_id = #{requestId} "
            + "AND version_no = #{expectedVersion} AND status IN ('PENDING', 'PROVISIONING', 'INITIALIZED')")
    int markFailed(@Param("requestId") String requestId,
            @Param("expectedVersion") Long expectedVersion,
            @Param("errorType") String errorType,
            @Param("operator") String operator,
            @Param("now") LocalDateTime now);
}

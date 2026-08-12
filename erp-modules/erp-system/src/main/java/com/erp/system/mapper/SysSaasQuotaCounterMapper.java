package com.erp.system.mapper;

import com.erp.system.domain.SysSaasQuotaCounter;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.time.LocalDateTime;

@Mapper
public interface SysSaasQuotaCounterMapper {
    @Insert("INSERT IGNORE INTO sys_saas_quota_counter (tenant_id, metric_key, period_start, "
            + "used_amount, reserved_amount, create_by, create_time, update_by, update_time, version_no) "
            + "VALUES (#{tenantId}, #{metricKey}, #{periodStart}, #{baseline}, 0, #{operator}, #{now}, "
            + "#{operator}, #{now}, 0)")
    int ensureCounter(@Param("tenantId") String tenantId, @Param("metricKey") String metricKey,
            @Param("periodStart") LocalDateTime periodStart, @Param("baseline") Long baseline,
            @Param("operator") String operator, @Param("now") LocalDateTime now);

    @Select("SELECT tenant_id, metric_key, period_start, used_amount, reserved_amount, version_no "
            + "FROM sys_saas_quota_counter WHERE tenant_id = #{tenantId} AND metric_key = #{metricKey} "
            + "AND period_start = #{periodStart} FOR UPDATE")
    SysSaasQuotaCounter findForUpdate(@Param("tenantId") String tenantId,
            @Param("metricKey") String metricKey, @Param("periodStart") LocalDateTime periodStart);

    @Update("UPDATE sys_saas_quota_counter SET reserved_amount = reserved_amount + #{amount}, "
            + "update_by = #{operator}, update_time = #{now}, version_no = version_no + 1 "
            + "WHERE tenant_id = #{tenantId} AND metric_key = #{metricKey} AND period_start = #{periodStart} "
            + "AND (#{limit} IS NULL OR (#{amount} <= #{limit} "
            + "AND used_amount <= #{limit} - #{amount} "
            + "AND reserved_amount <= #{limit} - #{amount} - used_amount))")
    int addReservation(@Param("tenantId") String tenantId, @Param("metricKey") String metricKey,
            @Param("periodStart") LocalDateTime periodStart, @Param("amount") Long amount,
            @Param("limit") Long limit, @Param("operator") String operator,
            @Param("now") LocalDateTime now);

    @Update("UPDATE sys_saas_quota_counter SET reserved_amount = reserved_amount - #{reservedAmount}, "
            + "used_amount = used_amount + #{settledAmount}, update_by = #{operator}, update_time = #{now}, "
            + "version_no = version_no + 1 WHERE tenant_id = #{tenantId} AND metric_key = #{metricKey} "
            + "AND period_start = #{periodStart} AND reserved_amount >= #{reservedAmount}")
    int settleReservation(@Param("tenantId") String tenantId, @Param("metricKey") String metricKey,
            @Param("periodStart") LocalDateTime periodStart, @Param("reservedAmount") Long reservedAmount,
            @Param("settledAmount") Long settledAmount, @Param("operator") String operator,
            @Param("now") LocalDateTime now);

    @Update("UPDATE sys_saas_quota_counter SET reserved_amount = reserved_amount - #{amount}, "
            + "update_by = #{operator}, update_time = #{now}, version_no = version_no + 1 "
            + "WHERE tenant_id = #{tenantId} AND metric_key = #{metricKey} AND period_start = #{periodStart} "
            + "AND reserved_amount >= #{amount}")
    int releaseReservation(@Param("tenantId") String tenantId, @Param("metricKey") String metricKey,
            @Param("periodStart") LocalDateTime periodStart, @Param("amount") Long amount,
            @Param("operator") String operator, @Param("now") LocalDateTime now);

    @Update("UPDATE sys_saas_quota_counter SET used_amount = used_amount - #{amount}, "
            + "update_by = #{operator}, update_time = #{now}, version_no = version_no + 1 "
            + "WHERE tenant_id = #{tenantId} AND metric_key = #{metricKey} AND period_start = #{periodStart} "
            + "AND used_amount >= #{amount}")
    int releaseConsumed(@Param("tenantId") String tenantId, @Param("metricKey") String metricKey,
            @Param("periodStart") LocalDateTime periodStart, @Param("amount") Long amount,
            @Param("operator") String operator, @Param("now") LocalDateTime now);
}

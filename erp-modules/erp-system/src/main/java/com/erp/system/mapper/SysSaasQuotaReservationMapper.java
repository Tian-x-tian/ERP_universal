package com.erp.system.mapper;

import com.erp.system.domain.SysSaasQuotaReservation;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.time.LocalDateTime;

@Mapper
public interface SysSaasQuotaReservationMapper {
    @Select("SELECT * FROM sys_saas_quota_reservation WHERE tenant_id = #{tenantId} "
            + "AND metric_key = #{metricKey} AND reservation_key = #{reservationKey} FOR UPDATE")
    SysSaasQuotaReservation findForUpdate(@Param("tenantId") String tenantId,
            @Param("metricKey") String metricKey, @Param("reservationKey") String reservationKey);

    @Insert("INSERT INTO sys_saas_quota_reservation (tenant_id, metric_key, reservation_key, period_start, "
            + "reserved_amount, settled_amount, status, reserve_event_key, settle_event_key, release_event_key, "
            + "create_by, create_time, update_by, update_time, version_no) VALUES (#{tenantId}, #{metricKey}, "
            + "#{reservationKey}, #{periodStart}, #{reservedAmount}, #{settledAmount}, #{status}, "
            + "#{reserveEventKey}, #{settleEventKey}, #{releaseEventKey}, #{createBy}, #{createTime}, "
            + "#{updateBy}, #{updateTime}, #{versionNo})")
    int insert(SysSaasQuotaReservation reservation);

    @Update("UPDATE sys_saas_quota_reservation SET settled_amount = #{settledAmount}, status = 'SETTLED', "
            + "settle_event_key = #{eventKey}, update_by = #{operator}, update_time = #{now}, "
            + "version_no = version_no + 1 WHERE tenant_id = #{tenantId} AND metric_key = #{metricKey} "
            + "AND reservation_key = #{reservationKey} AND status = 'RESERVED'")
    int markSettled(@Param("tenantId") String tenantId, @Param("metricKey") String metricKey,
            @Param("reservationKey") String reservationKey, @Param("settledAmount") Long settledAmount,
            @Param("eventKey") String eventKey, @Param("operator") String operator,
            @Param("now") LocalDateTime now);

    @Update("UPDATE sys_saas_quota_reservation SET status = 'RELEASED', "
            + "release_event_key = #{eventKey}, update_by = #{operator}, update_time = #{now}, "
            + "version_no = version_no + 1 WHERE tenant_id = #{tenantId} AND metric_key = #{metricKey} "
            + "AND reservation_key = #{reservationKey} AND status IN ('RESERVED', 'SETTLED')")
    int markReleased(@Param("tenantId") String tenantId, @Param("metricKey") String metricKey,
            @Param("reservationKey") String reservationKey, @Param("eventKey") String eventKey,
            @Param("operator") String operator, @Param("now") LocalDateTime now);
}

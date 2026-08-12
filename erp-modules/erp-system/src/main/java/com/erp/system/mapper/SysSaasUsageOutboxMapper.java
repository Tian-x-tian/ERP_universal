package com.erp.system.mapper;

import com.erp.system.domain.SysSaasUsageOutbox;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface SysSaasUsageOutboxMapper {
    @Insert("INSERT INTO sys_saas_usage_outbox (tenant_id, event_key, metric_key, amount, period_start, "
            + "occurred_at, status, attempt_count, next_attempt_at, sent_at, last_error_type, "
            + "create_by, create_time, update_by, update_time) VALUES (#{tenantId}, #{eventKey}, "
            + "#{metricKey}, #{amount}, #{periodStart}, #{occurredAt}, #{status}, #{attemptCount}, "
            + "#{nextAttemptAt}, #{sentAt}, #{lastErrorType}, #{createBy}, #{createTime}, "
            + "#{updateBy}, #{updateTime})")
    int insert(SysSaasUsageOutbox row);

    @Select("SELECT outbox_id, tenant_id, event_key, metric_key, amount, period_start, occurred_at, "
            + "status, attempt_count, next_attempt_at, sent_at, last_error_type, create_by, create_time, "
            + "update_by, update_time FROM sys_saas_usage_outbox WHERE tenant_id = #{tenantId} "
            + "AND status = 'PENDING' AND next_attempt_at <= #{now} ORDER BY outbox_id LIMIT #{limit}")
    List<SysSaasUsageOutbox> findPending(@Param("tenantId") String tenantId,
            @Param("now") LocalDateTime now, @Param("limit") int limit);

    @Update("UPDATE sys_saas_usage_outbox SET status = 'SENT', sent_at = #{now}, "
            + "last_error_type = NULL, update_by = 'usage-dispatcher', update_time = #{now} "
            + "WHERE tenant_id = #{tenantId} AND outbox_id = #{outboxId} AND status = 'PENDING'")
    int markSent(@Param("tenantId") String tenantId, @Param("outboxId") Long outboxId,
            @Param("now") LocalDateTime now);

    @Update("UPDATE sys_saas_usage_outbox SET attempt_count = #{attemptCount}, "
            + "next_attempt_at = #{nextAttemptAt}, last_error_type = #{lastErrorType}, "
            + "update_by = 'usage-dispatcher', update_time = #{now} WHERE tenant_id = #{tenantId} "
            + "AND outbox_id = #{outboxId} AND status = 'PENDING'")
    int markRetry(@Param("tenantId") String tenantId, @Param("outboxId") Long outboxId,
            @Param("attemptCount") int attemptCount, @Param("nextAttemptAt") LocalDateTime nextAttemptAt,
            @Param("lastErrorType") String lastErrorType, @Param("now") LocalDateTime now);
}

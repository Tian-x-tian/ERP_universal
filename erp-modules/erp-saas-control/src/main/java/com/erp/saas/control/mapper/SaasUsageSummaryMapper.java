package com.erp.saas.control.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.erp.saas.control.domain.entity.SaasUsageSummaryEntity;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Param;

public interface SaasUsageSummaryMapper extends BaseMapper<SaasUsageSummaryEntity> {
    @Insert("INSERT INTO saas_usage_summary (usage_summary_id, tenant_id, metric_key, period_start, used_amount, "
            + "last_event_key, last_occurred_at, create_by, create_time, update_by, update_time, version_no) "
            + "VALUES (#{row.usageSummaryId}, #{row.tenantId}, #{row.metricKey}, #{row.periodStart}, "
            + "#{row.usedAmount}, #{row.lastEventKey}, #{row.lastOccurredAt}, #{row.createBy}, "
            + "#{row.createTime}, #{row.updateBy}, #{row.updateTime}, #{row.versionNo}) "
            + "ON DUPLICATE KEY UPDATE "
            + "used_amount = IF(VALUES(last_occurred_at) >= last_occurred_at, VALUES(used_amount), used_amount), "
            + "last_event_key = IF(VALUES(last_occurred_at) >= last_occurred_at, "
            + "VALUES(last_event_key), last_event_key), "
            + "update_by = IF(VALUES(last_occurred_at) >= last_occurred_at, VALUES(update_by), update_by), "
            + "update_time = IF(VALUES(last_occurred_at) >= last_occurred_at, VALUES(update_time), update_time), "
            + "version_no = IF(VALUES(last_occurred_at) >= last_occurred_at, version_no + 1, version_no), "
            + "last_occurred_at = IF(VALUES(last_occurred_at) >= last_occurred_at, "
            + "VALUES(last_occurred_at), last_occurred_at)")
    int upsertLatest(@Param("row") SaasUsageSummaryEntity row);
}

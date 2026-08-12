package com.erp.saas.control.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.erp.saas.control.domain.entity.SaasUsageEventEntity;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

public interface SaasUsageEventMapper extends BaseMapper<SaasUsageEventEntity> {
    @Select("SELECT * FROM saas_usage_event WHERE idempotency_key = #{idempotencyKey}")
    SaasUsageEventEntity findByIdempotencyKey(@Param("idempotencyKey") String idempotencyKey);
}

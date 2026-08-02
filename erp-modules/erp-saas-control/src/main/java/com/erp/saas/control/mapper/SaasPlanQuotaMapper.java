package com.erp.saas.control.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.erp.saas.control.domain.entity.SaasPlanQuotaEntity;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

public interface SaasPlanQuotaMapper extends BaseMapper<SaasPlanQuotaEntity> {
    @Delete("DELETE FROM saas_plan_quota WHERE plan_id = #{planId}")
    int deleteByPlanId(@Param("planId") Long planId);

    @Select("SELECT * FROM saas_plan_quota WHERE plan_id = #{planId} ORDER BY quota_key")
    List<SaasPlanQuotaEntity> findByPlanId(@Param("planId") Long planId);
}

package com.erp.saas.control.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.erp.saas.control.domain.entity.SaasPlanFeatureEntity;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

public interface SaasPlanFeatureMapper extends BaseMapper<SaasPlanFeatureEntity> {
    @Delete("DELETE FROM saas_plan_feature WHERE plan_id = #{planId}")
    int deleteByPlanId(@Param("planId") Long planId);

    @Select("SELECT * FROM saas_plan_feature WHERE plan_id = #{planId} ORDER BY feature_id")
    List<SaasPlanFeatureEntity> findByPlanId(@Param("planId") Long planId);
}

package com.erp.saas.control.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.erp.saas.control.domain.entity.SaasPlanEntity;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.time.LocalDateTime;
import java.util.List;

public interface SaasPlanMapper extends BaseMapper<SaasPlanEntity> {
    @Select("SELECT * FROM saas_plan WHERE plan_id = #{planId} FOR UPDATE")
    SaasPlanEntity findByIdForUpdate(@Param("planId") Long planId);

    @Select("SELECT * FROM saas_plan WHERE plan_code = #{planCode} AND status = 'ACTIVE'")
    SaasPlanEntity findActiveByCode(@Param("planCode") String planCode);

    @Select("SELECT * FROM saas_plan WHERE plan_code = #{planCode} ORDER BY plan_id FOR UPDATE")
    List<SaasPlanEntity> findFamilyForUpdate(@Param("planCode") String planCode);

    @Update("UPDATE saas_plan SET plan_code = #{plan.planCode}, plan_version = #{plan.planVersion}, "
            + "plan_name = #{plan.planName}, trial_days = #{plan.trialDays}, grace_days = #{plan.graceDays}, "
            + "description = #{plan.description}, update_by = #{operator}, update_time = #{now}, "
            + "version_no = version_no + 1 WHERE plan_id = #{plan.planId} "
            + "AND version_no = #{expectedVersion} AND status = 'DRAFT'")
    int updateDraft(@Param("plan") SaasPlanEntity plan, @Param("expectedVersion") Long expectedVersion,
            @Param("operator") String operator, @Param("now") LocalDateTime now);

    @Update("UPDATE saas_plan SET update_by = #{operator}, update_time = #{now}, version_no = version_no + 1 "
            + "WHERE plan_id = #{planId} AND version_no = #{expectedVersion} AND status = 'DRAFT'")
    int bumpDraft(@Param("planId") Long planId, @Param("expectedVersion") Long expectedVersion,
            @Param("operator") String operator, @Param("now") LocalDateTime now);

    @Update("UPDATE saas_plan SET status = 'RETIRED', update_by = #{operator}, update_time = #{now}, "
            + "version_no = version_no + 1 WHERE plan_id = #{planId} "
            + "AND version_no = #{expectedVersion} AND status = 'ACTIVE'")
    int retire(@Param("planId") Long planId, @Param("expectedVersion") Long expectedVersion,
            @Param("operator") String operator, @Param("now") LocalDateTime now);

    @Update("UPDATE saas_plan SET status = 'ACTIVE', update_by = #{operator}, update_time = #{now}, "
            + "version_no = version_no + 1 WHERE plan_id = #{planId} "
            + "AND version_no = #{expectedVersion} AND status = 'DRAFT'")
    int activate(@Param("planId") Long planId, @Param("expectedVersion") Long expectedVersion,
            @Param("operator") String operator, @Param("now") LocalDateTime now);
}

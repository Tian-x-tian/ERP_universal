package com.erp.saas.control.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.erp.saas.contract.model.SubscriptionState;
import com.erp.saas.control.domain.entity.SaasSubscriptionEntity;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.time.LocalDateTime;

public interface SaasSubscriptionMapper extends BaseMapper<SaasSubscriptionEntity> {
    @Select("SELECT * FROM saas_subscription WHERE tenant_id = #{tenantId} "
            + "AND state IN ('TRIAL','ACTIVE','GRACE') ORDER BY subscription_id DESC LIMIT 1")
    SaasSubscriptionEntity findCurrentByTenantId(@Param("tenantId") String tenantId);

    @Select("SELECT * FROM saas_subscription WHERE tenant_id = #{tenantId} "
            + "AND state IN ('TRIAL','ACTIVE','GRACE') ORDER BY subscription_id DESC LIMIT 1 FOR UPDATE")
    SaasSubscriptionEntity findCurrentForUpdate(@Param("tenantId") String tenantId);

    @Update("UPDATE saas_subscription SET state = #{nextState}, update_by = #{operator}, "
            + "update_time = #{now}, version_no = version_no + 1 WHERE subscription_id = #{subscriptionId} "
            + "AND state = #{expectedState} AND version_no = #{expectedVersion}")
    int transitionState(@Param("subscriptionId") Long subscriptionId,
            @Param("expectedState") SubscriptionState expectedState,
            @Param("nextState") SubscriptionState nextState,
            @Param("expectedVersion") Long expectedVersion,
            @Param("operator") String operator, @Param("now") LocalDateTime now);

    @Update("UPDATE saas_subscription SET plan_id = #{planId}, state = 'ACTIVE', start_at = #{now}, "
            + "end_at = #{endAt}, grace_end_at = #{graceEndAt}, non_expiring = #{nonExpiring}, "
            + "update_by = #{operator}, update_time = #{now}, version_no = version_no + 1 "
            + "WHERE subscription_id = #{subscriptionId} AND version_no = #{expectedVersion} "
            + "AND state IN ('TRIAL','ACTIVE','GRACE')")
    int renewCurrent(@Param("subscriptionId") Long subscriptionId, @Param("planId") Long planId,
            @Param("endAt") LocalDateTime endAt, @Param("graceEndAt") LocalDateTime graceEndAt,
            @Param("nonExpiring") boolean nonExpiring, @Param("expectedVersion") Long expectedVersion,
            @Param("operator") String operator, @Param("now") LocalDateTime now);
}

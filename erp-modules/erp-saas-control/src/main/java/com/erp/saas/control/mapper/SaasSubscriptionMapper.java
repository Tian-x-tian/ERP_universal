package com.erp.saas.control.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.erp.saas.control.domain.entity.SaasSubscriptionEntity;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

public interface SaasSubscriptionMapper extends BaseMapper<SaasSubscriptionEntity> {
    @Select("SELECT * FROM saas_subscription WHERE tenant_id = #{tenantId} "
            + "AND state IN ('TRIAL','ACTIVE','GRACE') ORDER BY subscription_id DESC LIMIT 1")
    SaasSubscriptionEntity findCurrentByTenantId(@Param("tenantId") String tenantId);
}

package com.erp.saas.control.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.erp.saas.control.domain.entity.SaasFeatureEntity;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.time.LocalDateTime;
import java.util.List;

public interface SaasFeatureMapper extends BaseMapper<SaasFeatureEntity> {
    @Select("SELECT * FROM saas_feature WHERE feature_id = #{featureId} FOR UPDATE")
    SaasFeatureEntity findByIdForUpdate(@Param("featureId") Long featureId);

    @Select("SELECT * FROM saas_feature WHERE feature_key = #{featureKey} FOR UPDATE")
    SaasFeatureEntity findByKeyForUpdate(@Param("featureKey") String featureKey);

    @Select("SELECT * FROM saas_feature ORDER BY feature_key")
    List<SaasFeatureEntity> findAllOrdered();

    @Update("UPDATE saas_feature SET feature_key = #{feature.featureKey}, feature_name = #{feature.featureName}, "
            + "status = #{feature.status}, description = #{feature.description}, update_by = #{operator}, "
            + "update_time = #{now}, version_no = version_no + 1 WHERE feature_id = #{feature.featureId} "
            + "AND version_no = #{expectedVersion}")
    int updateVersioned(@Param("feature") SaasFeatureEntity feature,
            @Param("expectedVersion") Long expectedVersion, @Param("operator") String operator,
            @Param("now") LocalDateTime now);
}

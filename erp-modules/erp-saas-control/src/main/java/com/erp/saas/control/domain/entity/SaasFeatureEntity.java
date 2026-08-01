package com.erp.saas.control.domain.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.erp.saas.control.domain.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("saas_feature")
public class SaasFeatureEntity {
    @TableId(value = "feature_id", type = IdType.ASSIGN_ID)
    private Long featureId;

    @TableField(value = "feature_key")
    private String featureKey;

    @TableField(value = "feature_name")
    private String featureName;

    @TableField(value = "status")
    private FeatureStatus status;

    @TableField(value = "description")
    private String description;

    @TableField(value = "create_by")
    private String createBy;

    @TableField(value = "create_time")
    private LocalDateTime createTime;

    @TableField(value = "update_by")
    private String updateBy;

    @TableField(value = "update_time")
    private LocalDateTime updateTime;

    @TableField(value = "version_no")
    private Long versionNo;
}

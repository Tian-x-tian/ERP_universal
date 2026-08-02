package com.erp.business.saas.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.erp.common.mybatis.BaseAuditEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;

@Data
@EqualsAndHashCode(callSuper = false)
@TableName("biz_saas_storage_object")
public class SaasStorageObject extends BaseAuditEntity implements Serializable {
    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long storageObjectId;
    private String tenantId;
    private String objectKey;
    private Long byteSize;
    private String status;
    private String quotaReferenceKey;
    private String lastError;
}

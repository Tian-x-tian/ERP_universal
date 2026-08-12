package com.erp.business.hr.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.erp.common.mybatis.BaseAuditEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;

/**
 * 薪资字段映射对象。
 */
@Data
@EqualsAndHashCode(callSuper = false)
@TableName("hr_salary_field_mapping")
public class HrSalaryFieldMapping extends BaseAuditEntity implements Serializable {
    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long mappingId;
    private String tenantId;
    private String direction;
    private String fieldCode;
    private String fieldName;
    private String targetField;
    private String defaultValue;
    private String status;
    private Integer sortNo;
    private String remark;
}

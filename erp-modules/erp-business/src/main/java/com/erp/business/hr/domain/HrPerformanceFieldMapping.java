package com.erp.business.hr.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 绩效字段映射对象。
 */
@Data
@TableName("hr_performance_field_mapping")
public class HrPerformanceFieldMapping implements Serializable {
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
    private String createBy;
    private Date createTime;
    private String updateBy;
    private Date updateTime;
}


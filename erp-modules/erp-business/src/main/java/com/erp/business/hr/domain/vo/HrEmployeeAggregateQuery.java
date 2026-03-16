package com.erp.business.hr.domain.vo;

import lombok.Data;

import java.io.Serializable;

/**
 * HR 员工台账聚合查询参数。
 */
@Data
public class HrEmployeeAggregateQuery implements Serializable {
    private static final long serialVersionUID = 1L;

    private String empCode;
    private String empName;
    private Long orgId;
    private Long deptId;
    private String position;
    private String status;
    private Long pageNum;
    private Long pageSize;
}

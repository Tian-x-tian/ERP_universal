package com.erp.system.domain.vo;

import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import java.io.Serializable;
import java.util.Date;

/**
 * 流程看板查询条件。
 */
@Data
public class WorkflowDashboardQueryVO implements Serializable {
    private static final long serialVersionUID = 1L;

    /** 流程标识关键字 */
    private String processKey;

    /** 流程分类 */
    private String category;

    /** 发起部门ID */
    private Long deptId;

    /** 开始时间 */
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date startTime;

    /** 结束时间 */
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date endTime;
}

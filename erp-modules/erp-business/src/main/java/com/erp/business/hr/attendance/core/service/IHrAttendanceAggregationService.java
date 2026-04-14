package com.erp.business.hr.attendance.core.service;

import java.time.LocalDate;

/**
 * 出勤汇总聚合服务接口。
 */
public interface IHrAttendanceAggregationService {

    /**
     * 重算指定员工的日汇总。
     *
     * @param employeeId 员工ID
     * @param workDate 工作日期
     * @param operator 操作人
     */
    void recalculateEmployeeDay(Long employeeId, LocalDate workDate, String operator);

    /**
     * 重算指定员工的月汇总。
     *
     * @param employeeId 员工ID
     * @param monthCode 月份编码
     * @param operator 操作人
     */
    void recalculateEmployeeMonth(Long employeeId, String monthCode, String operator);
}

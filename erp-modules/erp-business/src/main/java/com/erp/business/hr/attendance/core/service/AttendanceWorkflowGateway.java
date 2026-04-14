package com.erp.business.hr.attendance.core.service;

import com.erp.business.hr.attendance.core.domain.HrAttendanceLeaveOrder;
import com.erp.business.hr.attendance.core.domain.HrAttendanceOvertimeOrder;

/**
 * 出勤流程网关接口。
 */
public interface AttendanceWorkflowGateway {

    /**
     * 发起请假审批流程。
     *
     * @param order 请假单
     * @return true 表示发起成功
     */
    boolean startLeaveWorkflow(HrAttendanceLeaveOrder order);

    /**
     * 发起加班审批流程。
     *
     * @param order 加班单
     * @return true 表示发起成功
     */
    boolean startOvertimeWorkflow(HrAttendanceOvertimeOrder order);
}

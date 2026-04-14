package com.erp.business.hr.attendance.core.service;

/**
 * 出勤工作流桥接接口。
 */
public interface IHrAttendanceWorkflowBridgeService {

    /**
     * 回写请假审批通过结果。
     *
     * @param orderId 单据ID
     * @param operator 操作人
     */
    void onLeaveApproved(Long orderId, String operator);

    /**
     * 回写请假审批驳回或撤回结果。
     *
     * @param orderId 单据ID
     * @param operator 操作人
     */
    void onLeaveRejected(Long orderId, String operator);

    /**
     * 回写加班审批通过结果。
     *
     * @param orderId 单据ID
     * @param operator 操作人
     */
    void onOvertimeApproved(Long orderId, String operator);

    /**
     * 回写加班审批驳回或撤回结果。
     *
     * @param orderId 单据ID
     * @param operator 操作人
     */
    void onOvertimeRejected(Long orderId, String operator);
}

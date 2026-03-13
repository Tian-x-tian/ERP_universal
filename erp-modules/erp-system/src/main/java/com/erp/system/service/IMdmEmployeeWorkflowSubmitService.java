package com.erp.system.service;

import com.erp.system.domain.MdmEmployee;

/**
 * 员工主数据审批提交流程服务接口。
 */
public interface IMdmEmployeeWorkflowSubmitService {

    /**
     * 提交草稿员工生效审批。
     *
     * @param employeeId 员工ID
     * @param processKey 流程标识
     * @param remark 提交备注
     * @return true 表示提交成功
     */
    boolean submitDraftActivation(Long employeeId, String processKey, String remark);

    /**
     * 提交员工变更审批。
     *
     * @param employeeId 员工ID
     * @param targetEmployee 目标员工数据
     * @param processKey 流程标识
     * @param remark 提交备注
     * @return true 表示提交成功
     */
    boolean submitChange(Long employeeId, MdmEmployee targetEmployee, String processKey, String remark);

    /**
     * 提交员工离职审批。
     *
     * @param employeeId 员工ID
     * @param processKey 流程标识
     * @param remark 提交备注
     * @return true 表示提交成功
     */
    boolean submitLeave(Long employeeId, String processKey, String remark);
}

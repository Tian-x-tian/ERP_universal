package com.erp.business.hr.service;

/**
 * 员工工作流扩展桥接服务接口。
 */
public interface IHrEmployeeWorkflowBridgeService {

    /**
     * 审批通过后回写 HR 扩展数据。
     *
     * @param changeRecordId 异动记录ID
     * @param archivePayloadJson 审批通过后的扩展档案JSON
     * @param operator 操作人
     */
    void onChangeApproved(Long changeRecordId, String archivePayloadJson, String operator);

    /**
     * 审批驳回或撤回后回写 HR 异动状态。
     *
     * @param changeRecordId 异动记录ID
     * @param operator 操作人
     */
    void onChangeRejected(Long changeRecordId, String operator);
}

package com.erp.system.service;

import com.erp.workflow.contract.domain.vo.WorkflowInstanceDetailVO;
import com.erp.workflow.contract.domain.vo.WorkflowStartBody;
import com.erp.workflow.contract.domain.vo.WorkflowTaskActionBody;

/**
 * 系统模块工作流远程门面接口。
 */
public interface ISysWorkflowEngineService {

    /**
     * 查询指定业务的最新流程实例详情。
     *
     * @param businessType 业务类型
     * @param businessNo 业务单号
     * @return 流程实例详情
     */
    WorkflowInstanceDetailVO selectLatestInstanceDetail(String businessType, String businessNo);

    /**
     * 判断是否存在运行中的流程实例。
     *
     * @param businessType 业务类型
     * @param businessNo 业务单号
     * @return true 表示存在
     */
    boolean hasRunningInstance(String businessType, String businessNo);

    /**
     * 发起流程。
     *
     * @param startBody 发起参数
     * @param initiatorUserId 发起人用户ID
     * @param initiatorName 发起人账号
     * @param initiatorNick 发起人昵称
     * @return true 表示成功
     */
    boolean startProcess(WorkflowStartBody startBody, Long initiatorUserId, String initiatorName, String initiatorNick);

    /**
     * 中止指定业务的运行中流程实例。
     *
     * @param businessType 业务类型
     * @param businessNo 业务单号
     * @param remark 中止原因
     * @return true 表示成功
     */
    boolean abortProcess(String businessType, String businessNo, String remark);

    /**
     * 审批通过任务。
     *
     * @param taskId 任务ID
     * @param actionBody 审批参数
     * @param actionUserId 操作人ID
     * @param actionUserName 操作人账号
     * @param actionUserNick 操作人昵称
     * @return true 表示成功
     */
    boolean approveTask(Long taskId, WorkflowTaskActionBody actionBody, Long actionUserId, String actionUserName, String actionUserNick);

    /**
     * 驳回任务。
     *
     * @param taskId 任务ID
     * @param actionBody 审批参数
     * @param actionUserId 操作人ID
     * @param actionUserName 操作人账号
     * @param actionUserNick 操作人昵称
     * @return true 表示成功
     */
    boolean rejectTask(Long taskId, WorkflowTaskActionBody actionBody, Long actionUserId, String actionUserName, String actionUserNick);
}

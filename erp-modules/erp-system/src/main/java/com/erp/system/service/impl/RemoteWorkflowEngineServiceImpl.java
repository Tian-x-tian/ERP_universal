package com.erp.system.service.impl;

import com.erp.common.client.internal.InternalWorkflowClient;
import com.erp.system.service.ISysWorkflowEngineService;
import com.erp.workflow.contract.domain.vo.WorkflowInstanceDetailVO;
import com.erp.workflow.contract.domain.vo.WorkflowStartBody;
import com.erp.workflow.contract.domain.vo.WorkflowTaskActionBody;
import org.springframework.stereotype.Service;

/**
 * 系统模块工作流远程门面实现。
 */
@Service
public class RemoteWorkflowEngineServiceImpl implements ISysWorkflowEngineService {
    private final InternalWorkflowClient internalWorkflowClient;

    public RemoteWorkflowEngineServiceImpl(InternalWorkflowClient internalWorkflowClient) {
        this.internalWorkflowClient = internalWorkflowClient;
    }

    /**
     * 查询指定业务的最新流程实例详情。
     *
     * @param businessType 业务类型
     * @param businessNo 业务单号
     * @return 流程实例详情
     */
    @Override
    public WorkflowInstanceDetailVO selectLatestInstanceDetail(String businessType, String businessNo) {
        return internalWorkflowClient.getLatestInstanceDetail(businessType, businessNo);
    }

    /**
     * 判断是否存在运行中的流程实例。
     *
     * @param businessType 业务类型
     * @param businessNo 业务单号
     * @return true 表示存在
     */
    @Override
    public boolean hasRunningInstance(String businessType, String businessNo) {
        return internalWorkflowClient.hasRunningInstance(businessType, businessNo);
    }

    /**
     * 发起流程。
     *
     * @param startBody 发起参数
     * @param initiatorUserId 发起人用户ID
     * @param initiatorName 发起人账号
     * @param initiatorNick 发起人昵称
     * @return true 表示成功
     */
    @Override
    public boolean startProcess(WorkflowStartBody startBody, Long initiatorUserId, String initiatorName, String initiatorNick) {
        return internalWorkflowClient.startProcess(startBody);
    }

    /**
     * 中止指定业务的运行中流程实例。
     *
     * @param businessType 业务类型
     * @param businessNo 业务单号
     * @param remark 中止原因
     * @return true 表示成功
     */
    @Override
    public boolean abortProcess(String businessType, String businessNo, String remark) {
        WorkflowTaskActionBody actionBody = new WorkflowTaskActionBody();
        actionBody.setActionComment(remark);
        return internalWorkflowClient.abortProcess(businessType, businessNo, actionBody);
    }

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
    @Override
    public boolean approveTask(Long taskId, WorkflowTaskActionBody actionBody, Long actionUserId, String actionUserName, String actionUserNick) {
        return internalWorkflowClient.approveTask(taskId, actionBody);
    }

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
    @Override
    public boolean rejectTask(Long taskId, WorkflowTaskActionBody actionBody, Long actionUserId, String actionUserName, String actionUserNick) {
        return internalWorkflowClient.rejectTask(taskId, actionBody);
    }
}

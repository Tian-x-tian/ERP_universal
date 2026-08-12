package com.erp.workflow.service;

import com.erp.workflow.contract.domain.SysWorkflowInstance;
import com.erp.workflow.contract.domain.SysWorkflowTask;
import com.erp.workflow.contract.domain.vo.WorkflowInstanceDetailVO;
import com.erp.workflow.contract.domain.vo.WorkflowStartBody;
import com.erp.workflow.contract.domain.vo.WorkflowTaskActionBody;
import com.erp.workflow.contract.domain.vo.WorkflowTaskFormVO;
import com.erp.workflow.contract.domain.vo.WorkflowTaskRemindBody;
import com.erp.workflow.contract.domain.vo.WorkflowTaskReturnBody;
import com.erp.workflow.contract.domain.vo.WorkflowTaskTransferBody;
import com.erp.workflow.contract.domain.vo.WorkflowSlaScanResultVO;

import java.util.List;

/**
 * 流程引擎服务接口
 */
public interface ISysWorkflowEngineService {

    /**
     * 查询流程实例列表。
     *
     * @param processKey 流程标识关键字
     * @param status     实例状态
     * @param businessNo 业务单号关键字
     * @return 流程实例列表
     */
    List<SysWorkflowInstance> selectInstanceList(String processKey, String status, String businessNo);

    /**
     * 查询流程实例详情。
     *
     * @param instanceId 流程实例ID
     * @return 流程实例详情
     */
    WorkflowInstanceDetailVO selectInstanceDetail(Long instanceId);

    /**
     * 发起流程实例。
     *
     * @param startBody      发起参数
     * @param initiatorUserId 发起人用户ID
     * @param initiatorName   发起人账号
     * @param initiatorNick   发起人昵称
     * @return 发起结果
     */
    boolean startProcess(WorkflowStartBody startBody, Long initiatorUserId, String initiatorName, String initiatorNick);

    /**
     * 查询当前用户流程任务列表。
     *
     * @param userId 当前用户ID
     * @param status 任务状态
     * @return 任务列表
     */
    List<SysWorkflowTask> selectMyTaskList(Long userId, String status);

    /**
     * 签收流程任务。
     *
     * @param taskId          任务ID
     * @param actionUserId    操作人用户ID
     * @param actionUserName  操作人账号
     * @param actionUserNick  操作人昵称
     * @return 签收结果
     */
    boolean claimTask(Long taskId, Long actionUserId, String actionUserName, String actionUserNick);

    /**
     * 同意审批任务。
     *
     * @param taskId          任务ID
     * @param actionBody      审批参数
     * @param actionUserId    操作人用户ID
     * @param actionUserName  操作人账号
     * @param actionUserNick  操作人昵称
     * @return 处理结果
     */
    boolean approveTask(Long taskId, WorkflowTaskActionBody actionBody, Long actionUserId, String actionUserName, String actionUserNick);

    /**
     * 驳回审批任务。
     *
     * @param taskId          任务ID
     * @param actionBody      审批参数
     * @param actionUserId    操作人用户ID
     * @param actionUserName  操作人账号
     * @param actionUserNick  操作人昵称
     * @return 处理结果
     */
    boolean rejectTask(Long taskId, WorkflowTaskActionBody actionBody, Long actionUserId, String actionUserName, String actionUserNick);

    /**
     * 转交审批任务。
     *
     * @param taskId          任务ID
     * @param transferBody    转交参数
     * @param actionUserId    操作人用户ID
     * @param actionUserName  操作人账号
     * @param actionUserNick  操作人昵称
     * @return 转交结果
     */
    boolean transferTask(Long taskId, WorkflowTaskTransferBody transferBody, Long actionUserId, String actionUserName, String actionUserNick);

    /**
     * 撤回流程实例。
     *
     * @param instanceId      流程实例ID
     * @param actionBody      撤回参数
     * @param actionUserId    操作人用户ID
     * @param actionUserName  操作人账号
     * @param actionUserNick  操作人昵称
     * @return 撤回结果
     */
    boolean withdrawInstance(Long instanceId, WorkflowTaskActionBody actionBody, Long actionUserId, String actionUserName, String actionUserNick);

    /**
     * 退回流程任务到指定节点。
     *
     * @param taskId          任务ID
     * @param returnBody      退回参数
     * @param actionUserId    操作人用户ID
     * @param actionUserName  操作人账号
     * @param actionUserNick  操作人昵称
     * @return 退回结果
     */
    boolean returnTask(Long taskId, WorkflowTaskReturnBody returnBody, Long actionUserId, String actionUserName, String actionUserNick);

    /**
     * 加签。
     *
     * @param taskId          任务ID
     * @param transferBody    加签参数
     * @param actionUserId    操作人用户ID
     * @param actionUserName  操作人账号
     * @param actionUserNick  操作人昵称
     * @return 加签结果
     */
    boolean addSign(Long taskId, WorkflowTaskTransferBody transferBody, Long actionUserId, String actionUserName, String actionUserNick);

    /**
     * 减签。
     *
     * @param taskId          任务ID
     * @param transferBody    减签参数
     * @param actionUserId    操作人用户ID
     * @param actionUserName  操作人账号
     * @param actionUserNick  操作人昵称
     * @return 减签结果
     */
    boolean removeSign(Long taskId, WorkflowTaskTransferBody transferBody, Long actionUserId, String actionUserName, String actionUserNick);

    /**
     * 委派流程任务。
     *
     * @param taskId          任务ID
     * @param transferBody    委派参数
     * @param actionUserId    操作人用户ID
     * @param actionUserName  操作人账号
     * @param actionUserNick  操作人昵称
     * @return 委派结果
     */
    boolean delegateTask(Long taskId, WorkflowTaskTransferBody transferBody, Long actionUserId, String actionUserName, String actionUserNick);

    /**
     * 催办流程任务。
     *
     * @param taskId          任务ID
     * @param remindBody      催办参数
     * @param actionUserId    操作人用户ID
     * @param actionUserName  操作人账号
     * @param actionUserNick  操作人昵称
     * @return 催办结果
     */
    boolean remindTask(Long taskId, WorkflowTaskRemindBody remindBody, Long actionUserId, String actionUserName, String actionUserNick);

    /**
     * 查询任务节点动态表单。
     *
     * @param taskId 任务ID
     * @param userId 当前用户ID
     * @return 动态表单
     */
    WorkflowTaskFormVO selectTaskForm(Long taskId, Long userId);

    /**
     * 执行流程任务 SLA 扫描。
     *
     * @return 扫描结果
     */
    WorkflowSlaScanResultVO scanTimeoutTasks();
}



package com.erp.system.service;

import com.erp.system.domain.SysWorkflowInstance;
import com.erp.system.domain.SysWorkflowTask;
import com.erp.system.domain.vo.WorkflowInstanceDetailVO;
import com.erp.system.domain.vo.WorkflowStartBody;
import com.erp.system.domain.vo.WorkflowTaskActionBody;
import com.erp.system.domain.vo.WorkflowTaskTransferBody;

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
}


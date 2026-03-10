package com.erp.system.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.erp.system.domain.SysNotice;
import com.erp.system.domain.SysTodoTask;
import com.erp.system.domain.SysUser;
import com.erp.system.domain.SysWorkflowDefinition;
import com.erp.system.domain.SysWorkflowInstance;
import com.erp.system.domain.SysWorkflowTask;
import com.erp.system.domain.SysWorkflowTaskAction;
import com.erp.system.domain.vo.WorkflowInstanceDetailVO;
import com.erp.system.domain.vo.WorkflowStartBody;
import com.erp.system.domain.vo.WorkflowTaskActionBody;
import com.erp.system.domain.vo.WorkflowTaskTransferBody;
import com.erp.system.mapper.SysWorkflowInstanceMapper;
import com.erp.system.mapper.SysWorkflowTaskActionMapper;
import com.erp.system.mapper.SysWorkflowTaskMapper;
import com.erp.system.service.ISysNoticeService;
import com.erp.system.service.ISysTodoTaskService;
import com.erp.system.service.ISysUserService;
import com.erp.system.service.ISysWorkflowDefinitionService;
import com.erp.system.service.ISysWorkflowEngineService;
import com.erp.system.support.TenantWriteGuard;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.Collections;
import java.util.Date;
import java.util.List;

/**
 * 流程引擎服务实现
 */
@Service
public class SysWorkflowEngineServiceImpl implements ISysWorkflowEngineService {

    private static final String INSTANCE_STATUS_RUNNING = "0";
    private static final String INSTANCE_STATUS_COMPLETED = "1";
    private static final String INSTANCE_STATUS_REJECTED = "2";

    private static final String TASK_STATUS_PENDING = "0";
    private static final String TASK_STATUS_PROCESSING = "1";
    private static final String TASK_STATUS_APPROVED = "2";
    private static final String TASK_STATUS_REJECTED = "3";
    private static final String TASK_STATUS_TRANSFERRED = "4";

    private static final String TODO_STATUS_PENDING = "0";
    private static final String TODO_STATUS_PROCESSING = "1";
    private static final String TODO_STATUS_FINISHED = "2";

    private static final String ACTION_START = "START";
    private static final String ACTION_APPROVE = "APPROVE";
    private static final String ACTION_REJECT = "REJECT";
    private static final String ACTION_TRANSFER = "TRANSFER";

    private final SysWorkflowInstanceMapper workflowInstanceMapper;
    private final SysWorkflowTaskMapper workflowTaskMapper;
    private final SysWorkflowTaskActionMapper workflowTaskActionMapper;
    private final ISysWorkflowDefinitionService workflowDefinitionService;
    private final ISysTodoTaskService todoTaskService;
    private final ISysNoticeService noticeService;
    private final ISysUserService userService;

    public SysWorkflowEngineServiceImpl(
            SysWorkflowInstanceMapper workflowInstanceMapper,
            SysWorkflowTaskMapper workflowTaskMapper,
            SysWorkflowTaskActionMapper workflowTaskActionMapper,
            ISysWorkflowDefinitionService workflowDefinitionService,
            ISysTodoTaskService todoTaskService,
            ISysNoticeService noticeService,
            ISysUserService userService) {
        this.workflowInstanceMapper = workflowInstanceMapper;
        this.workflowTaskMapper = workflowTaskMapper;
        this.workflowTaskActionMapper = workflowTaskActionMapper;
        this.workflowDefinitionService = workflowDefinitionService;
        this.todoTaskService = todoTaskService;
        this.noticeService = noticeService;
        this.userService = userService;
    }

    /**
     * 查询流程实例列表。
     *
     * @param processKey 流程标识关键字
     * @param status     实例状态
     * @param businessNo 业务单号关键字
     * @return 流程实例列表
     */
    @Override
    public List<SysWorkflowInstance> selectInstanceList(String processKey, String status, String businessNo) {
        LambdaQueryWrapper<SysWorkflowInstance> queryWrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(processKey)) {
            queryWrapper.like(SysWorkflowInstance::getProcessKey, processKey.trim());
        }
        if (StringUtils.hasText(status)) {
            queryWrapper.eq(SysWorkflowInstance::getStatus, status.trim());
        }
        if (StringUtils.hasText(businessNo)) {
            queryWrapper.like(SysWorkflowInstance::getBusinessNo, businessNo.trim());
        }
        queryWrapper.orderByDesc(SysWorkflowInstance::getStartTime)
                .orderByDesc(SysWorkflowInstance::getInstanceId);
        return workflowInstanceMapper.selectList(queryWrapper);
    }

    /**
     * 查询流程实例详情。
     *
     * @param instanceId 流程实例ID
     * @return 流程实例详情
     */
    @Override
    public WorkflowInstanceDetailVO selectInstanceDetail(Long instanceId) {
        if (instanceId == null) {
            return null;
        }
        SysWorkflowInstance instance = workflowInstanceMapper.selectById(instanceId);
        if (instance == null) {
            return null;
        }
        WorkflowInstanceDetailVO detailVO = new WorkflowInstanceDetailVO();
        detailVO.setInstance(instance);
        detailVO.setTaskList(workflowTaskMapper.selectList(new LambdaQueryWrapper<SysWorkflowTask>()
                .eq(SysWorkflowTask::getInstanceId, instanceId)
                .orderByAsc(SysWorkflowTask::getCreateTime)
                .orderByAsc(SysWorkflowTask::getTaskId)));
        detailVO.setActionList(workflowTaskActionMapper.selectList(new LambdaQueryWrapper<SysWorkflowTaskAction>()
                .eq(SysWorkflowTaskAction::getInstanceId, instanceId)
                .orderByAsc(SysWorkflowTaskAction::getActionTime)
                .orderByAsc(SysWorkflowTaskAction::getActionId)));
        return detailVO;
    }

    /**
     * 发起流程实例并生成首个审批任务与待办。
     *
     * @param startBody       发起参数
     * @param initiatorUserId 发起人用户ID
     * @param initiatorName   发起人账号
     * @param initiatorNick   发起人昵称
     * @return 发起结果
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean startProcess(WorkflowStartBody startBody, Long initiatorUserId, String initiatorName, String initiatorNick) {
        if (startBody == null || initiatorUserId == null
                || !StringUtils.hasText(initiatorName)
                || !StringUtils.hasText(startBody.getProcessKey())
                || !StringUtils.hasText(startBody.getBusinessNo())) {
            return false;
        }
        SysWorkflowDefinition definition = workflowDefinitionService.selectLatestPublishedByProcessKey(startBody.getProcessKey());
        if (definition == null) {
            return false;
        }

        Date now = new Date();
        String tenantId = resolveTenantId();
        AssigneeInfo assigneeInfo = resolveAssignee(startBody, initiatorUserId, initiatorName, initiatorNick);

        SysWorkflowInstance instance = new SysWorkflowInstance();
        instance.setTenantId(tenantId);
        instance.setDefinitionId(definition.getDefinitionId());
        instance.setProcessKey(definition.getProcessKey());
        instance.setProcessName(definition.getProcessName());
        instance.setCategory(definition.getCategory());
        instance.setBusinessNo(startBody.getBusinessNo().trim());
        instance.setBusinessType(normalizeText(startBody.getBusinessType()));
        instance.setFormData(startBody.getFormData());
        instance.setCurrentNode(resolveNodeName(startBody.getNodeName()));
        instance.setInitiatorUserId(initiatorUserId);
        instance.setInitiatorUserName(initiatorName.trim());
        instance.setInitiatorNickName(normalizeText(initiatorNick));
        instance.setStatus(INSTANCE_STATUS_RUNNING);
        instance.setStartTime(now);
        instance.setLastAction(ACTION_START);
        instance.setLastActionUserId(initiatorUserId);
        instance.setLastActionUserName(initiatorName.trim());
        instance.setLastActionTime(now);
        instance.setRemark(normalizeText(startBody.getRemark()));
        if (workflowInstanceMapper.insert(instance) <= 0) {
            return false;
        }

        SysTodoTask todoTask = new SysTodoTask();
        todoTask.setTenantId(tenantId);
        todoTask.setProcessName(instance.getProcessName());
        todoTask.setNodeName(instance.getCurrentNode());
        todoTask.setBusinessNo(instance.getBusinessNo());
        todoTask.setPriority(normalizePriority(startBody.getPriority()));
        todoTask.setStatus(TODO_STATUS_PENDING);
        todoTask.setAssigneeUserId(assigneeInfo.userId);
        todoTask.setDueTime(startBody.getDueTime());
        todoTask.setCreateTime(now);
        todoTask.setRemark("流程发起自动创建");
        if (!todoTaskService.save(todoTask)) {
            return false;
        }

        SysWorkflowTask workflowTask = new SysWorkflowTask();
        workflowTask.setTenantId(tenantId);
        workflowTask.setInstanceId(instance.getInstanceId());
        workflowTask.setDefinitionId(definition.getDefinitionId());
        workflowTask.setNodeKey("NODE_1");
        workflowTask.setNodeName(instance.getCurrentNode());
        workflowTask.setCandidateUserIds(String.valueOf(assigneeInfo.userId));
        workflowTask.setAssigneeUserId(assigneeInfo.userId);
        workflowTask.setAssigneeUserName(assigneeInfo.userName);
        workflowTask.setAssigneeNickName(assigneeInfo.nickName);
        workflowTask.setStatus(TASK_STATUS_PENDING);
        workflowTask.setTodoId(todoTask.getTodoId());
        workflowTask.setDueTime(startBody.getDueTime());
        workflowTask.setCreateTime(now);
        if (workflowTaskMapper.insert(workflowTask) <= 0) {
            return false;
        }

        recordTaskAction(instance, workflowTask, ACTION_START, initiatorUserId, initiatorName, initiatorNick, null, null, "流程发起");
        pushNoticeToUser(tenantId, assigneeInfo.userId,
                "收到待审批任务：" + instance.getProcessName(),
                "审批通知",
                "流程引擎",
                instance.getBusinessNo(),
                "流程【" + instance.getProcessName() + "】已发起，请及时处理审批任务。");
        return true;
    }

    /**
     * 查询当前用户流程任务列表。
     *
     * @param userId 当前用户ID
     * @param status 任务状态
     * @return 任务列表
     */
    @Override
    public List<SysWorkflowTask> selectMyTaskList(Long userId, String status) {
        if (userId == null) {
            return Collections.emptyList();
        }
        LambdaQueryWrapper<SysWorkflowTask> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(SysWorkflowTask::getAssigneeUserId, userId);
        if (StringUtils.hasText(status)) {
            queryWrapper.eq(SysWorkflowTask::getStatus, status.trim());
        }
        queryWrapper.orderByDesc(SysWorkflowTask::getCreateTime)
                .orderByDesc(SysWorkflowTask::getTaskId);
        return workflowTaskMapper.selectList(queryWrapper);
    }

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
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean approveTask(Long taskId, WorkflowTaskActionBody actionBody, Long actionUserId, String actionUserName, String actionUserNick) {
        SysWorkflowTask task = workflowTaskMapper.selectById(taskId);
        if (!canHandleTask(task, actionUserId)) {
            return false;
        }
        Date now = new Date();
        String actionComment = normalizeText(actionBody == null ? null : actionBody.getActionComment());

        SysWorkflowTask updateTask = new SysWorkflowTask();
        updateTask.setTaskId(task.getTaskId());
        updateTask.setStatus(TASK_STATUS_APPROVED);
        updateTask.setActionComment(actionComment);
        updateTask.setClaimTime(task.getClaimTime() == null ? now : task.getClaimTime());
        updateTask.setFinishTime(now);
        if (workflowTaskMapper.updateById(updateTask) <= 0) {
            return false;
        }

        finishTodoTask(task.getTodoId(), actionUserId, "审批通过");
        updateInstanceStatus(task.getInstanceId(), INSTANCE_STATUS_COMPLETED, "完成", ACTION_APPROVE, actionUserId, actionUserName, now);

        SysWorkflowInstance instance = workflowInstanceMapper.selectById(task.getInstanceId());
        if (instance == null) {
            return false;
        }
        recordTaskAction(instance, task, ACTION_APPROVE, actionUserId, actionUserName, actionUserNick, null, null, actionComment);
        pushNoticeToUser(instance.getTenantId(), instance.getInitiatorUserId(),
                "审批结果通知：" + instance.getProcessName(),
                "审批通知",
                "流程引擎",
                instance.getBusinessNo(),
                "流程【" + instance.getProcessName() + "】已审批通过。");
        return true;
    }

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
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean rejectTask(Long taskId, WorkflowTaskActionBody actionBody, Long actionUserId, String actionUserName, String actionUserNick) {
        SysWorkflowTask task = workflowTaskMapper.selectById(taskId);
        if (!canHandleTask(task, actionUserId)) {
            return false;
        }
        Date now = new Date();
        String actionComment = normalizeText(actionBody == null ? null : actionBody.getActionComment());

        SysWorkflowTask updateTask = new SysWorkflowTask();
        updateTask.setTaskId(task.getTaskId());
        updateTask.setStatus(TASK_STATUS_REJECTED);
        updateTask.setActionComment(actionComment);
        updateTask.setClaimTime(task.getClaimTime() == null ? now : task.getClaimTime());
        updateTask.setFinishTime(now);
        if (workflowTaskMapper.updateById(updateTask) <= 0) {
            return false;
        }

        finishTodoTask(task.getTodoId(), actionUserId, "审批驳回");
        updateInstanceStatus(task.getInstanceId(), INSTANCE_STATUS_REJECTED, "已驳回", ACTION_REJECT, actionUserId, actionUserName, now);

        SysWorkflowInstance instance = workflowInstanceMapper.selectById(task.getInstanceId());
        if (instance == null) {
            return false;
        }
        recordTaskAction(instance, task, ACTION_REJECT, actionUserId, actionUserName, actionUserNick, null, null, actionComment);
        pushNoticeToUser(instance.getTenantId(), instance.getInitiatorUserId(),
                "审批结果通知：" + instance.getProcessName(),
                "审批通知",
                "流程引擎",
                instance.getBusinessNo(),
                "流程【" + instance.getProcessName() + "】已驳回，请修改后重新发起。");
        return true;
    }

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
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean transferTask(Long taskId, WorkflowTaskTransferBody transferBody, Long actionUserId, String actionUserName, String actionUserNick) {
        SysWorkflowTask task = workflowTaskMapper.selectById(taskId);
        if (!canHandleTask(task, actionUserId) || transferBody == null || transferBody.getTargetUserId() == null) {
            return false;
        }
        if (transferBody.getTargetUserId().equals(actionUserId)) {
            return false;
        }
        Date now = new Date();

        SysWorkflowTask updateTask = new SysWorkflowTask();
        updateTask.setTaskId(task.getTaskId());
        updateTask.setStatus(TASK_STATUS_TRANSFERRED);
        updateTask.setActionComment(normalizeText(transferBody.getActionComment()));
        updateTask.setClaimTime(task.getClaimTime() == null ? now : task.getClaimTime());
        updateTask.setFinishTime(now);
        if (workflowTaskMapper.updateById(updateTask) <= 0) {
            return false;
        }

        finishTodoTask(task.getTodoId(), actionUserId, "审批任务已转交");

        SysWorkflowInstance instance = workflowInstanceMapper.selectById(task.getInstanceId());
        if (instance == null) {
            return false;
        }

        AssigneeInfo targetAssignee = resolveTransferAssignee(transferBody);
        SysTodoTask newTodoTask = new SysTodoTask();
        newTodoTask.setTenantId(instance.getTenantId());
        newTodoTask.setProcessName(instance.getProcessName());
        newTodoTask.setNodeName(task.getNodeName());
        newTodoTask.setBusinessNo(instance.getBusinessNo());
        newTodoTask.setPriority("M");
        newTodoTask.setStatus(TODO_STATUS_PENDING);
        newTodoTask.setAssigneeUserId(targetAssignee.userId);
        newTodoTask.setDueTime(task.getDueTime());
        newTodoTask.setCreateTime(now);
        newTodoTask.setRemark("任务由用户 " + actionUserName + " 转交");
        if (!todoTaskService.save(newTodoTask)) {
            return false;
        }

        SysWorkflowTask newTask = new SysWorkflowTask();
        newTask.setTenantId(task.getTenantId());
        newTask.setInstanceId(task.getInstanceId());
        newTask.setDefinitionId(task.getDefinitionId());
        newTask.setNodeKey(task.getNodeKey());
        newTask.setNodeName(task.getNodeName());
        newTask.setCandidateUserIds(String.valueOf(targetAssignee.userId));
        newTask.setAssigneeUserId(targetAssignee.userId);
        newTask.setAssigneeUserName(targetAssignee.userName);
        newTask.setAssigneeNickName(targetAssignee.nickName);
        newTask.setStatus(TASK_STATUS_PENDING);
        newTask.setTodoId(newTodoTask.getTodoId());
        newTask.setDueTime(task.getDueTime());
        newTask.setCreateTime(now);
        if (workflowTaskMapper.insert(newTask) <= 0) {
            return false;
        }

        updateInstanceStatus(task.getInstanceId(), INSTANCE_STATUS_RUNNING, task.getNodeName(), ACTION_TRANSFER, actionUserId, actionUserName, now);
        recordTaskAction(instance, task, ACTION_TRANSFER, actionUserId, actionUserName, actionUserNick,
                actionUserId, targetAssignee.userId, normalizeText(transferBody.getActionComment()));
        pushNoticeToUser(instance.getTenantId(), targetAssignee.userId,
                "收到转交流程任务：" + instance.getProcessName(),
                "审批通知",
                "流程引擎",
                instance.getBusinessNo(),
                "流程【" + instance.getProcessName() + "】有新的转交任务，请及时处理。");
        return true;
    }

    /**
     * 校验当前任务是否允许被当前用户处理。
     *
     * @param task   流程任务
     * @param userId 当前用户ID
     * @return true 表示允许处理
     */
    private boolean canHandleTask(SysWorkflowTask task, Long userId) {
        if (task == null || userId == null || task.getAssigneeUserId() == null) {
            return false;
        }
        if (!userId.equals(task.getAssigneeUserId())) {
            return false;
        }
        return TASK_STATUS_PENDING.equals(task.getStatus()) || TASK_STATUS_PROCESSING.equals(task.getStatus());
    }

    /**
     * 将待办状态置为完成。
     *
     * @param todoId         待办ID
     * @param actionUserId   操作人用户ID
     * @param finishRemark   完成说明
     */
    private void finishTodoTask(Long todoId, Long actionUserId, String finishRemark) {
        if (todoId == null || actionUserId == null) {
            return;
        }
        SysTodoTask updateTodo = new SysTodoTask();
        updateTodo.setTodoId(todoId);
        updateTodo.setStatus(TODO_STATUS_FINISHED);
        updateTodo.setClaimTime(new Date());
        updateTodo.setFinishTime(new Date());
        updateTodo.setRemark(finishRemark);
        todoTaskService.updateById(updateTodo);
    }

    /**
     * 更新流程实例状态与最近动作信息。
     *
     * @param instanceId      流程实例ID
     * @param status          流程状态
     * @param currentNode     当前节点
     * @param actionType      最近动作类型
     * @param actionUserId    最近动作人用户ID
     * @param actionUserName  最近动作人账号
     * @param actionTime      最近动作时间
     */
    private void updateInstanceStatus(Long instanceId, String status, String currentNode, String actionType,
                                      Long actionUserId, String actionUserName, Date actionTime) {
        if (instanceId == null) {
            return;
        }
        SysWorkflowInstance updateInstance = new SysWorkflowInstance();
        updateInstance.setInstanceId(instanceId);
        updateInstance.setStatus(status);
        updateInstance.setCurrentNode(currentNode);
        updateInstance.setLastAction(actionType);
        updateInstance.setLastActionUserId(actionUserId);
        updateInstance.setLastActionUserName(actionUserName);
        updateInstance.setLastActionTime(actionTime);
        if (INSTANCE_STATUS_COMPLETED.equals(status) || INSTANCE_STATUS_REJECTED.equals(status)) {
            updateInstance.setFinishTime(actionTime);
        }
        workflowInstanceMapper.updateById(updateInstance);
    }

    /**
     * 记录流程任务动作日志。
     *
     * @param instance            流程实例
     * @param task                流程任务
     * @param actionType          动作类型
     * @param actionUserId        动作人用户ID
     * @param actionUserName      动作人账号
     * @param actionUserNick      动作人昵称
     * @param fromAssigneeUserId  来源办理人用户ID
     * @param toAssigneeUserId    目标办理人用户ID
     * @param comment             动作意见
     */
    private void recordTaskAction(SysWorkflowInstance instance, SysWorkflowTask task, String actionType,
                                  Long actionUserId, String actionUserName, String actionUserNick,
                                  Long fromAssigneeUserId, Long toAssigneeUserId, String comment) {
        if (instance == null) {
            return;
        }
        SysWorkflowTaskAction action = new SysWorkflowTaskAction();
        action.setTenantId(instance.getTenantId());
        action.setInstanceId(instance.getInstanceId());
        action.setTaskId(task == null ? null : task.getTaskId());
        action.setDefinitionId(instance.getDefinitionId());
        action.setNodeName(task == null ? instance.getCurrentNode() : task.getNodeName());
        action.setActionType(actionType);
        action.setActionUserId(actionUserId);
        action.setActionUserName(normalizeText(actionUserName));
        action.setActionNickName(normalizeText(actionUserNick));
        action.setFromAssigneeUserId(fromAssigneeUserId);
        action.setToAssigneeUserId(toAssigneeUserId);
        action.setActionComment(normalizeText(comment));
        action.setActionTime(new Date());
        workflowTaskActionMapper.insert(action);
    }

    /**
     * 发送流程消息通知到指定用户。
     *
     * @param tenantId       租户编号
     * @param receiverUserId 接收人用户ID
     * @param title          消息标题
     * @param noticeType     消息类型
     * @param source         消息来源
     * @param businessNo     业务单号
     * @param content        消息内容
     */
    private void pushNoticeToUser(String tenantId, Long receiverUserId, String title, String noticeType,
                                  String source, String businessNo, String content) {
        if (!StringUtils.hasText(tenantId) || receiverUserId == null || !StringUtils.hasText(title)) {
            return;
        }
        SysNotice notice = new SysNotice();
        notice.setTenantId(tenantId);
        notice.setTitle(title.trim());
        notice.setNoticeType(noticeType);
        notice.setSource(source);
        notice.setBusinessNo(businessNo);
        notice.setContent(content);
        notice.setReceiverUserId(receiverUserId);
        notice.setStatus("0");
        noticeService.createNotice(notice);
    }

    /**
     * 解析流程发起时的审批人信息。
     *
     * @param startBody       发起参数
     * @param initiatorUserId 发起人用户ID
     * @param initiatorName   发起人账号
     * @param initiatorNick   发起人昵称
     * @return 审批人信息
     */
    private AssigneeInfo resolveAssignee(WorkflowStartBody startBody, Long initiatorUserId, String initiatorName, String initiatorNick) {
        if (startBody.getAssigneeUserId() == null) {
            return new AssigneeInfo(initiatorUserId, normalizeText(initiatorName), normalizeText(initiatorNick));
        }
        Long assigneeUserId = startBody.getAssigneeUserId();
        SysUser assigneeUser = userService.getById(assigneeUserId);
        if (assigneeUser != null) {
            return new AssigneeInfo(assigneeUserId, assigneeUser.getUserName(), assigneeUser.getNickName());
        }
        return new AssigneeInfo(
                assigneeUserId,
                normalizeText(startBody.getAssigneeUserName()),
                normalizeText(startBody.getAssigneeNickName()));
    }

    /**
     * 解析任务转交目标人信息。
     *
     * @param transferBody 转交参数
     * @return 目标办理人信息
     */
    private AssigneeInfo resolveTransferAssignee(WorkflowTaskTransferBody transferBody) {
        SysUser targetUser = userService.getById(transferBody.getTargetUserId());
        if (targetUser != null) {
            return new AssigneeInfo(targetUser.getUserId(), targetUser.getUserName(), targetUser.getNickName());
        }
        return new AssigneeInfo(transferBody.getTargetUserId(),
                normalizeText(transferBody.getTargetUserName()),
                normalizeText(transferBody.getTargetNickName()));
    }

    /**
     * 规范化节点名称。
     *
     * @param nodeName 原始节点名称
     * @return 节点名称
     */
    private String resolveNodeName(String nodeName) {
        return StringUtils.hasText(nodeName) ? nodeName.trim() : "一级审批";
    }

    /**
     * 规范化优先级。
     *
     * @param priority 原始优先级
     * @return 规范化优先级
     */
    private String normalizePriority(String priority) {
        if (!StringUtils.hasText(priority)) {
            return "M";
        }
        String normalized = priority.trim().toUpperCase();
        if ("H".equals(normalized) || "M".equals(normalized) || "L".equals(normalized)) {
            return normalized;
        }
        return "M";
    }

    /**
     * 将文本值规范化为去空格字符串。
     *
     * @param text 原始文本
     * @return 规范化文本
     */
    private String normalizeText(String text) {
        return StringUtils.hasText(text) ? text.trim() : null;
    }

    /**
     * 解析当前租户编号，缺失时回退到平台租户。
     *
     * @return 租户编号
     */
    private String resolveTenantId() {
        String tenantId = TenantWriteGuard.currentTenantId();
        return StringUtils.hasText(tenantId) ? tenantId : "000000";
    }

    /**
     * 审批人结构对象。
     */
    private static final class AssigneeInfo {
        private final Long userId;
        private final String userName;
        private final String nickName;

        /**
         * 构造审批人结构对象。
         *
         * @param userId   用户ID
         * @param userName 用户账号
         * @param nickName 用户昵称
         */
        private AssigneeInfo(Long userId, String userName, String nickName) {
            this.userId = userId;
            this.userName = userName;
            this.nickName = nickName;
        }
    }
}

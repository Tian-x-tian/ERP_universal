package com.erp.system.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.erp.system.domain.SysNotice;
import com.erp.system.domain.SysDept;
import com.erp.system.domain.SysTodoTask;
import com.erp.system.domain.SysUser;
import com.erp.system.domain.SysUserPost;
import com.erp.system.domain.SysUserRole;
import com.erp.system.domain.SysWorkflowDefinition;
import com.erp.system.domain.SysWorkflowInstance;
import com.erp.system.domain.SysWorkflowTask;
import com.erp.system.domain.SysWorkflowTaskAction;
import com.erp.system.domain.vo.WorkflowInstanceDetailVO;
import com.erp.system.domain.vo.WorkflowStartBody;
import com.erp.system.domain.vo.WorkflowTaskActionBody;
import com.erp.system.domain.vo.WorkflowTaskFormFieldVO;
import com.erp.system.domain.vo.WorkflowTaskFormVO;
import com.erp.system.domain.vo.WorkflowTaskRemindBody;
import com.erp.system.domain.vo.WorkflowTaskReturnBody;
import com.erp.system.domain.vo.WorkflowTaskTransferBody;
import com.erp.system.domain.vo.WorkflowSlaScanResultVO;
import com.erp.system.mapper.SysWorkflowInstanceMapper;
import com.erp.system.mapper.SysWorkflowTaskActionMapper;
import com.erp.system.mapper.SysWorkflowTaskMapper;
import com.erp.system.service.ISysDeptService;
import com.erp.system.service.ISysNoticeService;
import com.erp.system.service.ISysTodoTaskService;
import com.erp.system.service.ISysUserPostService;
import com.erp.system.service.ISysUserRoleService;
import com.erp.system.service.ISysUserService;
import com.erp.system.service.ISysWorkflowDefinitionService;
import com.erp.system.service.ISysWorkflowEngineService;
import com.erp.system.support.TenantWriteGuard;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * 流程引擎服务实现
 */
@Service
public class SysWorkflowEngineServiceImpl implements ISysWorkflowEngineService {

    private static final String INSTANCE_STATUS_RUNNING = "0";
    private static final String INSTANCE_STATUS_COMPLETED = "1";
    private static final String INSTANCE_STATUS_REJECTED = "2";
    private static final String INSTANCE_STATUS_WITHDRAWN = "3";

    private static final String TASK_STATUS_PENDING = "0";
    private static final String TASK_STATUS_PROCESSING = "1";
    private static final String TASK_STATUS_APPROVED = "2";
    private static final String TASK_STATUS_REJECTED = "3";
    private static final String TASK_STATUS_TRANSFERRED = "4";
    private static final String TASK_STATUS_CANCELED = "5";
    private static final String COUNTERSIGN_AUTO_FINISH_COMMENT = "会签节点任一审批通过，系统自动结束其他同节点任务";

    private static final String TODO_STATUS_PENDING = "0";
    private static final String TODO_STATUS_PROCESSING = "1";
    private static final String TODO_STATUS_FINISHED = "2";

    private static final String ACTION_START = "START";
    private static final String ACTION_CLAIM = "CLAIM";
    private static final String ACTION_APPROVE = "APPROVE";
    private static final String ACTION_REJECT = "REJECT";
    private static final String ACTION_TRANSFER = "TRANSFER";
    private static final String ACTION_WITHDRAW = "WITHDRAW";
    private static final String ACTION_RETURN = "RETURN";
    private static final String ACTION_ADD_SIGN = "ADD_SIGN";
    private static final String ACTION_REMOVE_SIGN = "REMOVE_SIGN";
    private static final String ACTION_DELEGATE = "DELEGATE";
    private static final String ACTION_REMIND = "REMIND";
    private static final String ACTION_CC = "CC";
    private static final String ACTION_TIMEOUT_REMIND = "TIMEOUT_REMIND";
    private static final String ACTION_TIMEOUT_ESCALATE = "TIMEOUT_ESCALATE";
    private static final String ACTION_TIMEOUT_TRANSFER = "TIMEOUT_TRANSFER";
    private static final Long SYSTEM_ACTION_USER_ID = 0L;
    private static final String SYSTEM_ACTION_USER_NAME = "system";
    private static final String SYSTEM_ACTION_USER_NICK = "系统调度";

    private final SysWorkflowInstanceMapper workflowInstanceMapper;
    private final SysWorkflowTaskMapper workflowTaskMapper;
    private final SysWorkflowTaskActionMapper workflowTaskActionMapper;
    private final ISysWorkflowDefinitionService workflowDefinitionService;
    private final ISysTodoTaskService todoTaskService;
    private final ISysNoticeService noticeService;
    private final ISysUserService userService;
    private final ISysDeptService deptService;
    private final ISysUserRoleService userRoleService;
    private final ISysUserPostService userPostService;
    private final ObjectMapper objectMapper;

    public SysWorkflowEngineServiceImpl(
            SysWorkflowInstanceMapper workflowInstanceMapper,
            SysWorkflowTaskMapper workflowTaskMapper,
            SysWorkflowTaskActionMapper workflowTaskActionMapper,
            ISysWorkflowDefinitionService workflowDefinitionService,
            ISysTodoTaskService todoTaskService,
            ISysNoticeService noticeService,
            ISysUserService userService,
            ISysDeptService deptService,
            ISysUserRoleService userRoleService,
            ISysUserPostService userPostService) {
        this.workflowInstanceMapper = workflowInstanceMapper;
        this.workflowTaskMapper = workflowTaskMapper;
        this.workflowTaskActionMapper = workflowTaskActionMapper;
        this.workflowDefinitionService = workflowDefinitionService;
        this.todoTaskService = todoTaskService;
        this.noticeService = noticeService;
        this.userService = userService;
        this.deptService = deptService;
        this.userRoleService = userRoleService;
        this.userPostService = userPostService;
        this.objectMapper = new ObjectMapper();
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
                || !StringUtils.hasText(startBody.getProcessKey())) {
            return false;
        }
        String processKey = startBody.getProcessKey().trim();
        SysWorkflowDefinition definition = workflowDefinitionService.selectLatestPublishedByProcessKey(processKey);
        if (definition == null) {
            return false;
        }

        Date now = new Date();
        WorkflowModel model = parseWorkflowModel(definition.getModelContent());
        if (!isRunnableStartModel(model)) {
            return false;
        }
        String tenantId = resolveTenantId();
        AssigneeInfo assigneeInfo = resolveAssignee(startBody, initiatorUserId, initiatorName, initiatorNick);
        String businessNo = resolveBusinessNo(startBody, definition, now);
        String businessType = resolveBusinessType(startBody.getBusinessType(), definition);
        String formData = normalizeJsonText(startBody.getFormData(), "{}");

        SysWorkflowInstance instance = new SysWorkflowInstance();
        instance.setTenantId(tenantId);
        instance.setDefinitionId(definition.getDefinitionId());
        instance.setDefinitionVersion(definition.getVersion());
        instance.setProcessKey(definition.getProcessKey());
        instance.setProcessName(definition.getProcessName());
        instance.setCategory(definition.getCategory());
        instance.setBusinessNo(businessNo);
        instance.setBusinessType(businessType);
        instance.setFormData(formData);
        instance.setFormSchemaSnapshot(definition.getFormSchema());
        instance.setModelContentSnapshot(definition.getModelContent());
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

        RouteResult routeResult = activateByModel(model,
                instance,
                model.resolveStartNodeKey(),
                parseJsonMap(instance.getFormData()),
                startBody.getDueTime(),
                assigneeInfo,
                initiateActionContext(initiatorUserId, initiatorName, initiatorNick));

        if (routeResult.createdTasks.isEmpty()) {
            return false;
        }
        SysWorkflowTask workflowTask = routeResult.createdTasks.get(0);
        updateInstanceStatus(instance.getInstanceId(),
                INSTANCE_STATUS_RUNNING,
                joinNodeNames(routeResult.activeNodeNames),
                ACTION_START,
                initiatorUserId,
                initiatorName,
                now);

        if (!recordTaskAction(instance, workflowTask, ACTION_START, initiatorUserId, initiatorName, initiatorNick, null, null, "流程发起")) {
            return false;
        }
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
     * 签收审批任务。
     *
     * @param taskId          任务ID
     * @param actionUserId    操作人用户ID
     * @param actionUserName  操作人账号
     * @param actionUserNick  操作人昵称
     * @return 签收结果
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean claimTask(Long taskId, Long actionUserId, String actionUserName, String actionUserNick) {
        if (taskId == null || actionUserId == null || !StringUtils.hasText(actionUserName)) {
            return false;
        }
        SysWorkflowTask task = workflowTaskMapper.selectById(taskId);
        if (task == null || task.getAssigneeUserId() == null || !actionUserId.equals(task.getAssigneeUserId())) {
            return false;
        }
        if (TASK_STATUS_PROCESSING.equals(task.getStatus())) {
            return true;
        }
        if (!TASK_STATUS_PENDING.equals(task.getStatus())) {
            return false;
        }
        SysWorkflowInstance instance = workflowInstanceMapper.selectById(task.getInstanceId());
        if (instance == null) {
            return false;
        }
        Date now = new Date();
        SysWorkflowTask updateTask = new SysWorkflowTask();
        updateTask.setTaskId(task.getTaskId());
        updateTask.setStatus(TASK_STATUS_PROCESSING);
        updateTask.setClaimTime(now);
        if (workflowTaskMapper.updateById(updateTask) <= 0) {
            return false;
        }

        if (task.getTodoId() != null) {
            SysTodoTask updateTodo = new SysTodoTask();
            updateTodo.setTodoId(task.getTodoId());
            updateTodo.setStatus(TODO_STATUS_PROCESSING);
            updateTodo.setClaimTime(now);
            updateTodo.setRemark("待办中心签收");
            todoTaskService.updateById(updateTodo);
        }

        if (!recordTaskAction(instance, task, ACTION_CLAIM, actionUserId, actionUserName, actionUserNick,
                null, task.getAssigneeUserId(), "待办中心签收")) {
            return false;
        }
        updateInstanceStatus(task.getInstanceId(),
                INSTANCE_STATUS_RUNNING,
                resolveRunningNodeName(task.getInstanceId(), task.getNodeName()),
                ACTION_CLAIM,
                actionUserId,
                actionUserName,
                now);
        return true;
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
        SysWorkflowInstance instance = workflowInstanceMapper.selectById(task.getInstanceId());
        if (instance == null) {
            return false;
        }
        if (!saveMergedFormData(instance, actionBody == null ? null : actionBody.getFormData(), task.getNodeKey(), task.getNodeName())) {
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
        recordTaskAction(instance, task, ACTION_APPROVE, actionUserId, actionUserName, actionUserNick, null, null, actionComment);

        WorkflowModel model = parseWorkflowModel(instance.getModelContentSnapshot());
        if (!canAdvanceAfterApprove(model, task, actionUserId, actionUserName, now)) {
            updateInstanceStatus(task.getInstanceId(),
                    INSTANCE_STATUS_RUNNING,
                    resolveRunningNodeName(task.getInstanceId(), task.getNodeName()),
                    ACTION_APPROVE,
                    actionUserId,
                    actionUserName,
                    now);
            return true;
        }
        SysWorkflowInstance latestInstance = workflowInstanceMapper.selectById(instance.getInstanceId());
        Map<String, Object> latestFormData = parseJsonMap(latestInstance == null ? instance.getFormData() : latestInstance.getFormData());
        RouteResult routeResult = activateByModel(model,
                instance,
                task.getNodeKey(),
                latestFormData,
                task.getDueTime(),
                resolveExplicitAssignee(instance.getInitiatorUserId(), instance.getInitiatorUserName(), instance.getInitiatorNickName()),
                initiateActionContext(actionUserId, actionUserName, actionUserNick));
        if (!routeResult.createdTasks.isEmpty()) {
            updateInstanceStatus(task.getInstanceId(),
                    INSTANCE_STATUS_RUNNING,
                    joinNodeNames(routeResult.activeNodeNames),
                    ACTION_APPROVE,
                    actionUserId,
                    actionUserName,
                    now);
            return true;
        }
        if (countPendingTask(task.getInstanceId()) > 0) {
            updateInstanceStatus(task.getInstanceId(),
                    INSTANCE_STATUS_RUNNING,
                    resolveRunningNodeName(task.getInstanceId(), task.getNodeName()),
                    ACTION_APPROVE,
                    actionUserId,
                    actionUserName,
                    now);
            return true;
        }
        updateInstanceStatus(task.getInstanceId(), INSTANCE_STATUS_COMPLETED, "完成", ACTION_APPROVE, actionUserId, actionUserName, now);
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
        SysWorkflowInstance instance = workflowInstanceMapper.selectById(task.getInstanceId());
        if (instance == null) {
            return false;
        }
        if (!saveMergedFormData(instance, actionBody == null ? null : actionBody.getFormData(), task.getNodeKey(), task.getNodeName())) {
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
        List<SysWorkflowTask> pendingTaskList = workflowTaskMapper.selectList(new LambdaQueryWrapper<SysWorkflowTask>()
                .eq(SysWorkflowTask::getInstanceId, task.getInstanceId())
                .in(SysWorkflowTask::getStatus, TASK_STATUS_PENDING, TASK_STATUS_PROCESSING));
        for (SysWorkflowTask pendingTask : pendingTaskList) {
            if (Objects.equals(pendingTask.getTaskId(), task.getTaskId())) {
                continue;
            }
            SysWorkflowTask cancelTask = new SysWorkflowTask();
            cancelTask.setTaskId(pendingTask.getTaskId());
            cancelTask.setStatus(TASK_STATUS_CANCELED);
            cancelTask.setActionComment("流程驳回关闭");
            cancelTask.setFinishTime(now);
            workflowTaskMapper.updateById(cancelTask);
            finishTodoTask(pendingTask.getTodoId(), actionUserId, "流程驳回关闭");
        }
        updateInstanceStatus(task.getInstanceId(), INSTANCE_STATUS_REJECTED, "已驳回", ACTION_REJECT, actionUserId, actionUserName, now);

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
     * 撤回流程实例。
     *
     * @param instanceId     流程实例ID
     * @param actionBody     撤回参数
     * @param actionUserId   操作人用户ID
     * @param actionUserName 操作人账号
     * @param actionUserNick 操作人昵称
     * @return 撤回结果
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean withdrawInstance(Long instanceId, WorkflowTaskActionBody actionBody, Long actionUserId, String actionUserName, String actionUserNick) {
        SysWorkflowInstance instance = workflowInstanceMapper.selectById(instanceId);
        if (instance == null || !INSTANCE_STATUS_RUNNING.equals(instance.getStatus())) {
            return false;
        }
        if (!Objects.equals(instance.getInitiatorUserId(), actionUserId)) {
            return false;
        }
        List<SysWorkflowTask> pendingTaskList = workflowTaskMapper.selectList(new LambdaQueryWrapper<SysWorkflowTask>()
                .eq(SysWorkflowTask::getInstanceId, instanceId)
                .in(SysWorkflowTask::getStatus, TASK_STATUS_PENDING, TASK_STATUS_PROCESSING));
        if (pendingTaskList.isEmpty()) {
            return false;
        }
        Date now = new Date();
        for (SysWorkflowTask pendingTask : pendingTaskList) {
            SysWorkflowTask cancelTask = new SysWorkflowTask();
            cancelTask.setTaskId(pendingTask.getTaskId());
            cancelTask.setStatus(TASK_STATUS_CANCELED);
            cancelTask.setActionComment("发起人撤回");
            cancelTask.setFinishTime(now);
            workflowTaskMapper.updateById(cancelTask);
            finishTodoTask(pendingTask.getTodoId(), actionUserId, "发起人撤回");
        }
        updateInstanceStatus(instanceId, INSTANCE_STATUS_WITHDRAWN, "已撤回", ACTION_WITHDRAW, actionUserId, actionUserName, now);
        recordTaskAction(instance, null, ACTION_WITHDRAW, actionUserId, actionUserName, actionUserNick, null, null,
                normalizeText(actionBody == null ? null : actionBody.getActionComment()));
        return true;
    }

    /**
     * 退回流程任务到指定节点。
     *
     * @param taskId         任务ID
     * @param returnBody     退回参数
     * @param actionUserId   操作人用户ID
     * @param actionUserName 操作人账号
     * @param actionUserNick 操作人昵称
     * @return 退回结果
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean returnTask(Long taskId, WorkflowTaskReturnBody returnBody, Long actionUserId, String actionUserName, String actionUserNick) {
        SysWorkflowTask currentTask = workflowTaskMapper.selectById(taskId);
        if (!canHandleTask(currentTask, actionUserId) || returnBody == null) {
            return false;
        }
        SysWorkflowInstance instance = workflowInstanceMapper.selectById(currentTask.getInstanceId());
        if (instance == null || !INSTANCE_STATUS_RUNNING.equals(instance.getStatus())) {
            return false;
        }
        if (!saveMergedFormData(instance, returnBody.getFormData(), currentTask.getNodeKey(), currentTask.getNodeName())) {
            return false;
        }

        Date now = new Date();
        SysWorkflowTask closeTask = new SysWorkflowTask();
        closeTask.setTaskId(currentTask.getTaskId());
        closeTask.setStatus(TASK_STATUS_CANCELED);
        closeTask.setActionComment(normalizeText(returnBody.getActionComment()));
        closeTask.setFinishTime(now);
        workflowTaskMapper.updateById(closeTask);
        finishTodoTask(currentTask.getTodoId(), actionUserId, "任务退回");

        AssigneeInfo targetAssignee = resolveExplicitAssignee(returnBody.getTargetUserId(), returnBody.getTargetUserName(), returnBody.getTargetNickName());
        if (targetAssignee == null) {
            targetAssignee = resolveAssigneeByNode(instance, returnBody.getTargetNodeKey(), returnBody.getTargetNodeName(), actionUserId, actionUserName, actionUserNick);
        }
        String targetNodeKey = StringUtils.hasText(returnBody.getTargetNodeKey()) ? returnBody.getTargetNodeKey().trim() : "RETURN_NODE";
        String targetNodeName = StringUtils.hasText(returnBody.getTargetNodeName()) ? returnBody.getTargetNodeName().trim() : "退回节点";
        SysWorkflowTask returnTask = createTaskForAssignee(instance, targetNodeKey, targetNodeName, targetAssignee, currentTask.getDueTime(), now);
        if (returnTask == null) {
            return false;
        }
        updateInstanceStatus(instance.getInstanceId(), INSTANCE_STATUS_RUNNING, targetNodeName, ACTION_RETURN, actionUserId, actionUserName, now);
        recordTaskAction(instance, currentTask, ACTION_RETURN, actionUserId, actionUserName, actionUserNick,
                currentTask.getAssigneeUserId(), targetAssignee.userId, normalizeText(returnBody.getActionComment()));
        return true;
    }

    /**
     * 加签。
     *
     * @param taskId         任务ID
     * @param transferBody   加签参数
     * @param actionUserId   操作人用户ID
     * @param actionUserName 操作人账号
     * @param actionUserNick 操作人昵称
     * @return 加签结果
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean addSign(Long taskId, WorkflowTaskTransferBody transferBody, Long actionUserId, String actionUserName, String actionUserNick) {
        SysWorkflowTask task = workflowTaskMapper.selectById(taskId);
        if (!canHandleTask(task, actionUserId) || transferBody == null || transferBody.getTargetUserId() == null) {
            return false;
        }
        if (Objects.equals(task.getAssigneeUserId(), transferBody.getTargetUserId())) {
            return false;
        }
        SysWorkflowInstance instance = workflowInstanceMapper.selectById(task.getInstanceId());
        if (instance == null) {
            return false;
        }
        AssigneeInfo targetAssignee = resolveTransferAssignee(transferBody);
        SysWorkflowTask signTask = createTaskForAssignee(instance, task.getNodeKey(), task.getNodeName(), targetAssignee, task.getDueTime(), new Date());
        if (signTask == null) {
            return false;
        }
        updateInstanceStatus(instance.getInstanceId(), INSTANCE_STATUS_RUNNING, task.getNodeName(), ACTION_ADD_SIGN, actionUserId, actionUserName, new Date());
        recordTaskAction(instance, task, ACTION_ADD_SIGN, actionUserId, actionUserName, actionUserNick,
                task.getAssigneeUserId(), targetAssignee.userId, normalizeText(transferBody.getActionComment()));
        return true;
    }

    /**
     * 减签。
     *
     * @param taskId         任务ID
     * @param transferBody   减签参数
     * @param actionUserId   操作人用户ID
     * @param actionUserName 操作人账号
     * @param actionUserNick 操作人昵称
     * @return 减签结果
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean removeSign(Long taskId, WorkflowTaskTransferBody transferBody, Long actionUserId, String actionUserName, String actionUserNick) {
        SysWorkflowTask currentTask = workflowTaskMapper.selectById(taskId);
        if (!canHandleTask(currentTask, actionUserId) || transferBody == null || transferBody.getTargetUserId() == null) {
            return false;
        }
        if (Objects.equals(currentTask.getAssigneeUserId(), transferBody.getTargetUserId())) {
            return false;
        }
        SysWorkflowTask targetTask = workflowTaskMapper.selectOne(new LambdaQueryWrapper<SysWorkflowTask>()
                .eq(SysWorkflowTask::getInstanceId, currentTask.getInstanceId())
                .eq(SysWorkflowTask::getNodeKey, currentTask.getNodeKey())
                .eq(SysWorkflowTask::getAssigneeUserId, transferBody.getTargetUserId())
                .in(SysWorkflowTask::getStatus, TASK_STATUS_PENDING, TASK_STATUS_PROCESSING)
                .last("LIMIT 1"));
        if (targetTask == null) {
            return false;
        }
        SysWorkflowTask cancelTask = new SysWorkflowTask();
        cancelTask.setTaskId(targetTask.getTaskId());
        cancelTask.setStatus(TASK_STATUS_CANCELED);
        cancelTask.setActionComment(normalizeText(transferBody.getActionComment()));
        cancelTask.setFinishTime(new Date());
        workflowTaskMapper.updateById(cancelTask);
        finishTodoTask(targetTask.getTodoId(), actionUserId, "减签");

        SysWorkflowInstance instance = workflowInstanceMapper.selectById(currentTask.getInstanceId());
        if (instance == null) {
            return false;
        }
        updateInstanceStatus(instance.getInstanceId(), INSTANCE_STATUS_RUNNING, currentTask.getNodeName(), ACTION_REMOVE_SIGN, actionUserId, actionUserName, new Date());
        recordTaskAction(instance, currentTask, ACTION_REMOVE_SIGN, actionUserId, actionUserName, actionUserNick,
                targetTask.getAssigneeUserId(), null, normalizeText(transferBody.getActionComment()));
        return true;
    }

    /**
     * 委派流程任务。
     *
     * @param taskId         任务ID
     * @param transferBody   委派参数
     * @param actionUserId   操作人用户ID
     * @param actionUserName 操作人账号
     * @param actionUserNick 操作人昵称
     * @return 委派结果
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean delegateTask(Long taskId, WorkflowTaskTransferBody transferBody, Long actionUserId, String actionUserName, String actionUserNick) {
        return transferTask(taskId, transferBody, actionUserId, actionUserName, actionUserNick)
                && recordDelegateAction(taskId, transferBody, actionUserId, actionUserName, actionUserNick);
    }

    /**
     * 催办流程任务。
     *
     * @param taskId         任务ID
     * @param remindBody     催办参数
     * @param actionUserId   操作人用户ID
     * @param actionUserName 操作人账号
     * @param actionUserNick 操作人昵称
     * @return 催办结果
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean remindTask(Long taskId, WorkflowTaskRemindBody remindBody, Long actionUserId, String actionUserName, String actionUserNick) {
        SysWorkflowTask task = workflowTaskMapper.selectById(taskId);
        if (task == null) {
            return false;
        }
        SysWorkflowInstance instance = workflowInstanceMapper.selectById(task.getInstanceId());
        if (instance == null) {
            return false;
        }
        boolean canRemind = Objects.equals(actionUserId, task.getAssigneeUserId()) || Objects.equals(actionUserId, instance.getInitiatorUserId());
        if (!canRemind) {
            return false;
        }
        Set<String> channels = normalizeChannels(remindBody == null ? null : remindBody.getChannels());
        String message = normalizeText(remindBody == null ? null : remindBody.getMessage());
        if (!StringUtils.hasText(message)) {
            message = "流程【" + instance.getProcessName() + "】有待处理任务，请尽快办理。";
        }
        pushNoticeToUser(instance.getTenantId(), task.getAssigneeUserId(),
                "催办通知：" + instance.getProcessName(), "审批通知", "流程引擎", instance.getBusinessNo(), message, channels);
        recordTaskAction(instance, task, ACTION_REMIND, actionUserId, actionUserName, actionUserNick, null, task.getAssigneeUserId(), message);
        return true;
    }

    /**
     * 查询任务节点动态表单。
     *
     * @param taskId 任务ID
     * @param userId 当前用户ID
     * @return 动态表单
     */
    @Override
    public WorkflowTaskFormVO selectTaskForm(Long taskId, Long userId) {
        SysWorkflowTask task = workflowTaskMapper.selectById(taskId);
        if (task == null) {
            return null;
        }
        SysWorkflowInstance instance = workflowInstanceMapper.selectById(task.getInstanceId());
        if (instance == null) {
            return null;
        }
        if (!Objects.equals(userId, task.getAssigneeUserId()) && !Objects.equals(userId, instance.getInitiatorUserId())) {
            return null;
        }
        FormSchema schema = parseFormSchema(instance.getFormSchemaSnapshot());
        Map<String, Object> valueMap = parseJsonMap(instance.getFormData());

        WorkflowTaskFormVO formVO = new WorkflowTaskFormVO();
        formVO.setTaskId(task.getTaskId());
        formVO.setInstanceId(task.getInstanceId());
        formVO.setDefinitionId(task.getDefinitionId());
        formVO.setDefinitionVersion(instance.getDefinitionVersion());
        formVO.setNodeKey(task.getNodeKey());
        formVO.setNodeName(task.getNodeName());
        formVO.setFormVersion(schema.version);
        for (SchemaField field : schema.fields) {
            FieldPermission permission = schema.resolvePermission(task.getNodeKey(), task.getNodeName(), field.fieldCode);
            if ("hidden".equals(permission.permission)) {
                continue;
            }
            WorkflowTaskFormFieldVO fieldVO = new WorkflowTaskFormFieldVO();
            fieldVO.setFieldCode(field.fieldCode);
            fieldVO.setFieldLabel(field.fieldLabel);
            fieldVO.setComponentType(field.componentType);
            fieldVO.setPermission(permission.permission);
            fieldVO.setRequired(permission.required == null ? field.required : permission.required);
            fieldVO.setPlaceholder(field.placeholder);
            fieldVO.setOptions(field.options);
            fieldVO.setValue(valueMap.get(field.fieldCode));
            formVO.getFields().add(fieldVO);
        }
        return formVO;
    }

    /**
     * 执行流程任务 SLA 扫描。
     *
     * @return 扫描结果
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public WorkflowSlaScanResultVO scanTimeoutTasks() {
        WorkflowSlaScanResultVO resultVO = new WorkflowSlaScanResultVO();
        resultVO.setScanTime(new Date());
        List<SysWorkflowTask> pendingTaskList = workflowTaskMapper.selectList(new LambdaQueryWrapper<SysWorkflowTask>()
                .in(SysWorkflowTask::getStatus, TASK_STATUS_PENDING, TASK_STATUS_PROCESSING)
                .isNotNull(SysWorkflowTask::getDueTime)
                .orderByAsc(SysWorkflowTask::getDueTime)
                .orderByAsc(SysWorkflowTask::getTaskId));
        resultVO.setScannedCount(pendingTaskList.size());

        Date now = new Date();
        for (SysWorkflowTask task : pendingTaskList) {
            if (task == null || task.getDueTime() == null) {
                resultVO.setSkippedCount(resultVO.getSkippedCount() + 1);
                continue;
            }
            SysWorkflowInstance instance = workflowInstanceMapper.selectById(task.getInstanceId());
            if (instance == null) {
                resultVO.setSkippedCount(resultVO.getSkippedCount() + 1);
                continue;
            }
            WorkflowModel model = parseWorkflowModel(instance.getModelContentSnapshot());
            WorkflowSlaConfig slaConfig = resolveEffectiveSlaConfig(model, task.getNodeKey());
            if (slaConfig == null || !slaConfig.enabled) {
                resultVO.setSkippedCount(resultVO.getSkippedCount() + 1);
                continue;
            }
            if (shouldSendTimeoutWarning(task, slaConfig, now) && !hasTaskAction(task.getTaskId(), ACTION_TIMEOUT_REMIND)) {
                sendTimeoutWarning(task, instance, slaConfig);
                resultVO.setWarningCount(resultVO.getWarningCount() + 1);
                resultVO.setRemindedCount(resultVO.getRemindedCount() + 1);
            }
            if (!now.after(task.getDueTime())) {
                continue;
            }
            resultVO.setOverdueCount(resultVO.getOverdueCount() + 1);
            boolean handoverCompleted = false;
            if (slaConfig.actionSet.contains("ESCALATE") && !hasTaskAction(task.getTaskId(), ACTION_TIMEOUT_ESCALATE)) {
                if (escalateTimeoutTask(task, instance, slaConfig, now)) {
                    resultVO.setEscalatedCount(resultVO.getEscalatedCount() + 1);
                    handoverCompleted = true;
                } else {
                    resultVO.setSkippedCount(resultVO.getSkippedCount() + 1);
                }
            }
            if (handoverCompleted && slaConfig.actionSet.contains("TRANSFER")) {
                resultVO.setSkippedCount(resultVO.getSkippedCount() + 1);
                continue;
            }
            if (slaConfig.actionSet.contains("TRANSFER") && !hasTaskAction(task.getTaskId(), ACTION_TIMEOUT_TRANSFER)) {
                if (autoTransferTimeoutTask(task, instance, slaConfig, now)) {
                    resultVO.setTransferredCount(resultVO.getTransferredCount() + 1);
                } else {
                    resultVO.setSkippedCount(resultVO.getSkippedCount() + 1);
                }
            }
        }
        return resultVO;
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
     * 判断当前任务是否需要发送超时提醒。
     *
     * @param task      流程任务
     * @param slaConfig SLA 配置
     * @param now       当前时间
     * @return true 表示需要提醒
     */
    private boolean shouldSendTimeoutWarning(SysWorkflowTask task, WorkflowSlaConfig slaConfig, Date now) {
        if (task == null || task.getDueTime() == null || slaConfig == null || !slaConfig.enabled) {
            return false;
        }
        long remindMillis = Math.max(0, slaConfig.reminderBeforeMinutes) * 60000L;
        long warningTime = task.getDueTime().getTime() - remindMillis;
        return now.getTime() >= warningTime;
    }

    /**
     * 发送节点超时提醒。
     *
     * @param task      流程任务
     * @param instance  流程实例
     * @param slaConfig SLA 配置
     */
    private void sendTimeoutWarning(SysWorkflowTask task, SysWorkflowInstance instance, WorkflowSlaConfig slaConfig) {
        if (task == null || instance == null || task.getAssigneeUserId() == null) {
            return;
        }
        String message = "流程【" + instance.getProcessName() + "】节点【" + task.getNodeName() + "】即将或已经超时，请及时处理。";
        pushNoticeToUser(instance.getTenantId(),
                task.getAssigneeUserId(),
                "超时提醒：" + instance.getProcessName(),
                "预警提醒",
                "流程引擎",
                instance.getBusinessNo(),
                message,
                normalizeChannels(slaConfig.channels));
        recordTaskAction(instance, task, ACTION_TIMEOUT_REMIND, SYSTEM_ACTION_USER_ID, SYSTEM_ACTION_USER_NAME,
                SYSTEM_ACTION_USER_NICK, null, task.getAssigneeUserId(), message);
        updateInstanceStatus(instance.getInstanceId(), INSTANCE_STATUS_RUNNING, task.getNodeName(), ACTION_TIMEOUT_REMIND,
                SYSTEM_ACTION_USER_ID, SYSTEM_ACTION_USER_NAME, new Date());
    }

    /**
     * 执行超时自动升级。
     *
     * @param task      流程任务
     * @param instance  流程实例
     * @param slaConfig SLA 配置
     * @param now       当前时间
     * @return true 表示执行成功
     */
    private boolean escalateTimeoutTask(SysWorkflowTask task,
                                        SysWorkflowInstance instance,
                                        WorkflowSlaConfig slaConfig,
                                        Date now) {
        if (task == null || instance == null || slaConfig == null) {
            return false;
        }
        AssigneeInfo targetAssignee = resolveSlaTargetAssignee(slaConfig, true, instance);
        if (targetAssignee == null || targetAssignee.userId == null) {
            return false;
        }
        if (Objects.equals(task.getAssigneeUserId(), targetAssignee.userId)) {
            return false;
        }
        SysWorkflowTask closeTask = new SysWorkflowTask();
        closeTask.setTaskId(task.getTaskId());
        closeTask.setStatus(TASK_STATUS_CANCELED);
        closeTask.setActionComment("SLA 超时自动升级");
        closeTask.setClaimTime(task.getClaimTime() == null ? now : task.getClaimTime());
        closeTask.setFinishTime(now);
        if (workflowTaskMapper.updateById(closeTask) <= 0) {
            return false;
        }
        finishTodoTask(task.getTodoId(), SYSTEM_ACTION_USER_ID, "SLA 超时自动升级");

        Date nextDueTime = resolveTransferredTaskDueTime(slaConfig, task.getDueTime(), now);
        SysTodoTask newTodoTask = new SysTodoTask();
        newTodoTask.setInstanceId(task.getInstanceId());
        newTodoTask.setTenantId(instance.getTenantId());
        newTodoTask.setProcessName(instance.getProcessName());
        newTodoTask.setNodeName(task.getNodeName());
        newTodoTask.setBusinessNo(instance.getBusinessNo());
        newTodoTask.setPriority("H");
        newTodoTask.setStatus(TODO_STATUS_PENDING);
        newTodoTask.setAssigneeUserId(targetAssignee.userId);
        newTodoTask.setDueTime(nextDueTime);
        newTodoTask.setCreateTime(now);
        newTodoTask.setRemark("系统按 SLA 自动升级");
        if (!todoTaskService.save(newTodoTask)) {
            return false;
        }

        SysWorkflowTask escalateTask = new SysWorkflowTask();
        escalateTask.setTenantId(task.getTenantId());
        escalateTask.setInstanceId(task.getInstanceId());
        escalateTask.setDefinitionId(task.getDefinitionId());
        escalateTask.setNodeKey(task.getNodeKey());
        escalateTask.setNodeName(task.getNodeName());
        escalateTask.setCandidateUserIds(String.valueOf(targetAssignee.userId));
        escalateTask.setAssigneeUserId(targetAssignee.userId);
        escalateTask.setAssigneeUserName(targetAssignee.userName);
        escalateTask.setAssigneeNickName(targetAssignee.nickName);
        escalateTask.setStatus(TASK_STATUS_PENDING);
        escalateTask.setTodoId(newTodoTask.getTodoId());
        escalateTask.setDueTime(nextDueTime);
        escalateTask.setCreateTime(now);
        if (workflowTaskMapper.insert(escalateTask) <= 0) {
            return false;
        }
        bindTodoWorkflowLink(newTodoTask, escalateTask);
        String message = "流程【" + instance.getProcessName() + "】节点【" + task.getNodeName() + "】已超时，已自动升级到您跟进。";
        pushNoticeToUser(instance.getTenantId(),
                targetAssignee.userId,
                "超时升级：" + instance.getProcessName(),
                "预警提醒",
                "流程引擎",
                instance.getBusinessNo(),
                message,
                normalizeChannels(slaConfig.channels));
        recordTaskAction(instance, task, ACTION_TIMEOUT_ESCALATE, SYSTEM_ACTION_USER_ID, SYSTEM_ACTION_USER_NAME,
                SYSTEM_ACTION_USER_NICK, task.getAssigneeUserId(), targetAssignee.userId, message);
        updateInstanceStatus(instance.getInstanceId(), INSTANCE_STATUS_RUNNING, task.getNodeName(), ACTION_TIMEOUT_ESCALATE,
                SYSTEM_ACTION_USER_ID, SYSTEM_ACTION_USER_NAME, new Date());
        return true;
    }

    /**
     * 执行超时自动转办。
     *
     * @param task      流程任务
     * @param instance  流程实例
     * @param slaConfig SLA 配置
     * @param now       当前时间
     * @return true 表示执行成功
     */
    private boolean autoTransferTimeoutTask(SysWorkflowTask task,
                                            SysWorkflowInstance instance,
                                            WorkflowSlaConfig slaConfig,
                                            Date now) {
        if (task == null || instance == null || slaConfig == null) {
            return false;
        }
        AssigneeInfo targetAssignee = resolveSlaTargetAssignee(slaConfig, false, instance);
        if (targetAssignee == null || targetAssignee.userId == null) {
            return false;
        }
        if (Objects.equals(task.getAssigneeUserId(), targetAssignee.userId)) {
            return false;
        }

        SysWorkflowTask updateTask = new SysWorkflowTask();
        updateTask.setTaskId(task.getTaskId());
        updateTask.setStatus(TASK_STATUS_TRANSFERRED);
        updateTask.setActionComment("SLA 超时自动转办");
        updateTask.setClaimTime(task.getClaimTime() == null ? now : task.getClaimTime());
        updateTask.setFinishTime(now);
        if (workflowTaskMapper.updateById(updateTask) <= 0) {
            return false;
        }
        finishTodoTask(task.getTodoId(), SYSTEM_ACTION_USER_ID, "SLA 超时自动转办");

        Date nextDueTime = resolveTransferredTaskDueTime(slaConfig, task.getDueTime(), now);
        SysTodoTask newTodoTask = new SysTodoTask();
        newTodoTask.setInstanceId(task.getInstanceId());
        newTodoTask.setTenantId(instance.getTenantId());
        newTodoTask.setProcessName(instance.getProcessName());
        newTodoTask.setNodeName(task.getNodeName());
        newTodoTask.setBusinessNo(instance.getBusinessNo());
        newTodoTask.setPriority("H");
        newTodoTask.setStatus(TODO_STATUS_PENDING);
        newTodoTask.setAssigneeUserId(targetAssignee.userId);
        newTodoTask.setDueTime(nextDueTime);
        newTodoTask.setCreateTime(now);
        newTodoTask.setRemark("系统按 SLA 自动转办");
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
        newTask.setDueTime(nextDueTime);
        newTask.setCreateTime(now);
        if (workflowTaskMapper.insert(newTask) <= 0) {
            return false;
        }
        bindTodoWorkflowLink(newTodoTask, newTask);
        String message = "流程【" + instance.getProcessName() + "】节点【" + task.getNodeName() + "】因超时已自动转办给您。";
        recordTaskAction(instance, task, ACTION_TIMEOUT_TRANSFER, SYSTEM_ACTION_USER_ID, SYSTEM_ACTION_USER_NAME,
                SYSTEM_ACTION_USER_NICK, task.getAssigneeUserId(), targetAssignee.userId, message);
        updateInstanceStatus(instance.getInstanceId(), INSTANCE_STATUS_RUNNING, task.getNodeName(), ACTION_TIMEOUT_TRANSFER,
                SYSTEM_ACTION_USER_ID, SYSTEM_ACTION_USER_NAME, now);
        pushNoticeToUser(instance.getTenantId(),
                targetAssignee.userId,
                "超时转办：" + instance.getProcessName(),
                "预警提醒",
                "流程引擎",
                instance.getBusinessNo(),
                message,
                normalizeChannels(slaConfig.channels));
        return true;
    }

    /**
     * 计算转办后新的截止时间。
     *
     * @param slaConfig    SLA 配置
     * @param previousDue  原截止时间
     * @param now          当前时间
     * @return 新截止时间
     */
    private Date resolveTransferredTaskDueTime(WorkflowSlaConfig slaConfig, Date previousDue, Date now) {
        if (slaConfig != null && slaConfig.durationMinutes > 0) {
            return new Date(now.getTime() + slaConfig.durationMinutes * 60000L);
        }
        return previousDue;
    }

    /**
     * 判断任务是否已经存在指定动作记录。
     *
     * @param taskId      任务ID
     * @param actionType  动作类型
     * @return true 表示已存在
     */
    private boolean hasTaskAction(Long taskId, String actionType) {
        if (taskId == null || !StringUtils.hasText(actionType)) {
            return false;
        }
        return workflowTaskActionMapper.selectCount(new LambdaQueryWrapper<SysWorkflowTaskAction>()
                .eq(SysWorkflowTaskAction::getTaskId, taskId)
                .eq(SysWorkflowTaskAction::getActionType, actionType.trim())) > 0;
    }

    /**
     * 解析节点生效的 SLA 配置。
     *
     * @param model   流程模型
     * @param nodeKey 节点编码
     * @return 生效配置
     */
    private WorkflowSlaConfig resolveEffectiveSlaConfig(WorkflowModel model, String nodeKey) {
        if (model == null) {
            return null;
        }
        WorkflowSlaConfig globalConfig = model.slaConfig == null ? new WorkflowSlaConfig() : model.slaConfig;
        WorkflowNode node = model.nodeMap.get(nodeKey);
        if (node == null || node.slaConfig == null) {
            return globalConfig;
        }
        return mergeSlaConfig(globalConfig, node.slaConfig);
    }

    /**
     * 合并流程级与节点级 SLA 配置。
     *
     * @param globalConfig 流程级配置
     * @param nodeConfig   节点级配置
     * @return 合并结果
     */
    private WorkflowSlaConfig mergeSlaConfig(WorkflowSlaConfig globalConfig, WorkflowSlaConfig nodeConfig) {
        WorkflowSlaConfig mergedConfig = new WorkflowSlaConfig();
        mergedConfig.enabled = nodeConfig.enabled || globalConfig.enabled;
        mergedConfig.durationMinutes = nodeConfig.durationMinutes > 0 ? nodeConfig.durationMinutes : globalConfig.durationMinutes;
        mergedConfig.reminderBeforeMinutes = nodeConfig.reminderBeforeMinutes > 0
                ? nodeConfig.reminderBeforeMinutes : globalConfig.reminderBeforeMinutes;
        mergedConfig.escalateTargetType = StringUtils.hasText(nodeConfig.escalateTargetType)
                ? nodeConfig.escalateTargetType : globalConfig.escalateTargetType;
        mergedConfig.escalateToUserId = nodeConfig.escalateToUserId != null ? nodeConfig.escalateToUserId : globalConfig.escalateToUserId;
        mergedConfig.escalateDeptId = nodeConfig.escalateDeptId != null ? nodeConfig.escalateDeptId : globalConfig.escalateDeptId;
        mergedConfig.escalateRoleIds.clear();
        mergedConfig.escalateRoleIds.addAll(nodeConfig.escalateRoleIds.isEmpty() ? globalConfig.escalateRoleIds : nodeConfig.escalateRoleIds);
        mergedConfig.escalatePostIds.clear();
        mergedConfig.escalatePostIds.addAll(nodeConfig.escalatePostIds.isEmpty() ? globalConfig.escalatePostIds : nodeConfig.escalatePostIds);
        mergedConfig.transferTargetType = StringUtils.hasText(nodeConfig.transferTargetType)
                ? nodeConfig.transferTargetType : globalConfig.transferTargetType;
        mergedConfig.transferToUserId = nodeConfig.transferToUserId != null ? nodeConfig.transferToUserId : globalConfig.transferToUserId;
        mergedConfig.transferDeptId = nodeConfig.transferDeptId != null ? nodeConfig.transferDeptId : globalConfig.transferDeptId;
        mergedConfig.transferRoleIds.clear();
        mergedConfig.transferRoleIds.addAll(nodeConfig.transferRoleIds.isEmpty() ? globalConfig.transferRoleIds : nodeConfig.transferRoleIds);
        mergedConfig.transferPostIds.clear();
        mergedConfig.transferPostIds.addAll(nodeConfig.transferPostIds.isEmpty() ? globalConfig.transferPostIds : nodeConfig.transferPostIds);
        mergedConfig.channels.clear();
        mergedConfig.channels.addAll(nodeConfig.channels.isEmpty() ? globalConfig.channels : nodeConfig.channels);
        mergedConfig.actionSet.clear();
        mergedConfig.actionSet.addAll(nodeConfig.actionSet.isEmpty() ? globalConfig.actionSet : nodeConfig.actionSet);
        return mergedConfig;
    }

    /**
     * 构造动作上下文。
     *
     * @param userId   用户ID
     * @param userName 用户账号
     * @param userNick 用户昵称
     * @return 动作上下文
     */
    private ActionContext initiateActionContext(Long userId, String userName, String userNick) {
        return new ActionContext(userId, userName, userNick);
    }

    /**
     * 保存合并后的表单数据并执行节点必填校验。
     *
     * @param instance  实例对象
     * @param deltaData 增量表单JSON
     * @param nodeKey   当前节点编码
     * @param nodeName  当前节点名称
     * @return true 表示保存成功
     */
    private boolean saveMergedFormData(SysWorkflowInstance instance, String deltaData, String nodeKey, String nodeName) {
        if (instance == null) {
            return false;
        }
        Map<String, Object> mergedData = mergeFormData(instance.getFormData(), deltaData);
        FormSchema schema = parseFormSchema(instance.getFormSchemaSnapshot());
        for (SchemaField field : schema.fields) {
            FieldPermission permission = schema.resolvePermission(nodeKey, nodeName, field.fieldCode);
            if ("hidden".equals(permission.permission)) {
                continue;
            }
            boolean required = permission.required == null ? field.required : permission.required;
            if (required && isBlankValue(mergedData.get(field.fieldCode))) {
                return false;
            }
        }
        SysWorkflowInstance updateEntity = new SysWorkflowInstance();
        updateEntity.setInstanceId(instance.getInstanceId());
        updateEntity.setFormData(writeJson(mergedData));
        return workflowInstanceMapper.updateById(updateEntity) > 0;
    }

    /**
     * 合并实例表单与增量表单。
     *
     * @param originJson 实例已有表单
     * @param deltaJson  增量表单
     * @return 合并后的表单
     */
    private Map<String, Object> mergeFormData(String originJson, String deltaJson) {
        Map<String, Object> merged = new LinkedHashMap<>(parseJsonMap(originJson));
        merged.putAll(parseJsonMap(deltaJson));
        return merged;
    }

    /**
     * 计算节点任务截止时间。
     *
     * @param model           流程模型
     * @param nodeKey         节点编码
     * @param fallbackDueTime 兜底截止时间
     * @param createTime      创建时间
     * @return 节点截止时间
     */
    private Date resolveTaskDueTime(WorkflowModel model, String nodeKey, Date fallbackDueTime, Date createTime) {
        WorkflowSlaConfig slaConfig = resolveEffectiveSlaConfig(model, nodeKey);
        if (slaConfig != null && slaConfig.enabled && slaConfig.durationMinutes > 0 && createTime != null) {
            return new Date(createTime.getTime() + slaConfig.durationMinutes * 60000L);
        }
        return fallbackDueTime;
    }

    /**
     * 创建任务及待办。
     *
     * @param instance     流程实例
     * @param nodeKey      节点编码
     * @param nodeName     节点名称
     * @param assigneeInfo 办理人
     * @param dueTime      截止时间
     * @param createTime   创建时间
     * @return 任务对象
     */
    private SysWorkflowTask createTaskForAssignee(SysWorkflowInstance instance,
                                                  String nodeKey,
                                                  String nodeName,
                                                  AssigneeInfo assigneeInfo,
                                                  Date dueTime,
                                                  Date createTime) {
        if (instance == null || assigneeInfo == null || assigneeInfo.userId == null) {
            return null;
        }
        SysTodoTask todoTask = new SysTodoTask();
        todoTask.setInstanceId(instance.getInstanceId());
        todoTask.setTenantId(instance.getTenantId());
        todoTask.setProcessName(instance.getProcessName());
        todoTask.setNodeName(nodeName);
        todoTask.setBusinessNo(instance.getBusinessNo());
        todoTask.setPriority("M");
        todoTask.setStatus(TODO_STATUS_PENDING);
        todoTask.setAssigneeUserId(assigneeInfo.userId);
        todoTask.setDueTime(dueTime);
        todoTask.setCreateTime(createTime);
        todoTask.setRemark("流程任务自动创建");
        if (!todoTaskService.save(todoTask)) {
            return null;
        }
        SysWorkflowTask workflowTask = new SysWorkflowTask();
        workflowTask.setTenantId(instance.getTenantId());
        workflowTask.setInstanceId(instance.getInstanceId());
        workflowTask.setDefinitionId(instance.getDefinitionId());
        workflowTask.setNodeKey(nodeKey);
        workflowTask.setNodeName(nodeName);
        workflowTask.setCandidateUserIds(String.valueOf(assigneeInfo.userId));
        workflowTask.setAssigneeUserId(assigneeInfo.userId);
        workflowTask.setAssigneeUserName(assigneeInfo.userName);
        workflowTask.setAssigneeNickName(assigneeInfo.nickName);
        workflowTask.setStatus(TASK_STATUS_PENDING);
        workflowTask.setTodoId(todoTask.getTodoId());
        workflowTask.setDueTime(dueTime);
        workflowTask.setCreateTime(createTime);
        if (workflowTaskMapper.insert(workflowTask) <= 0) {
            return null;
        }
        bindTodoWorkflowLink(todoTask, workflowTask);
        pushNoticeToUser(instance.getTenantId(), assigneeInfo.userId,
                "收到待审批任务：" + instance.getProcessName(),
                "审批通知",
                "流程引擎",
                instance.getBusinessNo(),
                "流程【" + instance.getProcessName() + "】有新的审批任务，请及时处理。");
        return workflowTask;
    }

    /**
     * 记录委派动作日志。
     *
     * @param taskId         原任务ID
     * @param transferBody   委派参数
     * @param actionUserId   操作人ID
     * @param actionUserName 操作人账号
     * @param actionUserNick 操作人昵称
     * @return true 表示记录成功
     */
    private boolean recordDelegateAction(Long taskId, WorkflowTaskTransferBody transferBody, Long actionUserId, String actionUserName, String actionUserNick) {
        SysWorkflowTask sourceTask = workflowTaskMapper.selectById(taskId);
        if (sourceTask == null) {
            return false;
        }
        SysWorkflowInstance instance = workflowInstanceMapper.selectById(sourceTask.getInstanceId());
        if (instance == null) {
            return false;
        }
        recordTaskAction(instance, sourceTask, ACTION_DELEGATE, actionUserId, actionUserName, actionUserNick,
                actionUserId, transferBody.getTargetUserId(), normalizeText(transferBody.getActionComment()));
        updateInstanceStatus(instance.getInstanceId(), INSTANCE_STATUS_RUNNING, sourceTask.getNodeName(), ACTION_DELEGATE, actionUserId, actionUserName, new Date());
        return true;
    }

    /**
     * 按流程模型激活后续节点任务。
     *
     * @param model           流程模型
     * @param instance        流程实例
     * @param fromNodeKey     起始节点编码
     * @param formData        表单数据
     * @param dueTime         截止时间
     * @param fallbackAssignee 兜底审批人
     * @param actionContext   动作上下文
     * @return 路由结果
     */
    private RouteResult activateByModel(WorkflowModel model,
                                        SysWorkflowInstance instance,
                                        String fromNodeKey,
                                        Map<String, Object> formData,
                                        Date dueTime,
                                        AssigneeInfo fallbackAssignee,
                                        ActionContext actionContext) {
        RouteResult result = new RouteResult();
        if (model == null || instance == null || !StringUtils.hasText(fromNodeKey)) {
            return result;
        }
        ArrayList<String> queue = new ArrayList<>(model.resolveNextNodeKeys(fromNodeKey, formData));
        Set<String> visitedPassNode = new LinkedHashSet<>();
        int index = 0;
        while (index < queue.size()) {
            String nodeKey = queue.get(index++);
            WorkflowNode node = model.nodeMap.get(nodeKey);
            if (node == null) {
                continue;
            }
            String nodeType = StringUtils.hasText(node.nodeType) ? node.nodeType : "approval";
            if ("end".equals(nodeType)) {
                continue;
            }
            if ("start".equals(nodeType) || "gateway".equals(nodeType) || "parallel".equals(nodeType)) {
                if ("parallel".equals(nodeType) && !canPassParallelNode(model, instance, nodeKey)) {
                    continue;
                }
                if (visitedPassNode.add(nodeKey)) {
                    queue.addAll(model.resolveNextNodeKeys(nodeKey, formData));
                }
                continue;
            }
            if ("cc".equals(nodeType)) {
                for (Long ccUserId : node.ccUserIds) {
                    pushNoticeToUser(instance.getTenantId(),
                            ccUserId,
                            "流程抄送：" + instance.getProcessName(),
                            "抄送通知",
                            "流程引擎",
                            instance.getBusinessNo(),
                            "流程【" + instance.getProcessName() + "】已抄送到您。");
                }
                recordTaskAction(instance, null, ACTION_CC, actionContext.userId, actionContext.userName, actionContext.userNick, null, null, "流程抄送");
                if (visitedPassNode.add(nodeKey)) {
                    queue.addAll(model.resolveNextNodeKeys(nodeKey, formData));
                }
                continue;
            }
            List<AssigneeInfo> assigneeList = resolveNodeAssignees(instance, node, fallbackAssignee, actionContext);
            for (AssigneeInfo assigneeInfo : assigneeList) {
                Date createTime = new Date();
                Date nodeDueTime = resolveTaskDueTime(model, node.nodeKey, dueTime, createTime);
                SysWorkflowTask nextTask = createTaskForAssignee(instance, node.nodeKey, node.nodeName, assigneeInfo, nodeDueTime, createTime);
                if (nextTask != null) {
                    result.createdTasks.add(nextTask);
                    result.activeNodeNames.add(node.nodeName);
                }
            }
        }
        return result;
    }

    /**
     * 判断当前审批动作后是否允许继续流转。
     * ALL 会签需等待同节点任务全部完成，ANY 会签会自动结束其他同节点任务。
     *
     * @param model          流程模型
     * @param currentTask    当前任务
     * @param actionUserId   操作人用户ID
     * @param actionUserName 操作人账号
     * @param actionTime     操作时间
     * @return true 表示允许继续流转
     */
    private boolean canAdvanceAfterApprove(WorkflowModel model,
                                           SysWorkflowTask currentTask,
                                           Long actionUserId,
                                           String actionUserName,
                                           Date actionTime) {
        if (model == null || currentTask == null || !StringUtils.hasText(currentTask.getNodeKey())) {
            return true;
        }
        List<SysWorkflowTask> siblingTaskList = workflowTaskMapper.selectList(new LambdaQueryWrapper<SysWorkflowTask>()
                .eq(SysWorkflowTask::getInstanceId, currentTask.getInstanceId())
                .eq(SysWorkflowTask::getNodeKey, currentTask.getNodeKey())
                .in(SysWorkflowTask::getStatus, TASK_STATUS_PENDING, TASK_STATUS_PROCESSING));
        if (siblingTaskList == null || siblingTaskList.isEmpty()) {
            return true;
        }
        if (model.isAnyApprove(currentTask.getNodeKey())) {
            for (SysWorkflowTask siblingTask : siblingTaskList) {
                if (Objects.equals(siblingTask.getTaskId(), currentTask.getTaskId())) {
                    continue;
                }
                SysWorkflowTask cancelTask = new SysWorkflowTask();
                cancelTask.setTaskId(siblingTask.getTaskId());
                cancelTask.setStatus(TASK_STATUS_CANCELED);
                cancelTask.setActionComment(COUNTERSIGN_AUTO_FINISH_COMMENT);
                cancelTask.setFinishTime(actionTime);
                workflowTaskMapper.updateById(cancelTask);
                finishTodoTask(siblingTask.getTodoId(), actionUserId, COUNTERSIGN_AUTO_FINISH_COMMENT);
            }
            return true;
        }
        return false;
    }

    /**
     * 判断并行网关是否满足汇聚条件。
     * 若存在多个前驱节点，则需等待所有前驱节点无在途任务后才能继续向后流转。
     *
     * @param model      流程模型
     * @param instance   流程实例
     * @param parallelKey 并行节点编码
     * @return true 表示可以继续流转
     */
    private boolean canPassParallelNode(WorkflowModel model, SysWorkflowInstance instance, String parallelKey) {
        if (model == null || instance == null || !StringUtils.hasText(parallelKey)) {
            return false;
        }
        List<String> previousNodeKeys = model.resolvePreviousNodeKeys(parallelKey);
        if (previousNodeKeys.size() <= 1) {
            return true;
        }
        for (String previousNodeKey : previousNodeKeys) {
            WorkflowNode previousNode = model.nodeMap.get(previousNodeKey);
            if (previousNode == null) {
                continue;
            }
            if ("start".equals(previousNode.nodeType) || "gateway".equals(previousNode.nodeType)
                    || "parallel".equals(previousNode.nodeType) || "cc".equals(previousNode.nodeType)
                    || "end".equals(previousNode.nodeType)) {
                continue;
            }
            if (countPendingTaskByNode(instance.getInstanceId(), previousNodeKey) > 0) {
                return false;
            }
        }
        return true;
    }

    /**
     * 解析节点审批人集合。
     *
     * @param instance         流程实例
     * @param node             节点定义
     * @param fallbackAssignee 兜底审批人
     * @param actionContext    动作上下文
     * @return 审批人集合
     */
    private List<AssigneeInfo> resolveNodeAssignees(SysWorkflowInstance instance, WorkflowNode node, AssigneeInfo fallbackAssignee, ActionContext actionContext) {
        if (node == null) {
            return Collections.emptyList();
        }
        return resolveParticipantAssigneeList(node.assigneeType,
                node.assigneeUserId,
                node.candidateUserIds,
                node.assigneeRoleIds,
                node.assigneePostIds,
                node.assigneeDeptId,
                instance,
                fallbackAssignee,
                actionContext);
    }

    /**
     * 统计实例待处理任务数量。
     *
     * @param instanceId 实例ID
     * @return 待处理任务数量
     */
    private long countPendingTask(Long instanceId) {
        Long count = workflowTaskMapper.selectCount(new LambdaQueryWrapper<SysWorkflowTask>()
                .eq(SysWorkflowTask::getInstanceId, instanceId)
                .in(SysWorkflowTask::getStatus, TASK_STATUS_PENDING, TASK_STATUS_PROCESSING));
        return count == null ? 0L : count;
    }

    /**
     * 统计实例指定节点的待处理任务数量。
     *
     * @param instanceId 实例ID
     * @param nodeKey    节点编码
     * @return 待处理任务数量
     */
    private long countPendingTaskByNode(Long instanceId, String nodeKey) {
        if (instanceId == null || !StringUtils.hasText(nodeKey)) {
            return 0L;
        }
        Long count = workflowTaskMapper.selectCount(new LambdaQueryWrapper<SysWorkflowTask>()
                .eq(SysWorkflowTask::getInstanceId, instanceId)
                .eq(SysWorkflowTask::getNodeKey, nodeKey.trim())
                .in(SysWorkflowTask::getStatus, TASK_STATUS_PENDING, TASK_STATUS_PROCESSING));
        return count == null ? 0L : count;
    }

    /**
     * 拼接节点名称列表。
     *
     * @param nodeNames 节点名称集合
     * @return 拼接文本
     */
    private String joinNodeNames(Set<String> nodeNames) {
        if (nodeNames == null || nodeNames.isEmpty()) {
            return null;
        }
        StringBuilder builder = new StringBuilder();
        for (String nodeName : nodeNames) {
            if (!StringUtils.hasText(nodeName)) {
                continue;
            }
            if (builder.length() > 0) {
                builder.append(" | ");
            }
            builder.append(nodeName.trim());
        }
        return builder.length() == 0 ? null : builder.toString();
    }

    /**
     * 解析当前实例运行节点文本。
     *
     * @param instanceId   实例ID
     * @param defaultValue 默认文本
     * @return 节点文本
     */
    private String resolveRunningNodeName(Long instanceId, String defaultValue) {
        List<SysWorkflowTask> pendingTaskList = workflowTaskMapper.selectList(new LambdaQueryWrapper<SysWorkflowTask>()
                .eq(SysWorkflowTask::getInstanceId, instanceId)
                .in(SysWorkflowTask::getStatus, TASK_STATUS_PENDING, TASK_STATUS_PROCESSING));
        LinkedHashSet<String> nodeNames = new LinkedHashSet<>();
        for (SysWorkflowTask pendingTask : pendingTaskList) {
            if (StringUtils.hasText(pendingTask.getNodeName())) {
                nodeNames.add(pendingTask.getNodeName().trim());
            }
        }
        if (nodeNames.isEmpty()) {
            return defaultValue;
        }
        return joinNodeNames(nodeNames);
    }

    /**
     * 回填待办与流程任务的双向关联字段。
     *
     * @param todoTask     待办任务
     * @param workflowTask 流程任务
     */
    private void bindTodoWorkflowLink(SysTodoTask todoTask, SysWorkflowTask workflowTask) {
        if (todoTask == null || todoTask.getTodoId() == null || workflowTask == null || workflowTask.getTaskId() == null) {
            return;
        }
        SysTodoTask updateTodo = new SysTodoTask();
        updateTodo.setTodoId(todoTask.getTodoId());
        updateTodo.setInstanceId(workflowTask.getInstanceId());
        updateTodo.setTaskId(workflowTask.getTaskId());
        todoTaskService.updateById(updateTodo);
        todoTask.setInstanceId(workflowTask.getInstanceId());
        todoTask.setTaskId(workflowTask.getTaskId());
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
        if (INSTANCE_STATUS_COMPLETED.equals(status) || INSTANCE_STATUS_REJECTED.equals(status) || INSTANCE_STATUS_WITHDRAWN.equals(status)) {
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
    private boolean recordTaskAction(SysWorkflowInstance instance, SysWorkflowTask task, String actionType,
                                     Long actionUserId, String actionUserName, String actionUserNick,
                                     Long fromAssigneeUserId, Long toAssigneeUserId, String comment) {
        if (instance == null) {
            return false;
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
        return workflowTaskActionMapper.insert(action) > 0;
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
        pushNoticeToUser(tenantId, receiverUserId, title, noticeType, source, businessNo, content, Collections.singleton("IN_APP"));
    }

    /**
     * 按渠道发送流程消息通知到指定用户。
     *
     * @param tenantId       租户编号
     * @param receiverUserId 接收人用户ID
     * @param title          消息标题
     * @param noticeType     消息类型
     * @param source         消息来源
     * @param businessNo     业务单号
     * @param content        消息内容
     * @param channels       通知渠道
     */
    private void pushNoticeToUser(String tenantId, Long receiverUserId, String title, String noticeType,
                                  String source, String businessNo, String content, Set<String> channels) {
        if (!StringUtils.hasText(tenantId) || receiverUserId == null || !StringUtils.hasText(title)) {
            return;
        }
        Set<String> channelSet = channels == null || channels.isEmpty() ? Collections.singleton("IN_APP") : channels;
        for (String channel : channelSet) {
            SysNotice notice = new SysNotice();
            notice.setTenantId(tenantId);
            notice.setTitle(title.trim());
            notice.setNoticeType(noticeType);
            notice.setSource(source);
            notice.setBusinessNo(businessNo);
            notice.setContent(content);
            notice.setReceiverUserId(receiverUserId);
            notice.setDeliveryChannel(channel);
            if ("IN_APP".equals(channel)) {
                notice.setDeliveryStatus("2");
                notice.setDeliveryTime(new Date());
            } else {
                notice.setDeliveryStatus("0");
            }
            notice.setStatus("0");
            noticeService.createNotice(notice);
        }
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
     * 解析参与人规则命中的审批人集合。
     *
     * @param assigneeType     审批人类型
     * @param assigneeUserId   单人审批用户ID
     * @param candidateUserIds 候选用户ID集合
     * @param assigneeRoleIds  角色ID集合
     * @param assigneePostIds  岗位ID集合
     * @param assigneeDeptId   部门ID
     * @param instance         流程实例
     * @param fallbackAssignee 兜底审批人
     * @param actionContext    动作上下文
     * @return 审批人集合
     */
    private List<AssigneeInfo> resolveParticipantAssigneeList(String assigneeType,
                                                              Long assigneeUserId,
                                                              List<Long> candidateUserIds,
                                                              List<Long> assigneeRoleIds,
                                                              List<Long> assigneePostIds,
                                                              Long assigneeDeptId,
                                                              SysWorkflowInstance instance,
                                                              AssigneeInfo fallbackAssignee,
                                                              ActionContext actionContext) {
        List<AssigneeInfo> assigneeList = new ArrayList<>();
        LinkedHashSet<Long> userIdSet = new LinkedHashSet<>();
        String normalizedType = normalizeParticipantType(assigneeType);
        if ("INITIATOR".equals(normalizedType) || "ORIGINATOR".equals(normalizedType)) {
            appendAssigneeInfo(assigneeList, resolveExplicitAssignee(
                    instance == null ? null : instance.getInitiatorUserId(),
                    instance == null ? null : instance.getInitiatorUserName(),
                    instance == null ? null : instance.getInitiatorNickName()));
        } else if ("DIRECT_LEADER".equals(normalizedType) || "LEADER".equals(normalizedType)) {
            appendAssigneeInfo(assigneeList, resolveDirectLeaderAssignee(instance));
        } else if ("DEPT_LEADER".equals(normalizedType)) {
            appendAssigneeInfo(assigneeList, resolveDeptLeaderAssignee(assigneeDeptId, instance));
        } else if ("ROLE".equals(normalizedType)) {
            userIdSet.addAll(resolveUserIdsByRoleIds(assigneeRoleIds));
        } else if ("POST".equals(normalizedType)) {
            userIdSet.addAll(resolveUserIdsByPostIds(assigneePostIds));
        } else if ("DEPT".equals(normalizedType)) {
            Long targetDeptId = assigneeDeptId != null ? assigneeDeptId : resolveInitiatorDeptId(instance);
            userIdSet.addAll(resolveUserIdsByDeptId(targetDeptId));
        } else {
            if (assigneeUserId != null) {
                appendAssigneeInfo(assigneeList, resolveExplicitAssignee(assigneeUserId, null, null));
            }
            if (candidateUserIds != null) {
                for (Long candidateUserId : candidateUserIds) {
                    appendAssigneeInfo(assigneeList, resolveExplicitAssignee(candidateUserId, null, null));
                }
            }
        }
        if (!userIdSet.isEmpty()) {
            assigneeList.addAll(loadAssigneeList(userIdSet));
        }
        if (assigneeList.isEmpty() && fallbackAssignee != null && fallbackAssignee.userId != null) {
            appendAssigneeInfo(assigneeList, fallbackAssignee);
        }
        if (assigneeList.isEmpty() && actionContext != null && actionContext.userId != null) {
            appendAssigneeInfo(assigneeList, new AssigneeInfo(actionContext.userId, actionContext.userName, actionContext.userNick));
        }
        if (assigneeList.isEmpty() && instance != null && instance.getInitiatorUserId() != null) {
            appendAssigneeInfo(assigneeList, resolveExplicitAssignee(instance.getInitiatorUserId(),
                    instance.getInitiatorUserName(),
                    instance.getInitiatorNickName()));
        }
        return assigneeList;
    }

    /**
     * 解析 SLA 动态目标审批人。
     *
     * @param slaConfig   SLA 配置
     * @param escalate    true 表示升级，否则表示转办
     * @param instance    流程实例
     * @return 审批人信息
     */
    private AssigneeInfo resolveSlaTargetAssignee(WorkflowSlaConfig slaConfig, boolean escalate, SysWorkflowInstance instance) {
        if (slaConfig == null) {
            return null;
        }
        List<AssigneeInfo> assigneeList = resolveParticipantAssigneeList(
                escalate ? slaConfig.escalateTargetType : slaConfig.transferTargetType,
                escalate ? slaConfig.escalateToUserId : slaConfig.transferToUserId,
                Collections.emptyList(),
                escalate ? slaConfig.escalateRoleIds : slaConfig.transferRoleIds,
                escalate ? slaConfig.escalatePostIds : slaConfig.transferPostIds,
                escalate ? slaConfig.escalateDeptId : slaConfig.transferDeptId,
                instance,
                null,
                null);
        return assigneeList.isEmpty() ? null : assigneeList.get(0);
    }

    /**
     * 追加审批人用户ID。
     *
     * @param userIdSet     用户ID集合
     * @param assigneeInfo  审批人信息
     */
    private void appendAssigneeInfo(List<AssigneeInfo> assigneeList, AssigneeInfo assigneeInfo) {
        if (assigneeList == null || assigneeInfo == null || assigneeInfo.userId == null) {
            return;
        }
        boolean exists = assigneeList.stream().anyMatch(item -> Objects.equals(item.userId, assigneeInfo.userId));
        if (!exists) {
            assigneeList.add(assigneeInfo);
        }
    }

    /**
     * 加载审批人集合。
     *
     * @param userIdSet 用户ID集合
     * @return 审批人信息集合
     */
    private List<AssigneeInfo> loadAssigneeList(Set<Long> userIdSet) {
        if (userIdSet == null || userIdSet.isEmpty()) {
            return Collections.emptyList();
        }
        List<SysUser> userList = userService.list(new LambdaQueryWrapper<SysUser>()
                .in(SysUser::getUserId, userIdSet)
                .ne(SysUser::getStatus, "1")
                .ne(SysUser::getDelFlag, "2")
                .orderByAsc(SysUser::getUserId));
        List<AssigneeInfo> assigneeList = new ArrayList<>();
        for (SysUser user : userList) {
            assigneeList.add(new AssigneeInfo(user.getUserId(),
                    normalizeText(user.getUserName()),
                    normalizeText(user.getNickName())));
        }
        return assigneeList;
    }

    /**
     * 按角色解析用户ID集合。
     *
     * @param roleIds 角色ID集合
     * @return 用户ID集合
     */
    private List<Long> resolveUserIdsByRoleIds(List<Long> roleIds) {
        if (roleIds == null || roleIds.isEmpty()) {
            return Collections.emptyList();
        }
        LinkedHashSet<Long> userIdSet = new LinkedHashSet<>();
        List<SysUserRole> relationList = userRoleService.list(new LambdaQueryWrapper<SysUserRole>()
                .in(SysUserRole::getRoleId, roleIds));
        for (SysUserRole relation : relationList) {
            if (relation != null && relation.getUserId() != null) {
                userIdSet.add(relation.getUserId());
            }
        }
        return new ArrayList<>(userIdSet);
    }

    /**
     * 按岗位解析用户ID集合。
     *
     * @param postIds 岗位ID集合
     * @return 用户ID集合
     */
    private List<Long> resolveUserIdsByPostIds(List<Long> postIds) {
        if (postIds == null || postIds.isEmpty()) {
            return Collections.emptyList();
        }
        LinkedHashSet<Long> userIdSet = new LinkedHashSet<>();
        List<SysUserPost> relationList = userPostService.list(new LambdaQueryWrapper<SysUserPost>()
                .in(SysUserPost::getPostId, postIds));
        for (SysUserPost relation : relationList) {
            if (relation != null && relation.getUserId() != null) {
                userIdSet.add(relation.getUserId());
            }
        }
        return new ArrayList<>(userIdSet);
    }

    /**
     * 按部门解析用户ID集合。
     *
     * @param deptId 部门ID
     * @return 用户ID集合
     */
    private List<Long> resolveUserIdsByDeptId(Long deptId) {
        if (deptId == null) {
            return Collections.emptyList();
        }
        List<SysUser> userList = userService.list(new LambdaQueryWrapper<SysUser>()
                .eq(SysUser::getDeptId, deptId)
                .ne(SysUser::getStatus, "1")
                .ne(SysUser::getDelFlag, "2")
                .orderByAsc(SysUser::getUserId));
        List<Long> userIds = new ArrayList<>();
        for (SysUser user : userList) {
            if (user.getUserId() != null) {
                userIds.add(user.getUserId());
            }
        }
        return userIds;
    }

    /**
     * 解析发起人的直属上级。
     *
     * @param instance 流程实例
     * @return 审批人信息
     */
    private AssigneeInfo resolveDirectLeaderAssignee(SysWorkflowInstance instance) {
        return resolveDeptLeaderAssignee(null, instance);
    }

    /**
     * 解析部门负责人审批人。
     *
     * @param explicitDeptId 显式配置的部门ID
     * @param instance       流程实例
     * @return 审批人信息
     */
    private AssigneeInfo resolveDeptLeaderAssignee(Long explicitDeptId, SysWorkflowInstance instance) {
        Long deptId = explicitDeptId != null ? explicitDeptId : resolveInitiatorDeptId(instance);
        if (deptId == null) {
            return null;
        }
        LinkedHashSet<Long> visitedDeptIds = new LinkedHashSet<>();
        Long currentDeptId = deptId;
        while (currentDeptId != null && visitedDeptIds.add(currentDeptId)) {
            SysDept dept = deptService.getById(currentDeptId);
            if (dept == null) {
                return null;
            }
            AssigneeInfo leaderAssignee = resolveLeaderByDept(dept);
            if (leaderAssignee != null) {
                return leaderAssignee;
            }
            currentDeptId = dept.getParentId();
        }
        return null;
    }

    /**
     * 解析部门负责人。
     *
     * @param dept 部门对象
     * @return 审批人信息
     */
    private AssigneeInfo resolveLeaderByDept(SysDept dept) {
        if (dept == null || !StringUtils.hasText(dept.getLeader())) {
            return null;
        }
        String leaderText = dept.getLeader().trim();
        List<SysUser> userList = userService.list(new LambdaQueryWrapper<SysUser>()
                .and(wrapper -> wrapper.eq(SysUser::getUserName, leaderText)
                        .or()
                        .eq(SysUser::getNickName, leaderText))
                .ne(SysUser::getStatus, "1")
                .ne(SysUser::getDelFlag, "2")
                .orderByAsc(SysUser::getUserId)
                .last("LIMIT 1"));
        if (userList.isEmpty()) {
            return null;
        }
        SysUser user = userList.get(0);
        return new AssigneeInfo(user.getUserId(), normalizeText(user.getUserName()), normalizeText(user.getNickName()));
    }

    /**
     * 解析发起人的部门ID。
     *
     * @param instance 流程实例
     * @return 部门ID
     */
    private Long resolveInitiatorDeptId(SysWorkflowInstance instance) {
        if (instance == null || instance.getInitiatorUserId() == null) {
            return null;
        }
        SysUser initiator = userService.getById(instance.getInitiatorUserId());
        return initiator == null ? null : initiator.getDeptId();
    }

    /**
     * 标准化参与人类型。
     *
     * @param assigneeType 参与人类型
     * @return 标准值
     */
    private String normalizeParticipantType(String assigneeType) {
        return StringUtils.hasText(assigneeType) ? assigneeType.trim().toUpperCase(Locale.ROOT) : "USER";
    }

    /**
     * 解析指定节点默认审批人。
     *
     * @param instance       流程实例
     * @param targetNodeKey  目标节点编码
     * @param targetNodeName 目标节点名称
     * @param fallbackUserId 兜底用户ID
     * @param fallbackName   兜底账号
     * @param fallbackNick   兜底昵称
     * @return 审批人信息
     */
    private AssigneeInfo resolveAssigneeByNode(SysWorkflowInstance instance,
                                               String targetNodeKey,
                                               String targetNodeName,
                                               Long fallbackUserId,
                                               String fallbackName,
                                               String fallbackNick) {
        if (instance == null) {
            return null;
        }
        WorkflowModel model = parseWorkflowModel(instance.getModelContentSnapshot());
        WorkflowNode node = model.resolveReturnNode(targetNodeKey, targetNodeName);
        if (node != null) {
            List<AssigneeInfo> assigneeList = resolveParticipantAssigneeList(node.assigneeType,
                    node.assigneeUserId,
                    node.candidateUserIds,
                    node.assigneeRoleIds,
                    node.assigneePostIds,
                    node.assigneeDeptId,
                    instance,
                    null,
                    null);
            if (!assigneeList.isEmpty()) {
                return assigneeList.get(0);
            }
        }
        AssigneeInfo fallbackAssignee = resolveExplicitAssignee(fallbackUserId, fallbackName, fallbackNick);
        if (fallbackAssignee != null) {
            return fallbackAssignee;
        }
        return resolveExplicitAssignee(instance.getInitiatorUserId(), instance.getInitiatorUserName(), instance.getInitiatorNickName());
    }

    /**
     * 解析显式审批人。
     *
     * @param userId   用户ID
     * @param userName 用户账号
     * @param nickName 用户昵称
     * @return 审批人信息
     */
    private AssigneeInfo resolveExplicitAssignee(Long userId, String userName, String nickName) {
        if (userId == null) {
            return null;
        }
        SysUser user = userService.getById(userId);
        if (user != null) {
            return new AssigneeInfo(userId, normalizeText(user.getUserName()), normalizeText(user.getNickName()));
        }
        return new AssigneeInfo(userId, normalizeText(userName), normalizeText(nickName));
    }

    /**
     * 解析流程模型。
     *
     * @param modelJson 流程模型JSON
     * @return 流程模型对象
     */
    private WorkflowModel parseWorkflowModel(String modelJson) {
        WorkflowModel model = new WorkflowModel();
        if (!StringUtils.hasText(modelJson)) {
            return model;
        }
        try {
            Map<String, Object> root = objectMapper.readValue(modelJson, new TypeReference<Map<String, Object>>() {
            });
            Object startNodeKey = root.get("startNodeKey");
            if (startNodeKey != null) {
                model.startNodeKey = String.valueOf(startNodeKey);
            }
            model.slaConfig = parseSlaConfig(root.get("slaConfig"));
            Object nodesObj = root.get("nodes");
            if (nodesObj instanceof List) {
                for (Object item : (List<?>) nodesObj) {
                    if (!(item instanceof Map)) {
                        continue;
                    }
                    Map<?, ?> nodeMap = (Map<?, ?>) item;
                    WorkflowNode node = new WorkflowNode();
                    node.nodeKey = readString(nodeMap, "nodeKey", "id", "key");
                    if (!StringUtils.hasText(node.nodeKey)) {
                        continue;
                    }
                    node.nodeName = readString(nodeMap, "nodeName", "name", "label");
                    if (!StringUtils.hasText(node.nodeName)) {
                        node.nodeName = node.nodeKey;
                    }
                    node.nodeType = normalizeNodeType(readString(nodeMap, "nodeType", "type"));
                    node.assigneeType = readString(nodeMap, "assigneeType", "approverType");
                    node.assigneeUserId = parseLong(readString(nodeMap, "assigneeUserId", "approverUserId"));
                    node.assigneeDeptId = parseLong(readString(nodeMap, "assigneeDeptId", "deptId"));
                    node.anyApprove = "ANY".equalsIgnoreCase(readString(nodeMap, "approveStrategy", "signType", "countersignStrategy"));
                    node.candidateUserIds.addAll(parseUserIds(nodeMap.get("candidateUserIds")));
                    node.candidateUserIds.addAll(parseUserIds(nodeMap.get("assignees")));
                    node.assigneeRoleIds.addAll(parseUserIds(nodeMap.get("assigneeRoleIds")));
                    node.assigneeRoleIds.addAll(parseUserIds(nodeMap.get("roleIds")));
                    node.assigneePostIds.addAll(parseUserIds(nodeMap.get("assigneePostIds")));
                    node.assigneePostIds.addAll(parseUserIds(nodeMap.get("postIds")));
                    node.ccUserIds.addAll(parseUserIds(nodeMap.get("ccUserIds")));
                    node.ccUserIds.addAll(parseUserIds(nodeMap.get("ccUsers")));
                    node.slaConfig = parseSlaConfig(nodeMap.get("slaConfig"));
                    model.nodeMap.put(node.nodeKey, node);
                    model.nodeOrder.add(node.nodeKey);
                }
            }
            Object edgesObj = root.get("edges");
            if (edgesObj instanceof List) {
                for (Object item : (List<?>) edgesObj) {
                    if (!(item instanceof Map)) {
                        continue;
                    }
                    Map<?, ?> edgeMap = (Map<?, ?>) item;
                    WorkflowEdge edge = new WorkflowEdge();
                    edge.from = readString(edgeMap, "from", "source", "sourceNodeKey");
                    edge.to = readString(edgeMap, "to", "target", "targetNodeKey");
                    edge.condition = readString(edgeMap, "condition", "conditionExpression", "expression");
                    if (StringUtils.hasText(edge.from) && StringUtils.hasText(edge.to)) {
                        model.edges.add(edge);
                    }
                }
            }
            if (!StringUtils.hasText(model.startNodeKey) && !model.nodeOrder.isEmpty()) {
                model.startNodeKey = model.nodeOrder.get(0);
            }
        } catch (Exception ignore) {
            return new WorkflowModel();
        }
        return model;
    }

    /**
     * 校验流程模型是否可用于发起流程。
     *
     * @param model 流程模型
     * @return true 表示模型可运行
     */
    private boolean isRunnableStartModel(WorkflowModel model) {
        if (model == null || model.nodeMap.isEmpty()) {
            return false;
        }
        String startNodeKey = model.resolveStartNodeKey();
        if (!StringUtils.hasText(startNodeKey) || !model.nodeMap.containsKey(startNodeKey.trim())) {
            return false;
        }
        for (WorkflowEdge edge : model.edges) {
            if (edge == null || !StringUtils.hasText(edge.from) || !StringUtils.hasText(edge.to)) {
                return false;
            }
            if (!model.nodeMap.containsKey(edge.from.trim()) || !model.nodeMap.containsKey(edge.to.trim())) {
                return false;
            }
        }
        return hasReachableApprovalNode(model, startNodeKey.trim(), new HashSet<>());
    }

    /**
     * 判断起始节点向后是否存在可执行的审批节点。
     *
     * @param model       流程模型
     * @param currentNode 当前节点
     * @param visitedNode 已访问节点集合
     * @return true 表示存在可执行审批节点
     */
    private boolean hasReachableApprovalNode(WorkflowModel model, String currentNode, Set<String> visitedNode) {
        if (model == null || !StringUtils.hasText(currentNode) || visitedNode == null || !visitedNode.add(currentNode)) {
            return false;
        }
        WorkflowNode node = model.nodeMap.get(currentNode);
        if (node == null) {
            return false;
        }
        if ("approval".equals(normalizeNodeType(node.nodeType))) {
            return true;
        }
        for (WorkflowEdge edge : model.edges) {
            if (edge == null || !Objects.equals(currentNode, edge.from)) {
                continue;
            }
            if (hasReachableApprovalNode(model, edge.to, visitedNode)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 解析JSON为Map结构。
     *
     * @param json JSON文本
     * @return Map结构
     */
    private Map<String, Object> parseJsonMap(String json) {
        if (!StringUtils.hasText(json)) {
            return new LinkedHashMap<>();
        }
        try {
            Map<String, Object> map = objectMapper.readValue(json, new TypeReference<Map<String, Object>>() {
            });
            return map == null ? new LinkedHashMap<>() : map;
        } catch (Exception ignore) {
            return new LinkedHashMap<>();
        }
    }

    /**
     * 写出JSON文本。
     *
     * @param value 值对象
     * @return JSON文本
     */
    private String writeJson(Object value) {
        try {
            return value == null ? null : objectMapper.writeValueAsString(value);
        } catch (Exception ignore) {
            return null;
        }
    }

    /**
     * 规范化通知渠道。
     *
     * @param channels 渠道列表
     * @return 渠道集合
     */
    private Set<String> normalizeChannels(List<String> channels) {
        LinkedHashSet<String> channelSet = new LinkedHashSet<>();
        if (channels != null) {
            for (String channel : channels) {
                if (!StringUtils.hasText(channel)) {
                    continue;
                }
                String normalized = channel.trim().toUpperCase(Locale.ROOT);
                if ("IN_APP".equals(normalized) || "SMS".equals(normalized) || "WECOM".equals(normalized)) {
                    channelSet.add(normalized);
                }
            }
        }
        if (channelSet.isEmpty()) {
            channelSet.add("IN_APP");
        }
        return channelSet;
    }

    /**
     * 判断字段值是否为空。
     *
     * @param value 字段值
     * @return true 表示为空
     */
    private boolean isBlankValue(Object value) {
        if (value == null) {
            return true;
        }
        if (value instanceof String) {
            return !StringUtils.hasText((String) value);
        }
        if (value instanceof List) {
            return ((List<?>) value).isEmpty();
        }
        if (value instanceof Map) {
            return ((Map<?, ?>) value).isEmpty();
        }
        return false;
    }

    /**
     * 生成或解析发起业务单号。
     *
     * @param startBody   发起参数
     * @param definition  流程定义
     * @param currentTime 当前时间
     * @return 业务单号
     */
    private String resolveBusinessNo(WorkflowStartBody startBody, SysWorkflowDefinition definition, Date currentTime) {
        if (startBody != null && StringUtils.hasText(startBody.getBusinessNo())) {
            return startBody.getBusinessNo().trim();
        }
        String prefix = "WF";
        if (definition != null && StringUtils.hasText(definition.getProcessKey())) {
            String normalizedProcessKey = definition.getProcessKey().replaceAll("[^A-Za-z0-9]", "").toUpperCase(Locale.ROOT);
            if (StringUtils.hasText(normalizedProcessKey)) {
                prefix = normalizedProcessKey.length() > 8 ? normalizedProcessKey.substring(0, 8) : normalizedProcessKey;
            }
        }
        Date now = currentTime == null ? new Date() : currentTime;
        return prefix + "-" + new java.text.SimpleDateFormat("yyyyMMddHHmmssSSS").format(now);
    }

    /**
     * 解析业务类型，缺失时回退流程分类。
     *
     * @param businessType 业务类型
     * @param definition   流程定义
     * @return 业务类型
     */
    private String resolveBusinessType(String businessType, SysWorkflowDefinition definition) {
        if (StringUtils.hasText(businessType)) {
            return businessType.trim();
        }
        if (definition == null) {
            return null;
        }
        if (StringUtils.hasText(definition.getCategory())) {
            return definition.getCategory().trim();
        }
        return normalizeText(definition.getProcessName());
    }

    /**
     * 规范化JSON文本。
     *
     * @param jsonText 原始JSON
     * @return 规范化JSON
     */
    private String normalizeJsonText(String jsonText) {
        return StringUtils.hasText(jsonText) ? jsonText.trim() : null;
    }

    /**
     * 规范化JSON文本，并在为空时回退默认值。
     *
     * @param jsonText      原始JSON
     * @param defaultValue  默认值
     * @return 规范化JSON
     */
    private String normalizeJsonText(String jsonText, String defaultValue) {
        String normalized = normalizeJsonText(jsonText);
        return normalized == null ? defaultValue : normalized;
    }

    /**
     * 解析表单定义。
     *
     * @param schemaJson 表单Schema JSON
     * @return 表单定义对象
     */
    private FormSchema parseFormSchema(String schemaJson) {
        FormSchema formSchema = new FormSchema();
        if (!StringUtils.hasText(schemaJson)) {
            return formSchema;
        }
        try {
            Map<String, Object> root = objectMapper.readValue(schemaJson, new TypeReference<Map<String, Object>>() {
            });
            Object versionObj = root.get("version");
            if (versionObj instanceof Number) {
                formSchema.version = ((Number) versionObj).intValue();
            }
            Object fieldsObj = root.get("fields");
            if (fieldsObj instanceof List) {
                for (Object fieldItem : (List<?>) fieldsObj) {
                    if (!(fieldItem instanceof Map)) {
                        continue;
                    }
                    Map<?, ?> fieldMap = (Map<?, ?>) fieldItem;
                    String fieldCode = readString(fieldMap, "fieldCode", "code", "name", "prop");
                    if (!StringUtils.hasText(fieldCode)) {
                        continue;
                    }
                    SchemaField field = new SchemaField();
                    field.fieldCode = fieldCode;
                    field.fieldLabel = readString(fieldMap, "fieldLabel", "label", "title");
                    if (!StringUtils.hasText(field.fieldLabel)) {
                        field.fieldLabel = field.fieldCode;
                    }
                    field.componentType = readString(fieldMap, "componentType", "type");
                    if (!StringUtils.hasText(field.componentType)) {
                        field.componentType = "input";
                    }
                    Object requiredObj = fieldMap.get("required");
                    field.required = requiredObj instanceof Boolean && (Boolean) requiredObj;
                    field.placeholder = readString(fieldMap, "placeholder");
                    Object optionsObj = fieldMap.get("options");
                    if (optionsObj instanceof List) {
                        for (Object optionItem : (List<?>) optionsObj) {
                            if (optionItem instanceof Map) {
                                field.options.add((Map<String, Object>) optionItem);
                            }
                        }
                    }
                    formSchema.fields.add(field);
                }
            }

            Object nodePermissionObj = root.get("nodePermissions");
            if (nodePermissionObj instanceof Map) {
                Map<?, ?> nodePermissionMap = (Map<?, ?>) nodePermissionObj;
                for (Map.Entry<?, ?> nodeEntry : nodePermissionMap.entrySet()) {
                    String nodeRef = String.valueOf(nodeEntry.getKey());
                    if (!(nodeEntry.getValue() instanceof Map)) {
                        continue;
                    }
                    Map<?, ?> fieldPermissionMap = (Map<?, ?>) nodeEntry.getValue();
                    for (Map.Entry<?, ?> fieldEntry : fieldPermissionMap.entrySet()) {
                        FieldPermission permission = parseFieldPermission(fieldEntry.getValue());
                        formSchema.bindPermission(nodeRef, String.valueOf(fieldEntry.getKey()), permission);
                    }
                }
            }
        } catch (Exception ignore) {
            return new FormSchema();
        }
        return formSchema;
    }

    /**
     * 解析字段权限配置。
     *
     * @param source 原始配置
     * @return 字段权限
     */
    private FieldPermission parseFieldPermission(Object source) {
        FieldPermission permission = new FieldPermission();
        if (source instanceof String) {
            permission.permission = normalizePermission((String) source);
            return permission;
        }
        if (!(source instanceof Map)) {
            return permission;
        }
        Map<?, ?> sourceMap = (Map<?, ?>) source;
        permission.permission = normalizePermission(readString(sourceMap, "permission", "mode"));
        Object requiredObj = sourceMap.get("required");
        if (requiredObj instanceof Boolean) {
            permission.required = (Boolean) requiredObj;
        }
        Object visibleObj = sourceMap.get("visible");
        if (visibleObj instanceof Boolean && !((Boolean) visibleObj)) {
            permission.permission = "hidden";
        }
        Object readOnlyObj = sourceMap.get("readOnly");
        if (readOnlyObj instanceof Boolean && ((Boolean) readOnlyObj)) {
            permission.permission = "read";
        }
        return permission;
    }

    /**
     * 标准化字段权限值。
     *
     * @param permission 原始权限
     * @return 标准权限
     */
    private String normalizePermission(String permission) {
        if (!StringUtils.hasText(permission)) {
            return "edit";
        }
        String normalized = permission.trim().toLowerCase(Locale.ROOT);
        if ("hidden".equals(normalized) || "hide".equals(normalized) || "invisible".equals(normalized)) {
            return "hidden";
        }
        if ("read".equals(normalized) || "readonly".equals(normalized) || "view".equals(normalized)) {
            return "read";
        }
        return "edit";
    }

    /**
     * 读取映射中的首个非空字符串。
     *
     * @param source 数据源
     * @param keys   key 列表
     * @return 字符串值
     */
    private String readString(Map<?, ?> source, String... keys) {
        if (source == null || keys == null) {
            return null;
        }
        for (String key : keys) {
            Object value = source.get(key);
            if (value != null && StringUtils.hasText(String.valueOf(value))) {
                return String.valueOf(value);
            }
        }
        return null;
    }

    /**
     * 解析用户ID集合。
     *
     * @param source 原始对象
     * @return 用户ID集合
     */
    private List<Long> parseUserIds(Object source) {
        List<Long> userIds = new ArrayList<>();
        if (source instanceof List) {
            for (Object item : (List<?>) source) {
                Long value = parseLong(String.valueOf(item));
                if (value != null) {
                    userIds.add(value);
                }
            }
        } else if (source != null) {
            String[] split = String.valueOf(source).split(",");
            for (String item : split) {
                Long value = parseLong(item);
                if (value != null) {
                    userIds.add(value);
                }
            }
        }
        return userIds;
    }

    /**
     * 解析Long值。
     *
     * @param value 文本值
     * @return Long值
     */
    private Long parseLong(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        try {
            return Long.parseLong(value.trim());
        } catch (Exception ignore) {
            return null;
        }
    }

    /**
     * 解析整数值。
     *
     * @param value 文本值
     * @return 整数值
     */
    private Integer parseInteger(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        try {
            return Integer.parseInt(value.trim());
        } catch (Exception ignore) {
            return null;
        }
    }

    /**
     * 解析 SLA 配置。
     *
     * @param source 原始对象
     * @return SLA 配置
     */
    private WorkflowSlaConfig parseSlaConfig(Object source) {
        WorkflowSlaConfig config = new WorkflowSlaConfig();
        if (!(source instanceof Map)) {
            return config;
        }
        Map<?, ?> dataMap = (Map<?, ?>) source;
        config.enabled = parseBooleanValue(dataMap.get("enabled"));
        Integer durationMinutes = parseInteger(readString(dataMap, "durationMinutes", "limitMinutes"));
        Integer durationHours = parseInteger(readString(dataMap, "durationHours", "limitHours"));
        if (durationMinutes != null && durationMinutes > 0) {
            config.durationMinutes = durationMinutes;
        } else if (durationHours != null && durationHours > 0) {
            config.durationMinutes = durationHours * 60;
        }
        Integer reminderBeforeMinutes = parseInteger(readString(dataMap, "reminderBeforeMinutes", "warnBeforeMinutes"));
        config.reminderBeforeMinutes = reminderBeforeMinutes == null ? 0 : Math.max(0, reminderBeforeMinutes);
        config.escalateTargetType = readString(dataMap, "escalateTargetType", "upgradeTargetType");
        config.escalateToUserId = parseLong(readString(dataMap, "escalateToUserId", "upgradeToUserId"));
        config.escalateDeptId = parseLong(readString(dataMap, "escalateDeptId", "upgradeDeptId"));
        config.escalateRoleIds.addAll(parseUserIds(dataMap.get("escalateRoleIds")));
        config.escalatePostIds.addAll(parseUserIds(dataMap.get("escalatePostIds")));
        config.transferTargetType = readString(dataMap, "transferTargetType", "autoTransferTargetType");
        config.transferToUserId = parseLong(readString(dataMap, "transferToUserId", "autoTransferUserId"));
        config.transferDeptId = parseLong(readString(dataMap, "transferDeptId", "autoTransferDeptId"));
        config.transferRoleIds.addAll(parseUserIds(dataMap.get("transferRoleIds")));
        config.transferPostIds.addAll(parseUserIds(dataMap.get("transferPostIds")));
        config.channels.addAll(parseStringList(dataMap.get("channels")));
        config.actionSet.addAll(normalizeSlaActionSet(dataMap.get("actions")));
        if (config.actionSet.isEmpty() && config.enabled) {
            config.actionSet.add("REMIND");
        }
        return config;
    }

    /**
     * 解析布尔值。
     *
     * @param source 原始对象
     * @return true 表示命中真值
     */
    private boolean parseBooleanValue(Object source) {
        if (source == null) {
            return false;
        }
        if (source instanceof Boolean) {
            return (Boolean) source;
        }
        return "true".equalsIgnoreCase(String.valueOf(source).trim())
                || "1".equals(String.valueOf(source).trim())
                || "yes".equalsIgnoreCase(String.valueOf(source).trim());
    }

    /**
     * 解析字符串列表。
     *
     * @param source 原始对象
     * @return 字符串列表
     */
    private List<String> parseStringList(Object source) {
        List<String> valueList = new ArrayList<>();
        if (source instanceof List) {
            for (Object item : (List<?>) source) {
                if (item != null && StringUtils.hasText(String.valueOf(item))) {
                    valueList.add(String.valueOf(item).trim());
                }
            }
            return valueList;
        }
        if (source != null && StringUtils.hasText(String.valueOf(source))) {
            String[] values = String.valueOf(source).split(",");
            for (String item : values) {
                if (StringUtils.hasText(item)) {
                    valueList.add(item.trim());
                }
            }
        }
        return valueList;
    }

    /**
     * 规范化 SLA 动作集合。
     *
     * @param source 原始对象
     * @return 动作集合
     */
    private Set<String> normalizeSlaActionSet(Object source) {
        Set<String> actionSet = new HashSet<>();
        for (String item : parseStringList(source)) {
            String normalized = item.trim().toUpperCase(Locale.ROOT);
            if ("REMIND".equals(normalized) || "ESCALATE".equals(normalized) || "TRANSFER".equals(normalized)) {
                actionSet.add(normalized);
            }
        }
        return actionSet;
    }

    /**
     * 规范化流程节点类型。
     *
     * @param nodeType 原始节点类型
     * @return 规范化节点类型
     */
    private String normalizeNodeType(String nodeType) {
        if (!StringUtils.hasText(nodeType)) {
            return "approval";
        }
        String normalized = nodeType.trim().toLowerCase(Locale.ROOT);
        if ("start".equals(normalized) || "end".equals(normalized) || "cc".equals(normalized)
                || "gateway".equals(normalized) || "parallel".equals(normalized)) {
            return normalized;
        }
        return "approval";
    }

    /**
     * 计算简易条件表达式。
     * 支持格式：field >= 100、form.amount == 200、field != 'abc'
     *
     * @param expression 表达式
     * @param formData   表单数据
     * @return true 表示条件命中
     */
    private boolean evaluateSimpleCondition(String expression, Map<String, Object> formData) {
        if (!StringUtils.hasText(expression)) {
            return true;
        }
        String normalized = expression.trim();
        if ("true".equalsIgnoreCase(normalized)) {
            return true;
        }
        if ("false".equalsIgnoreCase(normalized)) {
            return false;
        }
        String[] operators = new String[]{">=", "<=", "==", "!=", ">", "<"};
        String fieldPart = null;
        String valuePart = null;
        String operator = null;
        for (String item : operators) {
            int idx = normalized.indexOf(item);
            if (idx > 0) {
                fieldPart = normalized.substring(0, idx).trim();
                valuePart = normalized.substring(idx + item.length()).trim();
                operator = item;
                break;
            }
        }
        if (!StringUtils.hasText(fieldPart) || !StringUtils.hasText(valuePart) || !StringUtils.hasText(operator)) {
            return false;
        }
        if (fieldPart.startsWith("form.")) {
            fieldPart = fieldPart.substring(5);
        }
        Object leftValue = formData == null ? null : formData.get(fieldPart);
        Object rightValue = parseConditionValue(valuePart);
        if (leftValue == null && rightValue == null) {
            return "==".equals(operator);
        }
        if (leftValue == null || rightValue == null) {
            return "!=".equals(operator);
        }
        if (leftValue instanceof Number && rightValue instanceof Number) {
            double left = ((Number) leftValue).doubleValue();
            double right = ((Number) rightValue).doubleValue();
            if (">=".equals(operator)) {
                return left >= right;
            }
            if ("<=".equals(operator)) {
                return left <= right;
            }
            if (">".equals(operator)) {
                return left > right;
            }
            if ("<".equals(operator)) {
                return left < right;
            }
            if ("==".equals(operator)) {
                return left == right;
            }
            return left != right;
        }
        String leftText = String.valueOf(leftValue);
        String rightText = String.valueOf(rightValue);
        int compare = leftText.compareTo(rightText);
        if (">=".equals(operator)) {
            return compare >= 0;
        }
        if ("<=".equals(operator)) {
            return compare <= 0;
        }
        if (">".equals(operator)) {
            return compare > 0;
        }
        if ("<".equals(operator)) {
            return compare < 0;
        }
        if ("==".equals(operator)) {
            return compare == 0;
        }
        return compare != 0;
    }

    /**
     * 解析条件比较值。
     *
     * @param source 条件右值
     * @return 解析结果
     */
    private Object parseConditionValue(String source) {
        if (!StringUtils.hasText(source)) {
            return null;
        }
        String normalized = source.trim();
        if ((normalized.startsWith("'") && normalized.endsWith("'"))
                || (normalized.startsWith("\"") && normalized.endsWith("\""))) {
            return normalized.substring(1, normalized.length() - 1);
        }
        if ("true".equalsIgnoreCase(normalized) || "false".equalsIgnoreCase(normalized)) {
            return Boolean.parseBoolean(normalized);
        }
        try {
            if (normalized.contains(".")) {
                return Double.parseDouble(normalized);
            }
            return Long.parseLong(normalized);
        } catch (Exception ignore) {
            return normalized;
        }
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
     * 路由结果对象。
     */
    private static final class RouteResult {
        private final List<SysWorkflowTask> createdTasks = new ArrayList<>();
        private final Set<String> activeNodeNames = new LinkedHashSet<>();
    }

    /**
     * 动作上下文对象。
     */
    private static final class ActionContext {
        private final Long userId;
        private final String userName;
        private final String userNick;

        /**
         * 构造动作上下文。
         *
         * @param userId   用户ID
         * @param userName 用户账号
         * @param userNick 用户昵称
         */
        private ActionContext(Long userId, String userName, String userNick) {
            this.userId = userId;
            this.userName = userName;
            this.userNick = userNick;
        }
    }

    /**
     * 节点激活结果对象。
     */
    private static final class ActivationResult {
        private boolean success = true;
        private final List<SysWorkflowTask> createdTasks = new ArrayList<>();
        private final Set<String> activeNodeNames = new LinkedHashSet<>();
    }

    /**
     * 流程模型对象。
     */
    private final class WorkflowModel {
        private String startNodeKey;
        private final Map<String, WorkflowNode> nodeMap = new LinkedHashMap<>();
        private final List<String> nodeOrder = new ArrayList<>();
        private final List<WorkflowEdge> edges = new ArrayList<>();
        private WorkflowSlaConfig slaConfig = new WorkflowSlaConfig();

        /**
         * 获取起始节点编码。
         *
         * @return 起始节点编码
         */
        private String resolveStartNodeKey() {
            if (StringUtils.hasText(startNodeKey)) {
                return startNodeKey;
            }
            return nodeOrder.isEmpty() ? null : nodeOrder.get(0);
        }

        /**
         * 判断节点是否为任一同意通过策略。
         *
         * @param nodeKey 节点编码
         * @return true 表示任一同意
         */
        private boolean isAnyApprove(String nodeKey) {
            WorkflowNode node = nodeMap.get(nodeKey);
            return node != null && node.anyApprove;
        }

        /**
         * 根据节点与表单数据计算下一跳节点集合。
         *
         * @param fromNodeKey 当前节点编码
         * @param formData    表单数据
         * @return 下一跳节点集合
         */
        private List<String> resolveNextNodeKeys(String fromNodeKey, Map<String, Object> formData) {
            List<String> nextNodes = new ArrayList<>();
            for (WorkflowEdge edge : edges) {
                if (!Objects.equals(edge.from, fromNodeKey)) {
                    continue;
                }
                if (StringUtils.hasText(edge.condition) && !evaluateSimpleCondition(edge.condition, formData)) {
                    continue;
                }
                nextNodes.add(edge.to);
            }
            return nextNodes;
        }

        /**
         * 解析节点的前驱节点集合。
         *
         * @param toNodeKey 目标节点编码
         * @return 前驱节点编码集合
         */
        private List<String> resolvePreviousNodeKeys(String toNodeKey) {
            List<String> previousNodes = new ArrayList<>();
            for (WorkflowEdge edge : edges) {
                if (Objects.equals(edge.to, toNodeKey) && StringUtils.hasText(edge.from)) {
                    previousNodes.add(edge.from);
                }
            }
            return previousNodes;
        }

        /**
         * 按节点编码或名称匹配节点。
         *
         * @param nodeKey  节点编码
         * @param nodeName 节点名称
         * @return 节点对象
         */
        private WorkflowNode resolveReturnNode(String nodeKey, String nodeName) {
            if (StringUtils.hasText(nodeKey) && nodeMap.containsKey(nodeKey.trim())) {
                return nodeMap.get(nodeKey.trim());
            }
            if (StringUtils.hasText(nodeName)) {
                for (WorkflowNode node : nodeMap.values()) {
                    if (nodeName.trim().equals(node.nodeName)) {
                        return node;
                    }
                }
            }
            return null;
        }
    }

    /**
     * 流程节点对象。
     */
    private static final class WorkflowNode {
        private String nodeKey;
        private String nodeName;
        private String nodeType;
        private String assigneeType;
        private Long assigneeUserId;
        private Long assigneeDeptId;
        private boolean anyApprove;
        private final List<Long> candidateUserIds = new ArrayList<>();
        private final List<Long> assigneeRoleIds = new ArrayList<>();
        private final List<Long> assigneePostIds = new ArrayList<>();
        private final List<Long> ccUserIds = new ArrayList<>();
        private WorkflowSlaConfig slaConfig = new WorkflowSlaConfig();
    }

    /**
     * 流程连线对象。
     */
    private static final class WorkflowEdge {
        private String from;
        private String to;
        private String condition;
    }

    /**
     * 流程 SLA 配置对象。
     */
    private static final class WorkflowSlaConfig {
        private boolean enabled;
        private int durationMinutes;
        private int reminderBeforeMinutes;
        private String escalateTargetType;
        private Long escalateToUserId;
        private Long escalateDeptId;
        private final List<Long> escalateRoleIds = new ArrayList<>();
        private final List<Long> escalatePostIds = new ArrayList<>();
        private String transferTargetType;
        private Long transferToUserId;
        private Long transferDeptId;
        private final List<Long> transferRoleIds = new ArrayList<>();
        private final List<Long> transferPostIds = new ArrayList<>();
        private final List<String> channels = new ArrayList<>();
        private final Set<String> actionSet = new HashSet<>();
    }

    /**
     * 动态表单定义。
     */
    private static final class FormSchema {
        private int version = 1;
        private final List<SchemaField> fields = new ArrayList<>();
        private final Map<String, Map<String, FieldPermission>> permissionMap = new LinkedHashMap<>();

        /**
         * 绑定字段权限。
         *
         * @param nodeRef   节点引用
         * @param fieldCode 字段编码
         * @param permission 权限信息
         */
        private void bindPermission(String nodeRef, String fieldCode, FieldPermission permission) {
            if (!StringUtils.hasText(nodeRef) || !StringUtils.hasText(fieldCode) || permission == null) {
                return;
            }
            permissionMap.computeIfAbsent(nodeRef.trim(), key -> new LinkedHashMap<>())
                    .put(fieldCode.trim(), permission);
        }

        /**
         * 解析节点字段权限。
         *
         * @param nodeKey   节点编码
         * @param nodeName  节点名称
         * @param fieldCode 字段编码
         * @return 字段权限
         */
        private FieldPermission resolvePermission(String nodeKey, String nodeName, String fieldCode) {
            FieldPermission byNodeKey = resolvePermissionByNode(nodeKey, fieldCode);
            if (byNodeKey != null) {
                return byNodeKey;
            }
            FieldPermission byNodeName = resolvePermissionByNode(nodeName, fieldCode);
            if (byNodeName != null) {
                return byNodeName;
            }
            return new FieldPermission();
        }

        /**
         * 根据节点引用获取字段权限。
         *
         * @param nodeRef   节点引用
         * @param fieldCode 字段编码
         * @return 字段权限
         */
        private FieldPermission resolvePermissionByNode(String nodeRef, String fieldCode) {
            if (!StringUtils.hasText(nodeRef) || !StringUtils.hasText(fieldCode)) {
                return null;
            }
            Map<String, FieldPermission> fieldPermissionMap = permissionMap.get(nodeRef.trim());
            if (fieldPermissionMap == null) {
                return null;
            }
            return fieldPermissionMap.get(fieldCode.trim());
        }
    }

    /**
     * 表单字段定义。
     */
    private static final class SchemaField {
        private String fieldCode;
        private String fieldLabel;
        private String componentType;
        private boolean required;
        private String placeholder;
        private final List<Map<String, Object>> options = new ArrayList<>();
    }

    /**
     * 字段权限定义。
     */
    private static final class FieldPermission {
        private String permission = "edit";
        private Boolean required;
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

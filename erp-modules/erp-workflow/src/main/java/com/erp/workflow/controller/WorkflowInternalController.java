package com.erp.workflow.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.erp.workflow.contract.domain.SysTodoTask;
import com.erp.workflow.contract.domain.SysWorkflowDefinition;
import com.erp.workflow.contract.domain.SysWorkflowInstance;
import com.erp.workflow.contract.domain.SysWorkflowTask;
import com.erp.workflow.contract.domain.vo.WorkflowDefinitionLiteVO;
import com.erp.workflow.contract.domain.vo.WorkflowInstanceDetailVO;
import com.erp.workflow.contract.domain.vo.WorkflowProcessOptionVO;
import com.erp.workflow.contract.domain.vo.WorkflowStartBody;
import com.erp.workflow.contract.domain.vo.WorkflowTaskActionBody;
import com.erp.workflow.domain.platform.SysUser;
import com.erp.workflow.mapper.SysWorkflowTaskMapper;
import com.erp.workflow.security.service.SecurityUserResolver;
import com.erp.workflow.service.ISysTodoTaskService;
import com.erp.workflow.service.ISysUserService;
import com.erp.workflow.service.ISysWorkflowDefinitionService;
import com.erp.workflow.service.ISysWorkflowEngineService;
import com.erp.workflow.service.IWorkflowBindingResolver;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * 工作流内部契约控制层。
 */
@RestController
@RequestMapping("/workflow/internal")
public class WorkflowInternalController {
    private static final String INSTANCE_STATUS_RUNNING = "0";
    private static final String TASK_STATUS_APPROVED = "2";
    private static final String TASK_STATUS_REJECTED = "3";
    private static final String TASK_STATUS_TRANSFERRED = "4";
    private static final String TASK_STATUS_CANCELED = "5";

    private final ISysWorkflowEngineService workflowEngineService;
    private final ISysWorkflowDefinitionService workflowDefinitionService;
    private final IWorkflowBindingResolver workflowBindingResolver;
    private final ISysTodoTaskService todoTaskService;
    private final ISysUserService userService;
    private final SysWorkflowTaskMapper workflowTaskMapper;
    private final SecurityUserResolver securityUserResolver;

    public WorkflowInternalController(ISysWorkflowEngineService workflowEngineService,
            ISysWorkflowDefinitionService workflowDefinitionService,
            IWorkflowBindingResolver workflowBindingResolver,
            ISysTodoTaskService todoTaskService,
            ISysUserService userService,
            SysWorkflowTaskMapper workflowTaskMapper,
            SecurityUserResolver securityUserResolver) {
        this.workflowEngineService = workflowEngineService;
        this.workflowDefinitionService = workflowDefinitionService;
        this.workflowBindingResolver = workflowBindingResolver;
        this.todoTaskService = todoTaskService;
        this.userService = userService;
        this.workflowTaskMapper = workflowTaskMapper;
        this.securityUserResolver = securityUserResolver;
    }

    /**
     * 查询业务动作可选流程列表。
     *
     * @param domainType 业务域类型
     * @param actionCode 动作编码
     * @return 流程选项列表
     */
    @GetMapping("/bindings/options")
    public List<WorkflowProcessOptionVO> bindingOptions(@RequestParam("domainType") String domainType,
            @RequestParam("actionCode") String actionCode) {
        return workflowBindingResolver.listProcessOptions(domainType, actionCode);
    }

    /**
     * 查询指定业务的最新流程实例详情。
     *
     * @param businessType 业务类型
     * @param businessNo 业务单号
     * @return 流程实例详情
     */
    @GetMapping("/instances/latest/detail")
    public WorkflowInstanceDetailVO latestInstanceDetail(@RequestParam("businessType") String businessType,
            @RequestParam("businessNo") String businessNo) {
        List<SysWorkflowInstance> instanceList = workflowEngineService.selectInstanceList(null, null, businessNo);
        SysWorkflowInstance latestInstance = instanceList.stream()
                .filter(instance -> instance != null && StringUtils.hasText(instance.getBusinessType()))
                .filter(instance -> instance.getBusinessType().equalsIgnoreCase(businessType))
                .findFirst()
                .orElse(null);
        if (latestInstance == null || latestInstance.getInstanceId() == null) {
            return null;
        }
        return workflowEngineService.selectInstanceDetail(latestInstance.getInstanceId());
    }

    /**
     * 判断是否存在运行中的流程实例。
     *
     * @param businessType 业务类型
     * @param businessNo 业务单号
     * @return true 表示存在
     */
    @GetMapping("/instances/running")
    public Boolean hasRunningInstance(@RequestParam("businessType") String businessType,
            @RequestParam("businessNo") String businessNo) {
        List<SysWorkflowInstance> instanceList = workflowEngineService.selectInstanceList(null, INSTANCE_STATUS_RUNNING, businessNo);
        return instanceList.stream()
                .filter(Objects::nonNull)
                .anyMatch(instance -> businessType.equalsIgnoreCase(instance.getBusinessType()));
    }

    /**
     * 发起流程。
     *
     * @param startBody 发起参数
     * @return true 表示成功
     */
    @PostMapping("/start")
    public Boolean start(@RequestBody WorkflowStartBody startBody) {
        CurrentUser currentUser = resolveCurrentUser();
        if (currentUser.userId == null || !StringUtils.hasText(currentUser.userName)) {
            return Boolean.FALSE;
        }
        String resolvedProcessKey = resolveProcessKey(startBody);
        if (!StringUtils.hasText(resolvedProcessKey)) {
            return Boolean.FALSE;
        }
        startBody.setProcessKey(resolvedProcessKey);
        return workflowEngineService.startProcess(startBody, currentUser.userId, currentUser.userName, currentUser.nickName);
    }

    /**
     * 按业务标识中止运行中的流程实例。
     *
     * @param businessType 业务类型
     * @param businessNo 业务单号
     * @param actionBody 中止说明
     * @return true 表示成功
     */
    @PostMapping("/instances/abort")
    public Boolean abort(@RequestParam("businessType") String businessType,
            @RequestParam("businessNo") String businessNo,
            @RequestBody(required = false) WorkflowTaskActionBody actionBody) {
        CurrentUser currentUser = resolveCurrentUser();
        if (currentUser.userId == null || !StringUtils.hasText(currentUser.userName)) {
            return Boolean.FALSE;
        }
        List<SysWorkflowInstance> instanceList = workflowEngineService.selectInstanceList(null, INSTANCE_STATUS_RUNNING, businessNo);
        SysWorkflowInstance latestInstance = instanceList.stream()
                .filter(Objects::nonNull)
                .filter(instance -> StringUtils.hasText(instance.getBusinessType()))
                .filter(instance -> instance.getBusinessType().equalsIgnoreCase(businessType))
                .findFirst()
                .orElse(null);
        if (latestInstance == null || latestInstance.getInstanceId() == null) {
            return Boolean.FALSE;
        }
        return workflowEngineService.withdrawInstance(latestInstance.getInstanceId(),
                actionBody,
                currentUser.userId,
                currentUser.userName,
                currentUser.nickName);
    }

    /**
     * 审批通过任务。
     *
     * @param taskId 任务ID
     * @param actionBody 审批参数
     * @return true 表示成功
     */
    @PostMapping("/tasks/approve/{taskId}")
    public Boolean approve(@PathVariable("taskId") Long taskId,
            @RequestBody(required = false) WorkflowTaskActionBody actionBody) {
        CurrentUser currentUser = resolveCurrentUser();
        if (currentUser.userId == null || !StringUtils.hasText(currentUser.userName)) {
            return Boolean.FALSE;
        }
        return workflowEngineService.approveTask(taskId, actionBody, currentUser.userId, currentUser.userName, currentUser.nickName);
    }

    /**
     * 驳回任务。
     *
     * @param taskId 任务ID
     * @param actionBody 审批参数
     * @return true 表示成功
     */
    @PostMapping("/tasks/reject/{taskId}")
    public Boolean reject(@PathVariable("taskId") Long taskId,
            @RequestBody(required = false) WorkflowTaskActionBody actionBody) {
        CurrentUser currentUser = resolveCurrentUser();
        if (currentUser.userId == null || !StringUtils.hasText(currentUser.userName)) {
            return Boolean.FALSE;
        }
        return workflowEngineService.rejectTask(taskId, actionBody, currentUser.userId, currentUser.userName, currentUser.nickName);
    }

    /**
     * 发布流程定义。
     *
     * @param definitionId 流程定义ID
     * @return true 表示成功
     */
    @PostMapping("/definitions/publish/{definitionId}")
    public Boolean publish(@PathVariable("definitionId") Long definitionId) {
        String operator = securityUserResolver.getCurrentUsername();
        if (!StringUtils.hasText(operator)) {
            return Boolean.FALSE;
        }
        return workflowDefinitionService.publishDefinition(definitionId, operator.trim());
    }

    /**
     * 查询流程定义轻量列表。
     *
     * @param processName 流程名称关键字
     * @param processKey  流程标识关键字
     * @param status      状态
     * @return 轻量流程定义列表
     */
    @GetMapping("/definitions/lite")
    public List<WorkflowDefinitionLiteVO> definitionLite(@RequestParam(value = "processName", required = false) String processName,
            @RequestParam(value = "processKey", required = false) String processKey,
            @RequestParam(value = "status", required = false) String status) {
        List<SysWorkflowDefinition> definitionList = workflowDefinitionService.selectList(processName, processKey, null, status);
        if (definitionList == null || definitionList.isEmpty()) {
            return new ArrayList<>();
        }
        return definitionList.stream().map(this::buildDefinitionLite).collect(Collectors.toList());
    }

    /**
     * 签收待办。
     *
     * @param todoId 待办ID
     * @return true 表示成功
     */
    @PostMapping("/todos/claim/{todoId}")
    public Boolean claimTodo(@PathVariable("todoId") Long todoId) {
        CurrentUser currentUser = resolveCurrentUser();
        if (currentUser.userId == null || !StringUtils.hasText(currentUser.userName)) {
            return Boolean.FALSE;
        }
        SysTodoTask todoTask = todoTaskService.getById(todoId);
        if (todoTask == null || todoTask.getAssigneeUserId() == null || !currentUser.userId.equals(todoTask.getAssigneeUserId())) {
            return Boolean.FALSE;
        }
        Long workflowTaskId = resolveWorkflowTaskId(todoTask);
        if (workflowTaskId == null) {
            return todoTaskService.claim(todoId, currentUser.userId);
        }
        return workflowEngineService.claimTask(workflowTaskId, currentUser.userId, currentUser.userName, currentUser.nickName);
    }

    /**
     * 办结待办。
     *
     * @param todoId 待办ID
     * @return true 表示成功
     */
    @PostMapping("/todos/finish/{todoId}")
    public Boolean finishTodo(@PathVariable("todoId") Long todoId) {
        CurrentUser currentUser = resolveCurrentUser();
        if (currentUser.userId == null || !StringUtils.hasText(currentUser.userName)) {
            return Boolean.FALSE;
        }
        SysTodoTask todoTask = todoTaskService.getById(todoId);
        if (todoTask == null || todoTask.getAssigneeUserId() == null || !currentUser.userId.equals(todoTask.getAssigneeUserId())) {
            return Boolean.FALSE;
        }
        Long workflowTaskId = resolveWorkflowTaskId(todoTask);
        if (workflowTaskId == null) {
            return todoTaskService.finish(todoId, currentUser.userId);
        }
        SysWorkflowTask workflowTask = workflowTaskMapper.selectById(workflowTaskId);
        if (workflowTask == null) {
            return todoTaskService.finish(todoId, currentUser.userId);
        }
        if (!Objects.equals(workflowTask.getAssigneeUserId(), currentUser.userId)) {
            if (isTaskTerminalStatus(workflowTask.getStatus())) {
                return todoTaskService.finish(todoId, currentUser.userId);
            }
            return Boolean.FALSE;
        }
        if (isTaskTerminalStatus(workflowTask.getStatus())) {
            return todoTaskService.finish(todoId, currentUser.userId);
        }
        WorkflowTaskActionBody actionBody = new WorkflowTaskActionBody();
        actionBody.setActionComment("AI 助手办结待办");
        boolean success = workflowEngineService.approveTask(workflowTaskId,
                actionBody,
                currentUser.userId,
                currentUser.userName,
                currentUser.nickName);
        if (success) {
            return Boolean.TRUE;
        }
        SysWorkflowTask latestTask = workflowTaskMapper.selectById(workflowTaskId);
        if (latestTask != null && isTaskTerminalStatus(latestTask.getStatus())) {
            return todoTaskService.finish(todoId, currentUser.userId);
        }
        return Boolean.FALSE;
    }

    /**
     * 解析当前用户。
     *
     * @return 当前用户
     */
    private CurrentUser resolveCurrentUser() {
        String userName = securityUserResolver.getCurrentUsername();
        Long userId = securityUserResolver.getCurrentUserId();
        if (!StringUtils.hasText(userName)) {
            return new CurrentUser(userId, null, null);
        }
        SysUser user = userService.selectUserByUserName(userName);
        String nickName = user == null ? userName : user.getNickName();
        Long resolvedUserId = userId != null ? userId : user == null ? null : user.getUserId();
        return new CurrentUser(resolvedUserId, userName.trim(), nickName);
    }

    /**
     * 解析发起流程最终使用的流程标识。
     *
     * @param startBody 发起参数
     * @return 流程标识
     */
    private String resolveProcessKey(WorkflowStartBody startBody) {
        if (startBody == null) {
            return null;
        }
        if (StringUtils.hasText(startBody.getProcessKey())) {
            return startBody.getProcessKey().trim();
        }
        String resolved = workflowBindingResolver.resolveProcessKey(
                startBody.getDomainType(),
                startBody.getActionCode(),
                startBody.getRequestedProcessKey());
        return StringUtils.hasText(resolved) ? resolved.trim() : null;
    }

    /**
     * 构造流程定义轻量对象。
     *
     * @param definition 流程定义实体
     * @return 轻量对象
     */
    private WorkflowDefinitionLiteVO buildDefinitionLite(SysWorkflowDefinition definition) {
        WorkflowDefinitionLiteVO lite = new WorkflowDefinitionLiteVO();
        lite.setDefinitionId(definition.getDefinitionId());
        lite.setProcessKey(definition.getProcessKey());
        lite.setProcessName(definition.getProcessName());
        lite.setCategory(definition.getCategory());
        lite.setVersion(definition.getVersion());
        lite.setStatus(definition.getStatus());
        lite.setPublishTime(definition.getPublishTime());
        lite.setCreateTime(definition.getCreateTime());
        return lite;
    }

    /**
     * 解析待办关联的流程任务ID，历史数据缺失回链时自动补链。
     *
     * @param todoTask 待办任务
     * @return 流程任务ID
     */
    private Long resolveWorkflowTaskId(SysTodoTask todoTask) {
        if (todoTask == null || todoTask.getTodoId() == null) {
            return null;
        }
        if (todoTask.getTaskId() != null) {
            SysWorkflowTask linkedTask = workflowTaskMapper.selectById(todoTask.getTaskId());
            if (linkedTask != null && Objects.equals(todoTask.getTodoId(), linkedTask.getTodoId())) {
                return linkedTask.getTaskId();
            }
        }
        SysWorkflowTask workflowTask = workflowTaskMapper.selectOne(new LambdaQueryWrapper<SysWorkflowTask>()
                .eq(SysWorkflowTask::getTodoId, todoTask.getTodoId())
                .orderByDesc(SysWorkflowTask::getTaskId)
                .last("LIMIT 1"));
        if (workflowTask == null || workflowTask.getTaskId() == null) {
            return null;
        }
        SysTodoTask updateTodo = new SysTodoTask();
        updateTodo.setTodoId(todoTask.getTodoId());
        updateTodo.setInstanceId(workflowTask.getInstanceId());
        updateTodo.setTaskId(workflowTask.getTaskId());
        todoTaskService.updateById(updateTodo);
        return workflowTask.getTaskId();
    }

    /**
     * 判断流程任务是否处于终态。
     *
     * @param status 任务状态
     * @return true 表示终态
     */
    private boolean isTaskTerminalStatus(String status) {
        return TASK_STATUS_APPROVED.equals(status)
                || TASK_STATUS_REJECTED.equals(status)
                || TASK_STATUS_TRANSFERRED.equals(status)
                || TASK_STATUS_CANCELED.equals(status);
    }

    /**
     * 当前用户结构。
     */
    private static final class CurrentUser {
        private final Long userId;
        private final String userName;
        private final String nickName;

        private CurrentUser(Long userId, String userName, String nickName) {
            this.userId = userId;
            this.userName = userName;
            this.nickName = nickName;
        }
    }
}

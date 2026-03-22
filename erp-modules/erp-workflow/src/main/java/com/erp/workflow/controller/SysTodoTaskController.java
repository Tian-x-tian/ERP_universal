package com.erp.workflow.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.erp.common.core.domain.R;
import com.erp.common.core.domain.ResultCode;
import com.erp.workflow.contract.domain.SysTodoTask;
import com.erp.workflow.domain.platform.SysUser;
import com.erp.workflow.contract.domain.SysWorkflowTask;
import com.erp.workflow.contract.domain.vo.WorkflowTaskActionBody;
import com.erp.workflow.mapper.SysWorkflowTaskMapper;
import com.erp.workflow.security.service.SecurityUserResolver;
import com.erp.workflow.service.ISysTodoTaskService;
import com.erp.workflow.service.ISysUserService;
import com.erp.workflow.service.ISysWorkflowEngineService;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Objects;

/**
 * 流程待办任务控制层
 */
@RestController
@ConditionalOnProperty(name = "erp.workflow.http-enabled", havingValue = "true")
@RequestMapping("/workflow/todos")
public class SysTodoTaskController {
    private static final String TASK_STATUS_APPROVED = "2";
    private static final String TASK_STATUS_REJECTED = "3";
    private static final String TASK_STATUS_TRANSFERRED = "4";
    private static final String TASK_STATUS_CANCELED = "5";

    private final ISysTodoTaskService todoTaskService;
    private final ISysWorkflowEngineService workflowEngineService;
    private final ISysUserService userService;
    private final SysWorkflowTaskMapper workflowTaskMapper;
    private final SecurityUserResolver securityUserResolver;

    public SysTodoTaskController(ISysTodoTaskService todoTaskService,
                                 ISysWorkflowEngineService workflowEngineService,
                                 ISysUserService userService,
                                 SysWorkflowTaskMapper workflowTaskMapper,
                                 SecurityUserResolver securityUserResolver) {
        this.todoTaskService = todoTaskService;
        this.workflowEngineService = workflowEngineService;
        this.userService = userService;
        this.workflowTaskMapper = workflowTaskMapper;
        this.securityUserResolver = securityUserResolver;
    }

    /**
     * 查询当前登录用户待办任务列表。
     *
     * @param status 状态（0待处理 1处理中 2已完成）
     * @return 待办任务列表
     */
    @GetMapping("/list")
    @PreAuthorize("@ss.hasPermi('workflow:todo:list')")
    public R<List<SysTodoTask>> list(@RequestParam(value = "status", required = false) String status) {
        CurrentUser currentUser = resolveCurrentUser();
        if (currentUser.userId == null) {
            return R.failed(ResultCode.UNAUTHORIZED);
        }
        return R.success(todoTaskService.selectByCurrentUser(currentUser.userId, status));
    }

    /**
     * 签收待办任务。
     *
     * @param todoId 待办ID
     * @return 更新结果
     */
    @PostMapping("/claim/{todoId}")
    @PreAuthorize("@ss.hasAnyPermi('workflow:todo:claim','workflow:todo:handle')")
    public R<Boolean> claim(@PathVariable("todoId") Long todoId) {
        CurrentUser currentUser = resolveCurrentUser();
        if (currentUser.userId == null || !StringUtils.hasText(currentUser.userName)) {
            return R.failed(ResultCode.UNAUTHORIZED);
        }
        SysTodoTask todoTask = todoTaskService.getById(todoId);
        if (todoTask == null || todoTask.getAssigneeUserId() == null || !currentUser.userId.equals(todoTask.getAssigneeUserId())) {
            return R.failed("待办不存在或无权限签收");
        }
        Long workflowTaskId = resolveWorkflowTaskId(todoTask);
        boolean success = workflowTaskId == null
                ? todoTaskService.claim(todoId, currentUser.userId)
                : workflowEngineService.claimTask(workflowTaskId, currentUser.userId, currentUser.userName, currentUser.nickName);
        return success ? R.success(true) : R.failed("待办签收失败");
    }

    /**
     * 办结待办任务。
     *
     * @param todoId 待办ID
     * @return 更新结果
     */
    @PostMapping("/finish/{todoId}")
    @PreAuthorize("@ss.hasAnyPermi('workflow:todo:finish','workflow:todo:handle')")
    public R<Boolean> finish(@PathVariable("todoId") Long todoId) {
        CurrentUser currentUser = resolveCurrentUser();
        if (currentUser.userId == null || !StringUtils.hasText(currentUser.userName)) {
            return R.failed(ResultCode.UNAUTHORIZED);
        }
        SysTodoTask todoTask = todoTaskService.getById(todoId);
        if (todoTask == null || todoTask.getAssigneeUserId() == null || !currentUser.userId.equals(todoTask.getAssigneeUserId())) {
            return R.failed("待办不存在或无权限办结");
        }
        Long workflowTaskId = resolveWorkflowTaskId(todoTask);
        boolean success;
        if (workflowTaskId == null) {
            success = todoTaskService.finish(todoId, currentUser.userId);
        } else {
            SysWorkflowTask workflowTask = workflowTaskMapper.selectById(workflowTaskId);
            if (workflowTask == null) {
                success = todoTaskService.finish(todoId, currentUser.userId);
                return success ? R.success(true) : R.failed("待办办结失败，流程任务不存在");
            }
            if (!Objects.equals(workflowTask.getAssigneeUserId(), currentUser.userId)) {
                if (isTaskTerminalStatus(workflowTask.getStatus())) {
                    success = todoTaskService.finish(todoId, currentUser.userId);
                    return success ? R.success(true) : R.failed("待办办结失败，任务已处理但待办未同步");
                }
                return R.failed("当前任务已转交给其他审批人，请刷新后重试");
            }
            if (isTaskTerminalStatus(workflowTask.getStatus())) {
                success = todoTaskService.finish(todoId, currentUser.userId);
                return success ? R.success(true) : R.failed("待办办结失败，任务已处理但待办未同步");
            }
            WorkflowTaskActionBody actionBody = new WorkflowTaskActionBody();
            actionBody.setActionComment("待办中心办结");
            success = workflowEngineService.approveTask(workflowTaskId, actionBody, currentUser.userId, currentUser.userName, currentUser.nickName);
            if (!success) {
                SysWorkflowTask latestTask = workflowTaskMapper.selectById(workflowTaskId);
                if (latestTask != null && isTaskTerminalStatus(latestTask.getStatus())) {
                    success = todoTaskService.finish(todoId, currentUser.userId);
                    if (success) {
                        return R.success(true);
                    }
                }
                return R.failed("待办办结失败，流程任务状态已变化，请刷新后重试");
            }
        }
        return success ? R.success(true) : R.failed("待办办结失败");
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
     * 获取当前登录用户上下文。
     *
     * @return 当前用户上下文
     */
    private CurrentUser resolveCurrentUser() {
        String userName = securityUserResolver.getCurrentUsername();
        if (!StringUtils.hasText(userName)) {
            return new CurrentUser(null, null, null);
        }
        SysUser user = userService.selectUserByUserName(userName);
        if (user == null) {
            return new CurrentUser(null, userName, null);
        }
        return new CurrentUser(user.getUserId(), user.getUserName(), user.getNickName());
    }

    /**
     * 当前登录用户结构对象。
     */
    private static final class CurrentUser {
        private final Long userId;
        private final String userName;
        private final String nickName;

        /**
         * 构造当前登录用户结构对象。
         *
         * @param userId   用户ID
         * @param userName 用户账号
         * @param nickName 用户昵称
         */
        private CurrentUser(Long userId, String userName, String nickName) {
            this.userId = userId;
            this.userName = userName;
            this.nickName = nickName;
        }
    }
}





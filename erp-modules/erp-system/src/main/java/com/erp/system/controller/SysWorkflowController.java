package com.erp.system.controller;

import com.erp.common.core.domain.R;
import com.erp.common.core.domain.ResultCode;
import com.erp.system.domain.SysUser;
import com.erp.system.domain.SysWorkflowDefinition;
import com.erp.system.domain.SysWorkflowInstance;
import com.erp.system.domain.SysWorkflowTask;
import com.erp.system.domain.vo.WorkflowInstanceDetailVO;
import com.erp.system.domain.vo.WorkflowStartBody;
import com.erp.system.domain.vo.WorkflowTaskActionBody;
import com.erp.system.domain.vo.WorkflowTaskFormVO;
import com.erp.system.domain.vo.WorkflowTaskRemindBody;
import com.erp.system.domain.vo.WorkflowTaskReturnBody;
import com.erp.system.domain.vo.WorkflowTaskTransferBody;
import com.erp.system.security.service.SecurityUserResolver;
import com.erp.system.service.ISysUserService;
import com.erp.system.service.ISysWorkflowDefinitionService;
import com.erp.system.service.ISysWorkflowEngineService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 流程引擎控制层
 */
@RestController
@RequestMapping("/system/workflow")
public class SysWorkflowController {

    private final ISysWorkflowDefinitionService workflowDefinitionService;
    private final ISysWorkflowEngineService workflowEngineService;
    private final SecurityUserResolver securityUserResolver;
    private final ISysUserService userService;

    public SysWorkflowController(
            ISysWorkflowDefinitionService workflowDefinitionService,
            ISysWorkflowEngineService workflowEngineService,
            SecurityUserResolver securityUserResolver,
            ISysUserService userService) {
        this.workflowDefinitionService = workflowDefinitionService;
        this.workflowEngineService = workflowEngineService;
        this.securityUserResolver = securityUserResolver;
        this.userService = userService;
    }

    /**
     * 查询流程定义列表。
     *
     * @param processName 流程名称关键字
     * @param processKey  流程标识关键字
     * @param category    流程分类
     * @param status      状态
     * @return 流程定义列表
     */
    @GetMapping("/definition/list")
    @PreAuthorize("@ss.hasPermi('system:workflow:list')")
    public R<List<SysWorkflowDefinition>> definitionList(
            @RequestParam(value = "processName", required = false) String processName,
            @RequestParam(value = "processKey", required = false) String processKey,
            @RequestParam(value = "category", required = false) String category,
            @RequestParam(value = "status", required = false) String status) {
        return R.success(workflowDefinitionService.selectList(processName, processKey, category, status));
    }

    /**
     * 查询流程定义详情。
     *
     * @param definitionId 流程定义ID
     * @return 流程定义详情
     */
    @GetMapping("/definition/{definitionId}")
    @PreAuthorize("@ss.hasPermi('system:workflow:query')")
    public R<SysWorkflowDefinition> definitionDetail(@PathVariable("definitionId") Long definitionId) {
        return R.success(workflowDefinitionService.getById(definitionId));
    }

    /**
     * 新增流程定义。
     *
     * @param definition 流程定义
     * @return 新增结果
     */
    @PostMapping("/definition")
    @PreAuthorize("@ss.hasPermi('system:workflow:add')")
    public R<Boolean> addDefinition(@RequestBody SysWorkflowDefinition definition) {
        String operator = resolveCurrentUsername();
        if (!StringUtils.hasText(operator)) {
            return R.failed(ResultCode.UNAUTHORIZED);
        }
        boolean success = workflowDefinitionService.createDefinition(definition, operator);
        return success ? R.success(true) : R.failed("新增流程定义失败，请检查流程标识与版本配置");
    }

    /**
     * 修改流程定义。
     *
     * @param definition 流程定义
     * @return 修改结果
     */
    @PutMapping("/definition")
    @PreAuthorize("@ss.hasPermi('system:workflow:edit')")
    public R<Boolean> editDefinition(@RequestBody SysWorkflowDefinition definition) {
        String operator = resolveCurrentUsername();
        if (!StringUtils.hasText(operator)) {
            return R.failed(ResultCode.UNAUTHORIZED);
        }
        boolean success = workflowDefinitionService.updateDefinition(definition, operator);
        return success ? R.success(true) : R.failed("修改流程定义失败");
    }

    /**
     * 发布流程定义。
     *
     * @param definitionId 流程定义ID
     * @return 发布结果
     */
    @PostMapping("/definition/publish/{definitionId}")
    @PreAuthorize("@ss.hasPermi('system:workflow:publish')")
    public R<Boolean> publishDefinition(@PathVariable("definitionId") Long definitionId) {
        String operator = resolveCurrentUsername();
        if (!StringUtils.hasText(operator)) {
            return R.failed(ResultCode.UNAUTHORIZED);
        }
        boolean success = workflowDefinitionService.publishDefinition(definitionId, operator);
        return success ? R.success(true) : R.failed("流程发布失败");
    }

    /**
     * 停用流程定义。
     *
     * @param definitionId 流程定义ID
     * @return 停用结果
     */
    @PostMapping("/definition/disable/{definitionId}")
    @PreAuthorize("@ss.hasPermi('system:workflow:publish')")
    public R<Boolean> disableDefinition(@PathVariable("definitionId") Long definitionId) {
        String operator = resolveCurrentUsername();
        if (!StringUtils.hasText(operator)) {
            return R.failed(ResultCode.UNAUTHORIZED);
        }
        boolean success = workflowDefinitionService.disableDefinition(definitionId, operator);
        return success ? R.success(true) : R.failed("流程停用失败");
    }

    /**
     * 查询流程定义版本历史。
     *
     * @param processKey 流程标识
     * @return 版本历史列表
     */
    @GetMapping("/definition/history/{processKey}")
    @PreAuthorize("@ss.hasPermi('system:workflow:query')")
    public R<List<SysWorkflowDefinition>> definitionHistory(@PathVariable("processKey") String processKey) {
        return R.success(workflowDefinitionService.selectHistoryByProcessKey(processKey));
    }

    /**
     * 从已有流程定义创建新版本草稿。
     *
     * @param definitionId 来源流程定义ID
     * @return 新版本草稿
     */
    @PostMapping("/definition/version/{definitionId}")
    @PreAuthorize("@ss.hasPermi('system:workflow:edit')")
    public R<SysWorkflowDefinition> createDefinitionVersion(@PathVariable("definitionId") Long definitionId) {
        String operator = resolveCurrentUsername();
        if (!StringUtils.hasText(operator)) {
            return R.failed(ResultCode.UNAUTHORIZED);
        }
        SysWorkflowDefinition newVersion = workflowDefinitionService.createNewVersion(definitionId, operator);
        if (newVersion == null) {
            return R.failed("创建新版本失败，请确认流程定义存在且具备可复制数据");
        }
        return R.success(newVersion);
    }

    /**
     * 删除流程定义。
     *
     * @param definitionIds 流程定义ID集合
     * @return 删除结果
     */
    @DeleteMapping("/definition/{definitionIds}")
    @PreAuthorize("@ss.hasPermi('system:workflow:remove')")
    public R<Boolean> removeDefinition(@PathVariable("definitionIds") List<Long> definitionIds) {
        String operator = resolveCurrentUsername();
        if (!StringUtils.hasText(operator)) {
            return R.failed(ResultCode.UNAUTHORIZED);
        }
        boolean success = workflowDefinitionService.removeDefinitions(definitionIds, operator);
        return success ? R.success(true) : R.failed("流程定义删除失败，已发布版本或已产生实例的版本不可删除");
    }

    /**
     * 查询流程实例列表。
     *
     * @param processKey 流程标识关键字
     * @param status     状态
     * @param businessNo 业务单号关键字
     * @return 流程实例列表
     */
    @GetMapping("/instance/list")
    @PreAuthorize("@ss.hasPermi('system:workflow:list')")
    public R<List<SysWorkflowInstance>> instanceList(
            @RequestParam(value = "processKey", required = false) String processKey,
            @RequestParam(value = "status", required = false) String status,
            @RequestParam(value = "businessNo", required = false) String businessNo) {
        return R.success(workflowEngineService.selectInstanceList(processKey, status, businessNo));
    }

    /**
     * 查询流程实例详情。
     *
     * @param instanceId 流程实例ID
     * @return 流程实例详情
     */
    @GetMapping("/instance/{instanceId}")
    @PreAuthorize("@ss.hasPermi('system:workflow:query')")
    public R<WorkflowInstanceDetailVO> instanceDetail(@PathVariable("instanceId") Long instanceId) {
        return R.success(workflowEngineService.selectInstanceDetail(instanceId));
    }

    /**
     * 发起流程实例。
     *
     * @param startBody 发起参数
     * @return 发起结果
     */
    @PostMapping("/instance/start")
    @PreAuthorize("@ss.hasPermi('system:workflow:start')")
    public R<Boolean> startInstance(@RequestBody WorkflowStartBody startBody) {
        CurrentUser currentUser = resolveCurrentUser();
        if (currentUser.userId == null || !StringUtils.hasText(currentUser.userName)) {
            return R.failed(ResultCode.UNAUTHORIZED);
        }
        boolean success = workflowEngineService.startProcess(
                startBody,
                currentUser.userId,
                currentUser.userName,
                currentUser.nickName);
        return success ? R.success(true) : R.failed("流程发起失败，请检查流程定义发布状态和审批人配置");
    }

    /**
     * 撤回流程实例。
     *
     * @param instanceId  流程实例ID
     * @param actionBody  撤回参数
     * @return 撤回结果
     */
    @PostMapping("/instance/withdraw/{instanceId}")
    @PreAuthorize("@ss.hasPermi('system:workflow:withdraw')")
    public R<Boolean> withdrawInstance(@PathVariable("instanceId") Long instanceId, @RequestBody(required = false) WorkflowTaskActionBody actionBody) {
        CurrentUser currentUser = resolveCurrentUser();
        if (currentUser.userId == null || !StringUtils.hasText(currentUser.userName)) {
            return R.failed(ResultCode.UNAUTHORIZED);
        }
        boolean success = workflowEngineService.withdrawInstance(
                instanceId,
                actionBody,
                currentUser.userId,
                currentUser.userName,
                currentUser.nickName);
        return success ? R.success(true) : R.failed("流程撤回失败");
    }

    /**
     * 查询当前用户流程任务列表。
     *
     * @param status 状态
     * @return 任务列表
     */
    @GetMapping("/task/list")
    @PreAuthorize("@ss.hasPermi('system:workflow:list')")
    public R<List<SysWorkflowTask>> taskList(@RequestParam(value = "status", required = false) String status) {
        Long currentUserId = securityUserResolver.getCurrentUserId();
        if (currentUserId == null) {
            return R.failed(ResultCode.UNAUTHORIZED);
        }
        return R.success(workflowEngineService.selectMyTaskList(currentUserId, status));
    }

    /**
     * 查询任务节点动态表单。
     *
     * @param taskId 任务ID
     * @return 动态表单
     */
    @GetMapping("/task/form/{taskId}")
    @PreAuthorize("@ss.hasPermi('system:workflow:form')")
    public R<WorkflowTaskFormVO> taskForm(@PathVariable("taskId") Long taskId) {
        Long currentUserId = securityUserResolver.getCurrentUserId();
        if (currentUserId == null) {
            return R.failed(ResultCode.UNAUTHORIZED);
        }
        WorkflowTaskFormVO formVO = workflowEngineService.selectTaskForm(taskId, currentUserId);
        if (formVO == null) {
            return R.failed("任务表单不存在或无权限查看");
        }
        return R.success(formVO);
    }

    /**
     * 同意审批任务。
     *
     * @param taskId      任务ID
     * @param actionBody  审批参数
     * @return 处理结果
     */
    @PostMapping("/task/approve/{taskId}")
    @PreAuthorize("@ss.hasPermi('system:workflow:handle')")
    public R<Boolean> approveTask(@PathVariable("taskId") Long taskId, @RequestBody(required = false) WorkflowTaskActionBody actionBody) {
        CurrentUser currentUser = resolveCurrentUser();
        if (currentUser.userId == null || !StringUtils.hasText(currentUser.userName)) {
            return R.failed(ResultCode.UNAUTHORIZED);
        }
        boolean success = workflowEngineService.approveTask(taskId, actionBody, currentUser.userId, currentUser.userName, currentUser.nickName);
        return success ? R.success(true) : R.failed("审批通过失败");
    }

    /**
     * 驳回审批任务。
     *
     * @param taskId      任务ID
     * @param actionBody  审批参数
     * @return 处理结果
     */
    @PostMapping("/task/reject/{taskId}")
    @PreAuthorize("@ss.hasPermi('system:workflow:handle')")
    public R<Boolean> rejectTask(@PathVariable("taskId") Long taskId, @RequestBody(required = false) WorkflowTaskActionBody actionBody) {
        CurrentUser currentUser = resolveCurrentUser();
        if (currentUser.userId == null || !StringUtils.hasText(currentUser.userName)) {
            return R.failed(ResultCode.UNAUTHORIZED);
        }
        boolean success = workflowEngineService.rejectTask(taskId, actionBody, currentUser.userId, currentUser.userName, currentUser.nickName);
        return success ? R.success(true) : R.failed("审批驳回失败");
    }

    /**
     * 转交审批任务。
     *
     * @param taskId       任务ID
     * @param transferBody 转交参数
     * @return 处理结果
     */
    @PostMapping("/task/transfer/{taskId}")
    @PreAuthorize("@ss.hasPermi('system:workflow:handle')")
    public R<Boolean> transferTask(@PathVariable("taskId") Long taskId, @RequestBody WorkflowTaskTransferBody transferBody) {
        CurrentUser currentUser = resolveCurrentUser();
        if (currentUser.userId == null || !StringUtils.hasText(currentUser.userName)) {
            return R.failed(ResultCode.UNAUTHORIZED);
        }
        boolean success = workflowEngineService.transferTask(taskId, transferBody, currentUser.userId, currentUser.userName, currentUser.nickName);
        return success ? R.success(true) : R.failed("任务转交失败");
    }

    /**
     * 退回流程任务到指定节点。
     *
     * @param taskId      任务ID
     * @param returnBody  退回参数
     * @return 处理结果
     */
    @PostMapping("/task/return/{taskId}")
    @PreAuthorize("@ss.hasPermi('system:workflow:return')")
    public R<Boolean> returnTask(@PathVariable("taskId") Long taskId, @RequestBody WorkflowTaskReturnBody returnBody) {
        CurrentUser currentUser = resolveCurrentUser();
        if (currentUser.userId == null || !StringUtils.hasText(currentUser.userName)) {
            return R.failed(ResultCode.UNAUTHORIZED);
        }
        boolean success = workflowEngineService.returnTask(taskId, returnBody, currentUser.userId, currentUser.userName, currentUser.nickName);
        return success ? R.success(true) : R.failed("任务退回失败");
    }

    /**
     * 加签。
     *
     * @param taskId       任务ID
     * @param transferBody 加签参数
     * @return 处理结果
     */
    @PostMapping("/task/addSign/{taskId}")
    @PreAuthorize("@ss.hasPermi('system:workflow:addSign')")
    public R<Boolean> addSign(@PathVariable("taskId") Long taskId, @RequestBody WorkflowTaskTransferBody transferBody) {
        CurrentUser currentUser = resolveCurrentUser();
        if (currentUser.userId == null || !StringUtils.hasText(currentUser.userName)) {
            return R.failed(ResultCode.UNAUTHORIZED);
        }
        boolean success = workflowEngineService.addSign(taskId, transferBody, currentUser.userId, currentUser.userName, currentUser.nickName);
        return success ? R.success(true) : R.failed("任务加签失败");
    }

    /**
     * 减签。
     *
     * @param taskId       任务ID
     * @param transferBody 减签参数
     * @return 处理结果
     */
    @PostMapping("/task/removeSign/{taskId}")
    @PreAuthorize("@ss.hasPermi('system:workflow:removeSign')")
    public R<Boolean> removeSign(@PathVariable("taskId") Long taskId, @RequestBody WorkflowTaskTransferBody transferBody) {
        CurrentUser currentUser = resolveCurrentUser();
        if (currentUser.userId == null || !StringUtils.hasText(currentUser.userName)) {
            return R.failed(ResultCode.UNAUTHORIZED);
        }
        boolean success = workflowEngineService.removeSign(taskId, transferBody, currentUser.userId, currentUser.userName, currentUser.nickName);
        return success ? R.success(true) : R.failed("任务减签失败");
    }

    /**
     * 委派任务。
     *
     * @param taskId       任务ID
     * @param transferBody 委派参数
     * @return 处理结果
     */
    @PostMapping("/task/delegate/{taskId}")
    @PreAuthorize("@ss.hasPermi('system:workflow:delegate')")
    public R<Boolean> delegateTask(@PathVariable("taskId") Long taskId, @RequestBody WorkflowTaskTransferBody transferBody) {
        CurrentUser currentUser = resolveCurrentUser();
        if (currentUser.userId == null || !StringUtils.hasText(currentUser.userName)) {
            return R.failed(ResultCode.UNAUTHORIZED);
        }
        boolean success = workflowEngineService.delegateTask(taskId, transferBody, currentUser.userId, currentUser.userName, currentUser.nickName);
        return success ? R.success(true) : R.failed("任务委派失败");
    }

    /**
     * 催办任务。
     *
     * @param taskId      任务ID
     * @param remindBody  催办参数
     * @return 处理结果
     */
    @PostMapping("/task/remind/{taskId}")
    @PreAuthorize("@ss.hasPermi('system:workflow:remind')")
    public R<Boolean> remindTask(@PathVariable("taskId") Long taskId, @RequestBody(required = false) WorkflowTaskRemindBody remindBody) {
        CurrentUser currentUser = resolveCurrentUser();
        if (currentUser.userId == null || !StringUtils.hasText(currentUser.userName)) {
            return R.failed(ResultCode.UNAUTHORIZED);
        }
        boolean success = workflowEngineService.remindTask(taskId, remindBody, currentUser.userId, currentUser.userName, currentUser.nickName);
        return success ? R.success(true) : R.failed("任务催办失败");
    }

    /**
     * 解析当前登录用户名。
     *
     * @return 当前用户名
     */
    private String resolveCurrentUsername() {
        return securityUserResolver.getCurrentUsername();
    }

    /**
     * 解析当前登录用户上下文。
     *
     * @return 当前用户上下文
     */
    private CurrentUser resolveCurrentUser() {
        String userName = resolveCurrentUsername();
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
         * 构造当前用户结构对象。
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

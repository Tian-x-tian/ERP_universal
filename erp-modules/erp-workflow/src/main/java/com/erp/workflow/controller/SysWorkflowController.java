package com.erp.workflow.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.erp.common.core.domain.R;
import com.erp.common.core.domain.ResultCode;
import com.erp.workflow.domain.platform.SysDept;
import com.erp.workflow.domain.platform.SysPost;
import com.erp.workflow.domain.platform.SysRole;
import com.erp.workflow.domain.platform.SysUser;
import com.erp.workflow.contract.domain.SysWorkflowDefinition;
import com.erp.workflow.contract.domain.SysWorkflowInstance;
import com.erp.workflow.contract.domain.SysWorkflowTask;
import com.erp.workflow.contract.domain.vo.WorkflowDashboardQueryVO;
import com.erp.workflow.contract.domain.vo.WorkflowInstanceDetailVO;
import com.erp.workflow.contract.domain.vo.WorkflowDashboardVO;
import com.erp.workflow.contract.domain.vo.WorkflowParticipantOptionVO;
import com.erp.workflow.contract.domain.vo.WorkflowParticipantOptionsVO;
import com.erp.workflow.contract.domain.vo.WorkflowSlaScanResultVO;
import com.erp.workflow.contract.domain.vo.WorkflowStartBody;
import com.erp.workflow.contract.domain.vo.WorkflowTaskActionBody;
import com.erp.workflow.contract.domain.vo.WorkflowTaskFormVO;
import com.erp.workflow.contract.domain.vo.WorkflowTaskRemindBody;
import com.erp.workflow.contract.domain.vo.WorkflowTaskReturnBody;
import com.erp.workflow.contract.domain.vo.WorkflowTaskTransferBody;
import com.erp.workflow.contract.domain.vo.WorkflowTemplateActivateBody;
import com.erp.workflow.contract.domain.vo.WorkflowTemplateVO;
import com.erp.workflow.security.service.SecurityUserResolver;
import com.erp.workflow.service.ISysDeptService;
import com.erp.workflow.service.ISysPostService;
import com.erp.workflow.service.ISysRoleService;
import com.erp.workflow.service.ISysUserService;
import com.erp.workflow.service.ISysWorkflowAnalyticsService;
import com.erp.workflow.service.ISysWorkflowDefinitionService;
import com.erp.workflow.service.ISysWorkflowEngineService;
import com.erp.workflow.service.ISysWorkflowTemplateService;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 流程引擎控制层
 */
@RestController
@ConditionalOnProperty(name = "erp.workflow.http-enabled", havingValue = "true")
@RequestMapping("/workflow")
public class SysWorkflowController {

    private final ISysWorkflowDefinitionService workflowDefinitionService;
    private final ISysWorkflowEngineService workflowEngineService;
    private final ISysWorkflowAnalyticsService workflowAnalyticsService;
    private final ISysWorkflowTemplateService workflowTemplateService;
    private final SecurityUserResolver securityUserResolver;
    private final ISysUserService userService;
    private final ISysDeptService deptService;
    private final ISysRoleService roleService;
    private final ISysPostService postService;

    public SysWorkflowController(
            ISysWorkflowDefinitionService workflowDefinitionService,
            ISysWorkflowEngineService workflowEngineService,
            ISysWorkflowAnalyticsService workflowAnalyticsService,
            ISysWorkflowTemplateService workflowTemplateService,
            SecurityUserResolver securityUserResolver,
            ISysUserService userService,
            ISysDeptService deptService,
            ISysRoleService roleService,
            ISysPostService postService) {
        this.workflowDefinitionService = workflowDefinitionService;
        this.workflowEngineService = workflowEngineService;
        this.workflowAnalyticsService = workflowAnalyticsService;
        this.workflowTemplateService = workflowTemplateService;
        this.securityUserResolver = securityUserResolver;
        this.userService = userService;
        this.deptService = deptService;
        this.roleService = roleService;
        this.postService = postService;
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
    @GetMapping("/definitions/list")
    @PreAuthorize("@ss.hasPermi('workflow:definition:list')")
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
    @GetMapping("/definitions/{definitionId}")
    @PreAuthorize("@ss.hasPermi('workflow:definition:query')")
    public R<SysWorkflowDefinition> definitionDetail(@PathVariable("definitionId") Long definitionId) {
        return R.success(workflowDefinitionService.getById(definitionId));
    }

    /**
     * 新增流程定义。
     *
     * @param definition 流程定义
     * @return 新增结果
     */
    @PostMapping("/definitions")
    @PreAuthorize("@ss.hasAnyPermi('workflow:definition:add','workflow:definition:design')")
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
    @PutMapping("/definitions")
    @PreAuthorize("@ss.hasAnyPermi('workflow:definition:edit','workflow:definition:design')")
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
    @PostMapping("/definitions/publish/{definitionId}")
    @PreAuthorize("@ss.hasPermi('workflow:definition:publish')")
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
    @PostMapping("/definitions/disable/{definitionId}")
    @PreAuthorize("@ss.hasPermi('workflow:definition:publish')")
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
    @GetMapping("/definitions/history/{processKey}")
    @PreAuthorize("@ss.hasPermi('workflow:definition:query')")
    public R<List<SysWorkflowDefinition>> definitionHistory(@PathVariable("processKey") String processKey) {
        return R.success(workflowDefinitionService.selectHistoryByProcessKey(processKey));
    }

    /**
     * 从已有流程定义创建新版本草稿。
     *
     * @param definitionId 来源流程定义ID
     * @return 新版本草稿
     */
    @PostMapping("/definitions/version/{definitionId}")
    @PreAuthorize("@ss.hasAnyPermi('workflow:definition:edit','workflow:definition:design')")
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
    @DeleteMapping("/definitions/{definitionIds}")
    @PreAuthorize("@ss.hasPermi('workflow:definition:remove')")
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
    @GetMapping("/instances/list")
    @PreAuthorize("@ss.hasPermi('workflow:instance:list')")
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
    @GetMapping("/instances/{instanceId}")
    @PreAuthorize("@ss.hasPermi('workflow:instance:query')")
    public R<WorkflowInstanceDetailVO> instanceDetail(@PathVariable("instanceId") Long instanceId) {
        return R.success(workflowEngineService.selectInstanceDetail(instanceId));
    }

    /**
     * 发起流程实例。
     *
     * @param startBody 发起参数
     * @return 发起结果
     */
    @PostMapping("/instances/start")
    @PreAuthorize("@ss.hasPermi('workflow:instance:start')")
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
        return success ? R.success(true) : R.failed("流程发起失败，请检查流程定义发布状态、流程模型与审批人配置");
    }

    /**
     * 撤回流程实例。
     *
     * @param instanceId  流程实例ID
     * @param actionBody  撤回参数
     * @return 撤回结果
     */
    @PostMapping("/instances/withdraw/{instanceId}")
    @PreAuthorize("@ss.hasPermi('workflow:instance:withdraw')")
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
    @GetMapping("/tasks/list")
    @PreAuthorize("@ss.hasPermi('workflow:todo:list')")
    public R<List<SysWorkflowTask>> taskList(@RequestParam(value = "status", required = false) String status) {
        Long currentUserId = securityUserResolver.getCurrentUserId();
        if (currentUserId == null) {
            return R.failed(ResultCode.UNAUTHORIZED);
        }
        return R.success(workflowEngineService.selectMyTaskList(currentUserId, status));
    }

    /**
     * 查询流程效率看板。
     *
     * @param queryVO 查询条件
     * @return 看板结果
     */
    @GetMapping("/reports/dashboard")
    @PreAuthorize("@ss.hasPermi('workflow:instance:report')")
    public R<WorkflowDashboardVO> reportDashboard(WorkflowDashboardQueryVO queryVO) {
        return R.success(workflowAnalyticsService.buildDashboard(queryVO));
    }

    /**
     * 查询流程设计器参与人配置选项。
     *
     * @return 用户、部门、角色、岗位选项
     */
    @GetMapping("/bindings/participants")
    @PreAuthorize("@ss.hasAnyPermi('workflow:definition:query','workflow:definition:design')")
    public R<WorkflowParticipantOptionsVO> participantOptions() {
        Long currentUserId = securityUserResolver.getCurrentUserId();
        if (currentUserId == null) {
            return R.failed(ResultCode.UNAUTHORIZED);
        }
        WorkflowParticipantOptionsVO optionsVO = new WorkflowParticipantOptionsVO();
        List<SysDept> deptList = loadDeptOptions();
        List<SysUser> userList = loadUserOptions();
        List<SysRole> roleList = roleService.list();
        List<SysPost> postList = postService.list();
        optionsVO.setDepts(buildDeptOptions(deptList));
        optionsVO.setUsers(buildUserOptions(userList));
        optionsVO.setRoles(buildRoleOptions(roleList));
        optionsVO.setPosts(buildPostOptions(postList));
        return R.success(optionsVO);
    }

    /**
     * 手动执行 SLA 扫描。
     *
     * @return 扫描结果
     */
    @PostMapping("/reports/sla/scan")
    @PreAuthorize("@ss.hasPermi('workflow:instance:sla')")
    public R<WorkflowSlaScanResultVO> scanWorkflowSla() {
        return R.success(workflowEngineService.scanTimeoutTasks());
    }

    /**
     * 查询流程模板市场。
     *
     * @param industry 行业筛选
     * @return 模板列表
     */
    @GetMapping("/templates/list")
    @PreAuthorize("@ss.hasAnyPermi('workflow:definition:template','workflow:definition:list')")
    public R<List<WorkflowTemplateVO>> templateList(@RequestParam(value = "industry", required = false) String industry) {
        return R.success(workflowTemplateService.selectTemplateList(industry));
    }

    /**
     * 启用流程模板。
     *
     * @param templateCode 模板编码
     * @param activateBody 启用参数
     * @return 新建流程定义
     */
    @PostMapping("/templates/activate/{templateCode}")
    @PreAuthorize("@ss.hasAnyPermi('workflow:definition:template','workflow:definition:add','workflow:definition:design')")
    public R<SysWorkflowDefinition> activateTemplate(@PathVariable("templateCode") String templateCode,
                                                     @RequestBody(required = false) WorkflowTemplateActivateBody activateBody) {
        String operator = resolveCurrentUsername();
        if (!StringUtils.hasText(operator)) {
            return R.failed(ResultCode.UNAUTHORIZED);
        }
        SysWorkflowDefinition definition = workflowTemplateService.activateTemplate(templateCode, activateBody, operator);
        if (definition == null) {
            return R.failed("模板启用失败，请检查模板编码或流程标识是否重复");
        }
        return R.success(definition);
    }

    /**
     * 查询任务节点动态表单。
     *
     * @param taskId 任务ID
     * @return 动态表单
     */
    @GetMapping("/tasks/form/{taskId}")
    @PreAuthorize("@ss.hasAnyPermi('workflow:todo:form','workflow:todo:handle')")
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
    @PostMapping("/tasks/approve/{taskId}")
    @PreAuthorize("@ss.hasAnyPermi('workflow:todo:approve','workflow:todo:handle')")
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
    @PostMapping("/tasks/reject/{taskId}")
    @PreAuthorize("@ss.hasAnyPermi('workflow:todo:reject','workflow:todo:handle')")
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
    @PostMapping("/tasks/transfer/{taskId}")
    @PreAuthorize("@ss.hasAnyPermi('workflow:todo:transfer','workflow:todo:handle')")
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
    @PostMapping("/tasks/return/{taskId}")
    @PreAuthorize("@ss.hasAnyPermi('workflow:todo:return','workflow:todo:handle')")
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
    @PostMapping("/tasks/addSign/{taskId}")
    @PreAuthorize("@ss.hasAnyPermi('workflow:todo:addSign','workflow:todo:handle')")
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
    @PostMapping("/tasks/removeSign/{taskId}")
    @PreAuthorize("@ss.hasAnyPermi('workflow:todo:removeSign','workflow:todo:handle')")
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
    @PostMapping("/tasks/delegate/{taskId}")
    @PreAuthorize("@ss.hasAnyPermi('workflow:todo:delegate','workflow:todo:handle')")
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
    @PostMapping("/tasks/remind/{taskId}")
    @PreAuthorize("@ss.hasAnyPermi('workflow:todo:remind','workflow:todo:handle')")
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
     * 加载当前数据权限范围下的部门选项。
     *
     * @return 部门列表
     */
    private List<SysDept> loadDeptOptions() {
        return deptService.list();
    }

    /**
     * 加载当前数据权限范围下的用户选项。
     *
     * @return 用户列表
     */
    private List<SysUser> loadUserOptions() {
        return userService.list();
    }

    /**
     * 构建部门选项。
     *
     * @param deptList 部门列表
     * @return 选项列表
     */
    private List<WorkflowParticipantOptionVO> buildDeptOptions(List<SysDept> deptList) {
        List<WorkflowParticipantOptionVO> optionList = new ArrayList<>();
        if (deptList == null) {
            return optionList;
        }
        for (SysDept dept : deptList) {
            if (dept == null || dept.getDeptId() == null || "1".equals(dept.getStatus())) {
                continue;
            }
            WorkflowParticipantOptionVO optionVO = new WorkflowParticipantOptionVO();
            optionVO.setValue(dept.getDeptId());
            optionVO.setLabel(dept.getDeptName());
            optionVO.setParentId(dept.getParentId());
            optionList.add(optionVO);
        }
        return optionList;
    }

    /**
     * 构建用户选项。
     *
     * @param userList 用户列表
     * @return 选项列表
     */
    private List<WorkflowParticipantOptionVO> buildUserOptions(List<SysUser> userList) {
        List<WorkflowParticipantOptionVO> optionList = new ArrayList<>();
        if (userList == null) {
            return optionList;
        }
        for (SysUser user : userList) {
            if (user == null || user.getUserId() == null || "1".equals(user.getStatus()) || "2".equals(user.getDelFlag())) {
                continue;
            }
            WorkflowParticipantOptionVO optionVO = new WorkflowParticipantOptionVO();
            optionVO.setValue(user.getUserId());
            optionVO.setLabel(StringUtils.hasText(user.getNickName())
                    ? user.getNickName() + " (" + user.getUserName() + ")"
                    : user.getUserName());
            optionVO.setParentId(user.getDeptId());
            optionList.add(optionVO);
        }
        return optionList;
    }

    /**
     * 构建角色选项。
     *
     * @param roleList 角色列表
     * @return 选项列表
     */
    private List<WorkflowParticipantOptionVO> buildRoleOptions(List<SysRole> roleList) {
        List<WorkflowParticipantOptionVO> optionList = new ArrayList<>();
        if (roleList == null) {
            return optionList;
        }
        for (SysRole role : roleList) {
            if (role == null || role.getRoleId() == null || "1".equals(role.getStatus())) {
                continue;
            }
            WorkflowParticipantOptionVO optionVO = new WorkflowParticipantOptionVO();
            optionVO.setValue(role.getRoleId());
            optionVO.setLabel(role.getRoleName());
            optionList.add(optionVO);
        }
        return optionList;
    }

    /**
     * 构建岗位选项。
     *
     * @param postList 岗位列表
     * @return 选项列表
     */
    private List<WorkflowParticipantOptionVO> buildPostOptions(List<SysPost> postList) {
        List<WorkflowParticipantOptionVO> optionList = new ArrayList<>();
        if (postList == null) {
            return optionList;
        }
        for (SysPost post : postList) {
            if (post == null || post.getPostId() == null || "1".equals(post.getStatus())) {
                continue;
            }
            WorkflowParticipantOptionVO optionVO = new WorkflowParticipantOptionVO();
            optionVO.setValue(post.getPostId());
            optionVO.setLabel(post.getPostName());
            optionList.add(optionVO);
        }
        return optionList;
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





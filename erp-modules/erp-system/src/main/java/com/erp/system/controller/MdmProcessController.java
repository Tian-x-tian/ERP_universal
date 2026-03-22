package com.erp.system.controller;

import com.erp.common.core.domain.R;
import com.erp.common.core.domain.ResultCode;
import com.erp.workflow.contract.domain.vo.WorkflowProcessOptionVO;
import com.erp.workflow.contract.domain.vo.WorkflowInstanceDetailVO;
import com.erp.workflow.contract.domain.vo.WorkflowTaskActionBody;
import com.erp.system.security.service.SecurityUserResolver;
import com.erp.system.service.ISysWorkflowDefinitionService;
import com.erp.system.service.ISysWorkflowEngineService;
import com.erp.system.service.IWorkflowBindingResolver;
import com.erp.system.support.MdmDomainTypeSupport;
import com.erp.system.support.MdmWorkflowBusinessSupport;
import com.erp.workflow.contract.support.WorkflowBindingActionSupport;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * MDM 流程包装控制层。
 * 负责将 MDM 域对象与工作流实例、审批动作统一衔接。
 */
@RestController
@RequestMapping("/system/mdm/process")
public class MdmProcessController {

    private final ISysWorkflowEngineService workflowEngineService;
    private final ISysWorkflowDefinitionService workflowDefinitionService;
    private final IWorkflowBindingResolver workflowBindingResolver;
    private final SecurityUserResolver securityUserResolver;

    public MdmProcessController(ISysWorkflowEngineService workflowEngineService,
                                ISysWorkflowDefinitionService workflowDefinitionService,
                                IWorkflowBindingResolver workflowBindingResolver,
                                SecurityUserResolver securityUserResolver) {
        this.workflowEngineService = workflowEngineService;
        this.workflowDefinitionService = workflowDefinitionService;
        this.workflowBindingResolver = workflowBindingResolver;
        this.securityUserResolver = securityUserResolver;
    }

    /**
     * 查询员工审批动作可选流程。
     *
     * @param action 审批动作（ONBOARD/CHANGE/LEAVE）
     * @return 流程选项列表
     */
    @GetMapping("/options/employee")
    @PreAuthorize("@ss.hasAnyPermi('system:mdm:employee:query','system:mdm:employee:edit','business:hr:employee:list','business:hr:employee:submit','business:hr:employee:leave')")
    public R<List<WorkflowProcessOptionVO>> employeeProcessOptions(@RequestParam("action") String action) {
        String normalizedAction = WorkflowBindingActionSupport.normalizeAction(action);
        if (!WorkflowBindingActionSupport.isSupportedAction(normalizedAction)) {
            return R.failed(ResultCode.PARAM_ERROR, "审批动作不合法，仅支持 ONBOARD/CHANGE/LEAVE");
        }
        List<WorkflowProcessOptionVO> optionList = workflowBindingResolver.listProcessOptions(
                MdmDomainTypeSupport.EMPLOYEE,
                normalizedAction);
        return R.success(optionList);
    }

    /**
     * 查询指定 MDM 业务的最新流程实例详情。
     *
     * @param domainType MDM 域类型
     * @param bizId      业务主键
     * @return 流程实例详情
     */
    @GetMapping("/{domainType}/{bizId}")
    @PreAuthorize("@ss.hasPermi('workflow:instance:query')")
    public R<WorkflowInstanceDetailVO> processDetail(@PathVariable("domainType") String domainType,
                                                     @PathVariable("bizId") Long bizId) {
        String businessType = MdmWorkflowBusinessSupport.resolveBusinessType(domainType);
        String businessNo = MdmWorkflowBusinessSupport.buildBusinessNo(domainType, bizId);
        if (!StringUtils.hasText(businessType) || !StringUtils.hasText(businessNo)) {
            return R.failed(ResultCode.PARAM_ERROR, "MDM 域类型或业务主键不合法");
        }
        return R.success(workflowEngineService.selectLatestInstanceDetail(businessType, businessNo));
    }

    /**
     * 审批通过当前 MDM 流程任务。
     *
     * @param taskId     任务ID
     * @param actionBody 审批参数
     * @return 审批结果
     */
    @PostMapping("/approve/{taskId}")
    @PreAuthorize("@ss.hasAnyPermi('workflow:todo:approve','workflow:todo:handle')")
    public R<Boolean> approve(@PathVariable("taskId") Long taskId,
                              @RequestBody(required = false) WorkflowTaskActionBody actionBody) {
        CurrentUser currentUser = resolveCurrentUser();
        if (currentUser.userId == null || !StringUtils.hasText(currentUser.userName)) {
            return R.failed(ResultCode.UNAUTHORIZED);
        }
        boolean success = workflowEngineService.approveTask(
                taskId,
                actionBody,
                currentUser.userId,
                currentUser.userName,
                currentUser.nickName);
        return success ? R.success(true) : R.failed("MDM 审批通过失败");
    }

    /**
     * 驳回当前 MDM 流程任务。
     *
     * @param taskId     任务ID
     * @param actionBody 审批参数
     * @return 处理结果
     */
    @PostMapping("/reject/{taskId}")
    @PreAuthorize("@ss.hasAnyPermi('workflow:todo:reject','workflow:todo:handle')")
    public R<Boolean> reject(@PathVariable("taskId") Long taskId,
                             @RequestBody(required = false) WorkflowTaskActionBody actionBody) {
        CurrentUser currentUser = resolveCurrentUser();
        if (currentUser.userId == null || !StringUtils.hasText(currentUser.userName)) {
            return R.failed(ResultCode.UNAUTHORIZED);
        }
        boolean success = workflowEngineService.rejectTask(
                taskId,
                actionBody,
                currentUser.userId,
                currentUser.userName,
                currentUser.nickName);
        return success ? R.success(true) : R.failed("MDM 审批驳回失败");
    }

    /**
     * 发布指定流程定义，使其可用于 MDM 提交审批。
     *
     * @param definitionId 流程定义ID
     * @return 发布结果
     */
    @PostMapping("/publish/{definitionId}")
    @PreAuthorize("@ss.hasPermi('workflow:definition:publish')")
    public R<Boolean> publish(@PathVariable("definitionId") Long definitionId) {
        String operator = securityUserResolver.getCurrentUsername();
        if (!StringUtils.hasText(operator)) {
            return R.failed(ResultCode.UNAUTHORIZED);
        }
        boolean success = workflowDefinitionService.publishDefinition(definitionId, operator.trim());
        return success ? R.success(true) : R.failed("MDM 流程发布失败");
    }

    /**
     * 解析当前用户上下文。
     *
     * @return 当前用户
     */
    private CurrentUser resolveCurrentUser() {
        CurrentUser currentUser = new CurrentUser();
        currentUser.userId = securityUserResolver.getCurrentUserId();
        currentUser.userName = securityUserResolver.getCurrentUsername();
        currentUser.nickName = currentUser.userName;
        return currentUser;
    }

    /**
     * 当前登录用户简要信息。
     */
    private static final class CurrentUser {
        private Long userId;
        private String userName;
        private String nickName;
    }
}


package com.erp.system.controller;

import com.erp.common.core.domain.R;
import com.erp.common.core.domain.ResultCode;
import com.erp.system.domain.SysWorkflowInstance;
import com.erp.system.domain.vo.WorkflowInstanceDetailVO;
import com.erp.system.domain.vo.WorkflowTaskActionBody;
import com.erp.system.security.service.SecurityUserResolver;
import com.erp.system.service.ISysWorkflowDefinitionService;
import com.erp.system.service.ISysWorkflowEngineService;
import com.erp.system.support.MdmWorkflowBusinessSupport;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
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
    private final SecurityUserResolver securityUserResolver;

    public MdmProcessController(ISysWorkflowEngineService workflowEngineService,
                                ISysWorkflowDefinitionService workflowDefinitionService,
                                SecurityUserResolver securityUserResolver) {
        this.workflowEngineService = workflowEngineService;
        this.workflowDefinitionService = workflowDefinitionService;
        this.securityUserResolver = securityUserResolver;
    }

    /**
     * 查询指定 MDM 业务的最新流程实例详情。
     *
     * @param domainType MDM 域类型
     * @param bizId      业务主键
     * @return 流程实例详情
     */
    @GetMapping("/{domainType}/{bizId}")
    @PreAuthorize("@ss.hasPermi('system:workflow:query')")
    public R<WorkflowInstanceDetailVO> processDetail(@PathVariable("domainType") String domainType,
                                                     @PathVariable("bizId") Long bizId) {
        String businessType = MdmWorkflowBusinessSupport.resolveBusinessType(domainType);
        String businessNo = MdmWorkflowBusinessSupport.buildBusinessNo(domainType, bizId);
        if (!StringUtils.hasText(businessType) || !StringUtils.hasText(businessNo)) {
            return R.failed(ResultCode.PARAM_ERROR, "MDM 域类型或业务主键不合法");
        }
        List<SysWorkflowInstance> instanceList = workflowEngineService.selectInstanceList(null, null, businessNo);
        SysWorkflowInstance latestInstance = instanceList.stream()
                .filter(instance -> businessType.equalsIgnoreCase(instance.getBusinessType()))
                .findFirst()
                .orElse(null);
        if (latestInstance == null || latestInstance.getInstanceId() == null) {
            return R.success(null);
        }
        return R.success(workflowEngineService.selectInstanceDetail(latestInstance.getInstanceId()));
    }

    /**
     * 审批通过当前 MDM 流程任务。
     *
     * @param taskId     任务ID
     * @param actionBody 审批参数
     * @return 审批结果
     */
    @PostMapping("/approve/{taskId}")
    @PreAuthorize("@ss.hasPermi('system:workflow:handle')")
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
    @PreAuthorize("@ss.hasPermi('system:workflow:handle')")
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
    @PreAuthorize("@ss.hasPermi('system:workflow:publish')")
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

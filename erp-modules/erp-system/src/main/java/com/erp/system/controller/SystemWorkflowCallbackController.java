package com.erp.system.controller;

import com.erp.system.service.IWorkflowBusinessCallback;
import com.erp.workflow.contract.domain.SysWorkflowInstance;
import com.erp.workflow.contract.domain.vo.WorkflowCallbackEvent;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Collections;
import java.util.List;

/**
 * 系统模块工作流终态回调控制层。
 */
@RestController
@RequestMapping("/system/internal/workflow/callbacks")
public class SystemWorkflowCallbackController {
    private static final String INSTANCE_STATUS_COMPLETED = "1";
    private static final String INSTANCE_STATUS_REJECTED = "2";
    private static final String INSTANCE_STATUS_WITHDRAWN = "3";

    private final List<IWorkflowBusinessCallback> callbackList;

    public SystemWorkflowCallbackController(List<IWorkflowBusinessCallback> callbackList) {
        this.callbackList = callbackList == null ? Collections.emptyList() : callbackList;
    }

    /**
     * 处理工作流终态回调事件。
     *
     * @param event 回调事件
     */
    @PostMapping("/terminal")
    public void terminal(@RequestBody WorkflowCallbackEvent event) {
        if (event == null || !StringUtils.hasText(event.getBusinessType())) {
            return;
        }
        SysWorkflowInstance instance = toInstance(event);
        for (IWorkflowBusinessCallback callback : callbackList) {
            if (callback == null || !callback.supports(event.getBusinessType())) {
                continue;
            }
            if (INSTANCE_STATUS_COMPLETED.equals(event.getStatus())) {
                callback.onWorkflowCompleted(instance);
                continue;
            }
            if (INSTANCE_STATUS_REJECTED.equals(event.getStatus())) {
                callback.onWorkflowRejected(instance);
                continue;
            }
            if (INSTANCE_STATUS_WITHDRAWN.equals(event.getStatus())) {
                callback.onWorkflowWithdrawn(instance);
            }
        }
    }

    /**
     * 将回调事件转换为流程实例快照。
     *
     * @param event 回调事件
     * @return 流程实例
     */
    private SysWorkflowInstance toInstance(WorkflowCallbackEvent event) {
        SysWorkflowInstance instance = new SysWorkflowInstance();
        instance.setInstanceId(event.getInstanceId());
        instance.setTenantId(event.getTenantId());
        instance.setOwnerService(event.getOwnerService());
        instance.setProcessKey(event.getProcessKey());
        instance.setProcessName(event.getProcessName());
        instance.setBusinessType(event.getBusinessType());
        instance.setBusinessNo(event.getBusinessNo());
        instance.setDomainType(event.getDomainType());
        instance.setActionCode(event.getActionCode());
        instance.setIdempotencyKey(event.getIdempotencyKey());
        instance.setStatus(event.getStatus());
        instance.setFormData(event.getFormData());
        instance.setLastActionUserName(event.getOperator());
        return instance;
    }
}

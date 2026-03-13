package com.erp.system.service;

import com.erp.system.domain.SysWorkflowInstance;

/**
 * 工作流业务回调接口。
 */
public interface IWorkflowBusinessCallback {

    /**
     * 当前回调是否支持指定业务类型。
     *
     * @param businessType 业务类型
     * @return true 表示支持
     */
    boolean supports(String businessType);

    /**
     * 处理流程审批完成事件。
     *
     * @param instance 流程实例
     */
    void onWorkflowCompleted(SysWorkflowInstance instance);

    /**
     * 处理流程驳回事件。
     *
     * @param instance 流程实例
     */
    void onWorkflowRejected(SysWorkflowInstance instance);

    /**
     * 处理流程撤回事件。
     *
     * @param instance 流程实例
     */
    void onWorkflowWithdrawn(SysWorkflowInstance instance);
}

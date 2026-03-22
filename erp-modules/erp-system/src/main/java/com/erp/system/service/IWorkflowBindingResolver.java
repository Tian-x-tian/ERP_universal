package com.erp.system.service;

import com.erp.workflow.contract.domain.vo.WorkflowProcessOptionVO;

import java.util.List;

/**
 * 系统模块工作流绑定远程门面接口。
 */
public interface IWorkflowBindingResolver {

    /**
     * 查询业务动作可选流程列表。
     *
     * @param domainType 业务域类型
     * @param actionCode 动作编码
     * @return 流程选项列表
     */
    List<WorkflowProcessOptionVO> listProcessOptions(String domainType, String actionCode);

    /**
     * 解析最终流程标识。
     *
     * @param domainType 业务域类型
     * @param actionCode 动作编码
     * @param requestedKey 请求流程标识
     * @return 最终流程标识
     */
    String resolveProcessKey(String domainType, String actionCode, String requestedKey);
}

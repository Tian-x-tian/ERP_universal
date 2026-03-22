package com.erp.workflow.service;

import com.erp.workflow.contract.domain.vo.WorkflowProcessOptionVO;

import java.util.List;

/**
 * 业务动作流程绑定解析器接口。
 */
public interface IWorkflowBindingResolver {

    /**
     * 查询业务动作可选流程列表。
     *
     * @param domainType 业务域类型
     * @param actionCode 业务动作编码
     * @return 可选流程列表
     */
    List<WorkflowProcessOptionVO> listProcessOptions(String domainType, String actionCode);

    /**
     * 解析并校验最终可用流程标识。
     *
     * @param domainType      业务域类型
     * @param actionCode      业务动作编码
     * @param requestedKey    前端请求流程标识
     * @return 最终流程标识
     */
    String resolveProcessKey(String domainType, String actionCode, String requestedKey);
}




package com.erp.system.service;

/**
 * 系统模块工作流定义远程门面接口。
 */
public interface ISysWorkflowDefinitionService {

    /**
     * 发布流程定义。
     *
     * @param definitionId 流程定义ID
     * @param operator 操作人账号
     * @return true 表示成功
     */
    boolean publishDefinition(Long definitionId, String operator);
}

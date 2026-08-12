package com.erp.system.service.impl;

import com.erp.common.client.internal.InternalWorkflowClient;
import com.erp.system.service.ISysWorkflowDefinitionService;
import org.springframework.stereotype.Service;

/**
 * 系统模块流程定义远程门面实现。
 */
@Service
public class RemoteWorkflowDefinitionServiceImpl implements ISysWorkflowDefinitionService {
    private final InternalWorkflowClient internalWorkflowClient;

    public RemoteWorkflowDefinitionServiceImpl(InternalWorkflowClient internalWorkflowClient) {
        this.internalWorkflowClient = internalWorkflowClient;
    }

    /**
     * 发布流程定义。
     *
     * @param definitionId 流程定义ID
     * @param operator 操作人账号
     * @return true 表示成功
     */
    @Override
    public boolean publishDefinition(Long definitionId, String operator) {
        return internalWorkflowClient.publishDefinition(definitionId);
    }
}

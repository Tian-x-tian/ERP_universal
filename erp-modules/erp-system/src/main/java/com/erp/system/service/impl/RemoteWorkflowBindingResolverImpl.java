package com.erp.system.service.impl;

import com.erp.common.client.internal.InternalWorkflowClient;
import com.erp.system.service.IWorkflowBindingResolver;
import com.erp.workflow.contract.domain.vo.WorkflowProcessOptionVO;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;

/**
 * 系统模块流程绑定远程门面实现。
 */
@Service
public class RemoteWorkflowBindingResolverImpl implements IWorkflowBindingResolver {
    private final InternalWorkflowClient internalWorkflowClient;

    public RemoteWorkflowBindingResolverImpl(InternalWorkflowClient internalWorkflowClient) {
        this.internalWorkflowClient = internalWorkflowClient;
    }

    /**
     * 查询业务动作可选流程列表。
     *
     * @param domainType 业务域类型
     * @param actionCode 动作编码
     * @return 流程选项列表
     */
    @Override
    public List<WorkflowProcessOptionVO> listProcessOptions(String domainType, String actionCode) {
        return internalWorkflowClient.listProcessOptions(domainType, actionCode);
    }

    /**
     * 解析最终流程标识。
     *
     * @param domainType 业务域类型
     * @param actionCode 动作编码
     * @param requestedKey 请求流程标识
     * @return 最终流程标识
     */
    @Override
    public String resolveProcessKey(String domainType, String actionCode, String requestedKey) {
        List<WorkflowProcessOptionVO> optionList = listProcessOptions(domainType, actionCode);
        if (optionList == null || optionList.isEmpty()) {
            throw new IllegalStateException("当前动作未配置可用审批流程");
        }
        if (!StringUtils.hasText(requestedKey)) {
            for (WorkflowProcessOptionVO option : optionList) {
                if (option != null && "1".equals(option.getIsDefault()) && StringUtils.hasText(option.getProcessKey())) {
                    return option.getProcessKey();
                }
            }
            WorkflowProcessOptionVO first = optionList.get(0);
            return first == null ? null : first.getProcessKey();
        }
        for (WorkflowProcessOptionVO option : optionList) {
            if (option != null && StringUtils.hasText(option.getProcessKey())
                    && option.getProcessKey().equalsIgnoreCase(requestedKey.trim())) {
                return option.getProcessKey();
            }
        }
        throw new IllegalArgumentException("流程标识不在当前动作可选范围内：" + requestedKey);
    }
}

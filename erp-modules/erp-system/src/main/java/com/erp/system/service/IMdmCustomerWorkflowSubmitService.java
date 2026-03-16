package com.erp.system.service;

import com.erp.system.domain.MdmCustomer;

/**
 * 客户主数据审批提交流程服务接口。
 */
public interface IMdmCustomerWorkflowSubmitService {

    /**
     * 提交草稿客户生效审批。
     *
     * @param customerId 客户ID
     * @param processKey 流程标识
     * @param remark     提交备注
     * @return true 表示提交成功
     */
    boolean submitDraftActivation(Long customerId, Integer versionNo, String processKey, String remark);

    /**
     * 提交客户变更审批。
     *
     * @param customerId     客户ID
     * @param targetCustomer 目标客户数据
     * @param processKey     流程标识
     * @param remark         提交备注
     * @return true 表示提交成功
     */
    boolean submitChange(Long customerId, Integer versionNo, MdmCustomer targetCustomer, String processKey, String remark);

    /**
     * 提交客户停用审批。
     *
     * @param customerId 客户ID
     * @param processKey 流程标识
     * @param remark     提交备注
     * @return true 表示提交成功
     */
    boolean submitDisable(Long customerId, Integer versionNo, String processKey, String remark);
}

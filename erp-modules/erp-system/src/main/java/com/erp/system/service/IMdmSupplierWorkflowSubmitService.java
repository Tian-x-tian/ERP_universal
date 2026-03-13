package com.erp.system.service;

import com.erp.system.domain.MdmSupplier;

/**
 * 供应商主数据审批提交流程服务接口。
 */
public interface IMdmSupplierWorkflowSubmitService {

    /**
     * 提交草稿供应商生效审批。
     *
     * @param supplierId 供应商ID
     * @param processKey 流程标识
     * @param remark     提交备注
     * @return true 表示提交成功
     */
    boolean submitDraftActivation(Long supplierId, String processKey, String remark);

    /**
     * 提交供应商变更审批。
     *
     * @param supplierId     供应商ID
     * @param targetSupplier 目标供应商数据
     * @param processKey     流程标识
     * @param remark         提交备注
     * @return true 表示提交成功
     */
    boolean submitChange(Long supplierId, MdmSupplier targetSupplier, String processKey, String remark);

    /**
     * 提交供应商停用审批。
     *
     * @param supplierId 供应商ID
     * @param processKey 流程标识
     * @param remark     提交备注
     * @return true 表示提交成功
     */
    boolean submitDisable(Long supplierId, String processKey, String remark);
}

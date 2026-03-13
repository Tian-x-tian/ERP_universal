package com.erp.system.service;

import com.erp.system.domain.MdmWarehouse;

/**
 * 仓库主数据审批提交流程服务接口。
 */
public interface IMdmWarehouseWorkflowSubmitService {

    /**
     * 提交草稿仓库生效审批。
     *
     * @param warehouseId 仓库ID
     * @param processKey 流程标识
     * @param remark 提交备注
     * @return true 表示提交成功
     */
    boolean submitDraftActivation(Long warehouseId, String processKey, String remark);

    /**
     * 提交仓库变更审批。
     *
     * @param warehouseId 仓库ID
     * @param targetWarehouse 目标仓库数据
     * @param processKey 流程标识
     * @param remark 提交备注
     * @return true 表示提交成功
     */
    boolean submitChange(Long warehouseId, MdmWarehouse targetWarehouse, String processKey, String remark);

    /**
     * 提交仓库停用审批。
     *
     * @param warehouseId 仓库ID
     * @param processKey 流程标识
     * @param remark 提交备注
     * @return true 表示提交成功
     */
    boolean submitDisable(Long warehouseId, String processKey, String remark);
}

package com.erp.system.service;

import com.erp.system.domain.MdmItem;

/**
 * 物料主数据审批提交流程服务接口。
 */
public interface IMdmItemWorkflowSubmitService {

    /**
     * 提交草稿物料生效审批。
     *
     * @param itemId     物料ID
     * @param processKey 流程标识
     * @param remark     提交备注
     * @return true 表示提交成功
     */
    boolean submitDraftActivation(Long itemId, Integer versionNo, String processKey, String remark);

    /**
     * 提交物料变更审批。
     *
     * @param itemId     物料ID
     * @param targetItem 目标物料数据
     * @param processKey 流程标识
     * @param remark     提交备注
     * @return true 表示提交成功
     */
    boolean submitChange(Long itemId, Integer versionNo, MdmItem targetItem, String processKey, String remark);

    /**
     * 提交物料停用审批。
     *
     * @param itemId     物料ID
     * @param processKey 流程标识
     * @param remark     提交备注
     * @return true 表示提交成功
     */
    boolean submitDisable(Long itemId, Integer versionNo, String processKey, String remark);
}

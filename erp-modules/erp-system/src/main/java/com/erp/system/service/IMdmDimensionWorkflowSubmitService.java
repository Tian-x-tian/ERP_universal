package com.erp.system.service;

import com.erp.system.domain.MdmCostCenter;
import com.erp.system.domain.MdmOrg;
import com.erp.system.domain.MdmProject;

/**
 * 维度主数据审批提交流程服务接口。
 */
public interface IMdmDimensionWorkflowSubmitService {
    boolean submitOrgDraftActivation(Long orgId, String processKey, String remark);
    boolean submitOrgChange(Long orgId, MdmOrg targetOrg, String processKey, String remark);
    boolean submitOrgDisable(Long orgId, String processKey, String remark);
    boolean submitCostCenterDraftActivation(Long ccId, String processKey, String remark);
    boolean submitCostCenterChange(Long ccId, MdmCostCenter targetCostCenter, String processKey, String remark);
    boolean submitCostCenterDisable(Long ccId, String processKey, String remark);
    boolean submitProjectDraftActivation(Long projectId, String processKey, String remark);
    boolean submitProjectChange(Long projectId, MdmProject targetProject, String processKey, String remark);
    boolean submitProjectDisable(Long projectId, String processKey, String remark);
}

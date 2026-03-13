package com.erp.system.domain.vo;

import com.erp.system.domain.MdmCostCenter;

import java.io.Serializable;

/**
 * 成本中心主数据审批提交流程参数。
 */
public class MdmCostCenterWorkflowSubmitBody implements Serializable {
    private static final long serialVersionUID = 1L;
    private String processKey;
    private String remark;
    private MdmCostCenter costCenter;

    public String getProcessKey() {
        return processKey;
    }

    public void setProcessKey(String processKey) {
        this.processKey = processKey;
    }

    public String getRemark() {
        return remark;
    }

    public void setRemark(String remark) {
        this.remark = remark;
    }

    public MdmCostCenter getCostCenter() {
        return costCenter;
    }

    public void setCostCenter(MdmCostCenter costCenter) {
        this.costCenter = costCenter;
    }
}

package com.erp.system.domain.vo;

import com.erp.system.domain.MdmTaxRate;

import java.io.Serializable;

/**
 * 税率主数据审批提交流程参数。
 */
public class MdmTaxRateWorkflowSubmitBody implements Serializable {
    private static final long serialVersionUID = 1L;
    private String processKey;
    private String remark;
    private Integer versionNo;
    private MdmTaxRate taxRate;

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

    public Integer getVersionNo() {
        return versionNo;
    }

    public void setVersionNo(Integer versionNo) {
        this.versionNo = versionNo;
    }

    public MdmTaxRate getTaxRate() {
        return taxRate;
    }

    public void setTaxRate(MdmTaxRate taxRate) {
        this.taxRate = taxRate;
    }
}

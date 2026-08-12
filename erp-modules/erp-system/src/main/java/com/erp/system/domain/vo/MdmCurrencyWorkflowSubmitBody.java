package com.erp.system.domain.vo;

import com.erp.system.domain.MdmCurrency;

import java.io.Serializable;

/**
 * 币种主数据审批提交流程参数。
 */
public class MdmCurrencyWorkflowSubmitBody implements Serializable {
    private static final long serialVersionUID = 1L;
    private String processKey;
    private String remark;
    private Integer versionNo;
    private MdmCurrency currency;

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

    public MdmCurrency getCurrency() {
        return currency;
    }

    public void setCurrency(MdmCurrency currency) {
        this.currency = currency;
    }
}

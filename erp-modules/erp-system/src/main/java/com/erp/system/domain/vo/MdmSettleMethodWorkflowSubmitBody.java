package com.erp.system.domain.vo;

import com.erp.system.domain.MdmSettleMethod;

import java.io.Serializable;

/**
 * 结算方式主数据审批提交流程参数。
 */
public class MdmSettleMethodWorkflowSubmitBody implements Serializable {
    private static final long serialVersionUID = 1L;
    private String processKey;
    private String remark;
    private MdmSettleMethod settleMethod;

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

    public MdmSettleMethod getSettleMethod() {
        return settleMethod;
    }

    public void setSettleMethod(MdmSettleMethod settleMethod) {
        this.settleMethod = settleMethod;
    }
}

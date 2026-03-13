package com.erp.system.domain.vo;

import com.erp.system.domain.MdmUom;

import java.io.Serializable;

/**
 * 计量单位主数据审批提交流程参数。
 */
public class MdmUomWorkflowSubmitBody implements Serializable {
    private static final long serialVersionUID = 1L;
    private String processKey;
    private String remark;
    private MdmUom uom;

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

    public MdmUom getUom() {
        return uom;
    }

    public void setUom(MdmUom uom) {
        this.uom = uom;
    }
}

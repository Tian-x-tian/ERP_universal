package com.erp.system.domain.vo;

import com.erp.system.domain.MdmOrg;

import java.io.Serializable;

/**
 * 组织主数据审批提交流程参数。
 */
public class MdmOrgWorkflowSubmitBody implements Serializable {
    private static final long serialVersionUID = 1L;
    private String processKey;
    private String remark;
    private MdmOrg org;

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

    public MdmOrg getOrg() {
        return org;
    }

    public void setOrg(MdmOrg org) {
        this.org = org;
    }
}

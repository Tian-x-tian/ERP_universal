package com.erp.system.domain.vo;

import com.erp.system.domain.MdmCustomer;

import java.io.Serializable;

/**
 * 客户主数据审批提交流程参数。
 */
public class MdmCustomerWorkflowSubmitBody implements Serializable {
    private static final long serialVersionUID = 1L;

    /** 流程标识 */
    private String processKey;

    /** 提交备注 */
    private String remark;

    /** 当前版本号 */
    private Integer versionNo;

    /** 变更后的客户数据 */
    private MdmCustomer customer;


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

    public MdmCustomer getCustomer() {
        return customer;
    }

    public void setCustomer(MdmCustomer customer) {
        this.customer = customer;
    }
}

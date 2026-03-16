package com.erp.system.domain.vo;

import com.erp.system.domain.MdmSupplier;

import java.io.Serializable;

/**
 * 供应商主数据审批提交流程参数。
 */
public class MdmSupplierWorkflowSubmitBody implements Serializable {
    private static final long serialVersionUID = 1L;

    /** 流程标识 */
    private String processKey;

    /** 提交备注 */
    private String remark;

    /** 当前版本号 */
    private Integer versionNo;

    /** 变更后的供应商数据 */
    private MdmSupplier supplier;


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

    public MdmSupplier getSupplier() {
        return supplier;
    }

    public void setSupplier(MdmSupplier supplier) {
        this.supplier = supplier;
    }
}

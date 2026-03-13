package com.erp.system.domain.vo;

import com.erp.system.domain.MdmItem;

import java.io.Serializable;

/**
 * 物料主数据审批提交流程参数。
 */
public class MdmItemWorkflowSubmitBody implements Serializable {
    private static final long serialVersionUID = 1L;

    /** 流程标识 */
    private String processKey;

    /** 提交备注 */
    private String remark;

    /** 变更后的物料数据 */
    private MdmItem item;


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

    public MdmItem getItem() {
        return item;
    }

    public void setItem(MdmItem item) {
        this.item = item;
    }
}

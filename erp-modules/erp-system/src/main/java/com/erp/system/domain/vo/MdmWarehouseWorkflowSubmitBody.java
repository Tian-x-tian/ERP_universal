package com.erp.system.domain.vo;

import com.erp.system.domain.MdmWarehouse;

import java.io.Serializable;

/**
 * 仓库主数据审批提交流程参数。
 */
public class MdmWarehouseWorkflowSubmitBody implements Serializable {
    private static final long serialVersionUID = 1L;

    /** 流程标识 */
    private String processKey;

    /** 提交备注 */
    private String remark;

    /** 当前版本号 */
    private Integer versionNo;

    /** 变更后的仓库数据 */
    private MdmWarehouse warehouse;


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

    public MdmWarehouse getWarehouse() {
        return warehouse;
    }

    public void setWarehouse(MdmWarehouse warehouse) {
        this.warehouse = warehouse;
    }
}

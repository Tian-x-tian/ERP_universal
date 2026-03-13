package com.erp.system.domain.vo;

import com.erp.system.domain.MdmEmployee;

import java.io.Serializable;

/**
 * 员工主数据审批提交流程参数。
 */
public class MdmEmployeeWorkflowSubmitBody implements Serializable {
    private static final long serialVersionUID = 1L;

    /** 流程标识 */
    private String processKey;

    /** 提交备注 */
    private String remark;

    /** 变更后的员工数据 */
    private MdmEmployee employee;


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

    public MdmEmployee getEmployee() {
        return employee;
    }

    public void setEmployee(MdmEmployee employee) {
        this.employee = employee;
    }
}

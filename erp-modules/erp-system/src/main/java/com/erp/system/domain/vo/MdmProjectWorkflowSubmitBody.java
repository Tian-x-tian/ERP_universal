package com.erp.system.domain.vo;

import com.erp.system.domain.MdmProject;

import java.io.Serializable;

/**
 * 项目主数据审批提交流程参数。
 */
public class MdmProjectWorkflowSubmitBody implements Serializable {
    private static final long serialVersionUID = 1L;
    private String processKey;
    private String remark;
    private Integer versionNo;
    private MdmProject project;

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

    public MdmProject getProject() {
        return project;
    }

    public void setProject(MdmProject project) {
        this.project = project;
    }
}

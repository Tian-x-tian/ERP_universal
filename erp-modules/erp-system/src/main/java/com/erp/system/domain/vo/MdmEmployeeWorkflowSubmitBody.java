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

    /** 当前版本号 */
    private Integer versionNo;

    /** HR 异动记录ID */
    private Long changeRecordId;

    /** HR 扩展档案快照JSON */
    private String archivePayloadJson;

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

    public Integer getVersionNo() {
        return versionNo;
    }

    public void setVersionNo(Integer versionNo) {
        this.versionNo = versionNo;
    }

    public Long getChangeRecordId() {
        return changeRecordId;
    }

    public void setChangeRecordId(Long changeRecordId) {
        this.changeRecordId = changeRecordId;
    }

    public String getArchivePayloadJson() {
        return archivePayloadJson;
    }

    public void setArchivePayloadJson(String archivePayloadJson) {
        this.archivePayloadJson = archivePayloadJson;
    }

    public MdmEmployee getEmployee() {
        return employee;
    }

    public void setEmployee(MdmEmployee employee) {
        this.employee = employee;
    }
}

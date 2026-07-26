package com.erp.system.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.erp.common.mybatis.BaseAuditEntity;

import java.io.Serializable;

/**
 * MDM 结算方式字典对象 mdm_settle_method。
 */
@TableName("mdm_settle_method")
public class MdmSettleMethod extends BaseAuditEntity implements Serializable {
    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long settleMethodId;
    private String tenantId;
    private String settleCode;
    private String settleName;
    private String status;
    private Integer versionNo;
    private String delFlag;
    private String remark;


    public Long getSettleMethodId() {
        return settleMethodId;
    }

    public void setSettleMethodId(Long settleMethodId) {
        this.settleMethodId = settleMethodId;
    }

    public String getTenantId() {
        return tenantId;
    }

    public void setTenantId(String tenantId) {
        this.tenantId = tenantId;
    }

    public String getSettleCode() {
        return settleCode;
    }

    public void setSettleCode(String settleCode) {
        this.settleCode = settleCode;
    }

    public String getSettleName() {
        return settleName;
    }

    public void setSettleName(String settleName) {
        this.settleName = settleName;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Integer getVersionNo() {
        return versionNo;
    }

    public void setVersionNo(Integer versionNo) {
        this.versionNo = versionNo;
    }

    public String getDelFlag() {
        return delFlag;
    }

    public void setDelFlag(String delFlag) {
        this.delFlag = delFlag;
    }

    public String getRemark() {
        return remark;
    }

    public void setRemark(String remark) {
        this.remark = remark;
    }

}

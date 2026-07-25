package com.erp.business.purchase.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * 采购申请单 pur_requisition。
 */
@TableName("pur_requisition")
@Data
public class PurchaseRequisition implements Serializable {
    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long requisitionId;
    private String tenantId;
    private String reqNo;
    private String reqTitle;
    private Long deptId;
    private Long applicantId;
    private String applicantName;
    private Date applyDate;
    private Date expectDate;
    private String status;
    private BigDecimal totalAmount;
    private String processKey;
    private Integer versionNo;
    private String remark;
    private String createBy;
    private Date createTime;
    private String updateBy;
    private Date updateTime;

    /** 申请行，不映射数据库列 */
    @TableField(exist = false)
    private List<PurchaseRequisitionLine> lines = new ArrayList<>();
}

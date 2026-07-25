package com.erp.business.purchase.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;

/**
 * 采购申请行 pur_requisition_line。
 */
@TableName("pur_requisition_line")
@Data
public class PurchaseRequisitionLine implements Serializable {
    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long lineId;
    private Long requisitionId;
    private String tenantId;
    private Integer lineNo;
    private Long itemId;
    private String itemCode;
    private String itemName;
    private String spec;
    private String uom;
    private BigDecimal qty;
    private BigDecimal estPrice;
    private BigDecimal estAmount;
    private Date expectDate;
    private String remark;
}

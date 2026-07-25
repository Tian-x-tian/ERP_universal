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
 * 采购订单 pur_order。
 */
@TableName("pur_order")
@Data
public class PurchaseOrder implements Serializable {
    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long orderId;
    private String tenantId;
    private String orderNo;
    private Long supplierId;
    private String supplierCode;
    private String supplierName;
    private Long requisitionId;
    private String requisitionNo;
    private Date orderDate;
    private Date expectDate;
    private String currencyCode;
    private String status;
    private BigDecimal totalQty;
    private BigDecimal totalAmount;
    private BigDecimal taxAmount;
    private String processKey;
    private String idempotencyNo;
    private Integer versionNo;
    private String remark;
    private String createBy;
    private Date createTime;
    private String updateBy;
    private Date updateTime;

    /** 订单行，不映射数据库列 */
    @TableField(exist = false)
    private List<PurchaseOrderLine> lines = new ArrayList<>();
}

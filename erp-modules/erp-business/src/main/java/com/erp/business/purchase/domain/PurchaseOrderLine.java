package com.erp.business.purchase.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * 采购订单行 pur_order_line。
 *
 * <p>{@code receivedQty} 与 {@code versionNo} 是阶段二防超收的核心字段：
 * 收货确认时以条件更新原子累加，不做「先查后写」。
 */
@TableName("pur_order_line")
@Data
public class PurchaseOrderLine implements Serializable {
    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long lineId;
    private Long orderId;
    private String tenantId;
    private Integer lineNo;
    private Long itemId;
    private String itemCode;
    private String itemName;
    private String spec;
    private String uom;
    private BigDecimal qty;
    private BigDecimal price;
    private BigDecimal amount;
    private BigDecimal taxRate;
    private BigDecimal receivedQty;
    private BigDecimal billedQty;
    private String lineStatus;
    private Integer versionNo;
    private String remark;
}

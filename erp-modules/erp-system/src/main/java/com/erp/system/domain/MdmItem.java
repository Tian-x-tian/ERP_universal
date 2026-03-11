package com.erp.system.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * MDM 物料主数据对象 mdm_item。
 */
@Data
@TableName("mdm_item")
public class MdmItem implements Serializable {
    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long itemId;
    private String tenantId;
    private String itemCode;
    private String itemName;
    private String specModel;
    private String brand;
    private String itemType;
    private Long categoryId;
    private Long unitId;
    private String unitConvert;
    private Long taxRateId;
    private String barcode;
    private Integer shelfLifeDays;
    private String batchControl;
    private String serialControl;
    private String costingMethod;
    private String status;
    private Date effectiveTime;
    private Integer versionNo;
    private String delFlag;
    private String remark;
    private String createBy;
    private Date createTime;
    private String updateBy;
    private Date updateTime;
}

package com.erp.system.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 流程定义对象 sys_wf_definition
 */
@Data
@TableName("sys_wf_definition")
public class SysWorkflowDefinition implements Serializable {
    private static final long serialVersionUID = 1L;

    /** 流程定义ID */
    @TableId(type = IdType.AUTO)
    private Long definitionId;

    /** 租户编号 */
    private String tenantId;

    /** 流程标识 */
    private String processKey;

    /** 流程名称 */
    private String processName;

    /** 流程分类（purchaseReq/purchaseExpense/expense/contract/stamp/onboard/offboard/inventoryTransfer/custom） */
    private String category;

    /** 版本号 */
    private Integer version;

    /** 状态（0草稿 1已发布 2停用） */
    private String status;

    /** 表单定义JSON */
    private String formSchema;

    /** 流程设计器JSON */
    private String modelContent;

    /** 发布人 */
    private String publishBy;

    /** 发布时间 */
    private Date publishTime;

    /** 备注 */
    private String remark;

    /** 创建者 */
    private String createBy;

    /** 创建时间 */
    private Date createTime;

    /** 更新者 */
    private String updateBy;

    /** 更新时间 */
    private Date updateTime;
}


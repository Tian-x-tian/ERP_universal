package com.erp.system.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 流程实例对象 sys_wf_instance
 */
@Data
@TableName("sys_wf_instance")
public class SysWorkflowInstance implements Serializable {
    private static final long serialVersionUID = 1L;

    /** 流程实例ID */
    @TableId(type = IdType.AUTO)
    private Long instanceId;

    /** 租户编号 */
    private String tenantId;

    /** 流程定义ID */
    private Long definitionId;

    /** 流程标识 */
    private String processKey;

    /** 流程名称 */
    private String processName;

    /** 流程分类 */
    private String category;

    /** 业务单号 */
    private String businessNo;

    /** 业务类型 */
    private String businessType;

    /** 发起表单数据JSON */
    private String formData;

    /** 当前节点名称 */
    private String currentNode;

    /** 发起人用户ID */
    private Long initiatorUserId;

    /** 发起人账号 */
    private String initiatorUserName;

    /** 发起人昵称 */
    private String initiatorNickName;

    /** 状态（0进行中 1已完成 2已驳回 3已撤销） */
    private String status;

    /** 发起时间 */
    private Date startTime;

    /** 结束时间 */
    private Date finishTime;

    /** 最近动作类型 */
    private String lastAction;

    /** 最近动作人ID */
    private Long lastActionUserId;

    /** 最近动作人账号 */
    private String lastActionUserName;

    /** 最近动作时间 */
    private Date lastActionTime;

    /** 备注 */
    private String remark;
}


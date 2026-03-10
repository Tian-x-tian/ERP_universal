package com.erp.system.domain.vo;

import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 流程发起请求对象。
 */
@Data
public class WorkflowStartBody implements Serializable {
    private static final long serialVersionUID = 1L;

    /** 流程标识 */
    private String processKey;

    /** 业务单号 */
    private String businessNo;

    /** 业务类型 */
    private String businessType;

    /** 起始节点名称 */
    private String nodeName;

    /** 目标审批人用户ID */
    private Long assigneeUserId;

    /** 目标审批人账号 */
    private String assigneeUserName;

    /** 目标审批人昵称 */
    private String assigneeNickName;

    /** 优先级（H高 M中 L低） */
    private String priority;

    /** 截止时间 */
    private Date dueTime;

    /** 发起表单JSON */
    private String formData;

    /** 备注 */
    private String remark;
}


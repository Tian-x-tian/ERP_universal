package com.erp.system.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 系统消息通知对象 sys_notice
 */
@Data
@TableName("sys_notice")
public class SysNotice implements Serializable {
    private static final long serialVersionUID = 1L;

    /** 消息ID */
    @TableId(type = IdType.AUTO)
    private Long noticeId;

    /** 租户编号 */
    private String tenantId;

    /** 消息标题 */
    private String title;

    /** 消息类型 */
    private String noticeType;

    /** 消息来源 */
    private String source;

    /** 关联业务单号 */
    private String businessNo;

    /** 消息内容 */
    private String content;

    /** 接收人用户ID */
    private Long receiverUserId;

    /** 状态（0未读 1已读） */
    private String status;

    /** 已读时间 */
    private Date readTime;

    /** 创建时间 */
    private Date createTime;
}

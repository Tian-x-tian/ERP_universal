package com.erp.business.hr.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 系统通知镜像对象。
 */
@Data
@TableName("sys_notice")
public class HrSysNotice implements Serializable {
    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long noticeId;
    private String tenantId;
    private String title;
    private String noticeType;
    private String source;
    private String businessNo;
    private String content;
    private Long receiverUserId;
    private String deliveryChannel;
    private String deliveryStatus;
    private Date deliveryTime;
    private String externalMessageId;
    private String status;
    private Date readTime;
    private Date createTime;
}

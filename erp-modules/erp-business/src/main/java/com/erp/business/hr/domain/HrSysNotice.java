package com.erp.business.hr.domain;

import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 系统通知 DTO。
 */
@Data
public class HrSysNotice implements Serializable {
    private static final long serialVersionUID = 1L;

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

package com.erp.workflow.domain.platform;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.io.Serializable;
import java.util.Date;

/**
 * 系统消息通知对象 sys_notice
 */
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

    /** 送达渠道（IN_APP/SMS/WECOM） */
    private String deliveryChannel;

    /** 送达状态（0待发送 1发送中 2已送达 3失败） */
    private String deliveryStatus;

    /** 送达时间 */
    private Date deliveryTime;

    /** 外部消息ID（短信/企业微信回执） */
    private String externalMessageId;

    /** 状态（0未读 1已读） */
    private String status;

    /** 已读时间 */
    private Date readTime;

    /** 创建时间 */
    private Date createTime;


    public Long getNoticeId() {
        return noticeId;
    }

    public void setNoticeId(Long noticeId) {
        this.noticeId = noticeId;
    }

    public String getTenantId() {
        return tenantId;
    }

    public void setTenantId(String tenantId) {
        this.tenantId = tenantId;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getNoticeType() {
        return noticeType;
    }

    public void setNoticeType(String noticeType) {
        this.noticeType = noticeType;
    }

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }

    public String getBusinessNo() {
        return businessNo;
    }

    public void setBusinessNo(String businessNo) {
        this.businessNo = businessNo;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public Long getReceiverUserId() {
        return receiverUserId;
    }

    public void setReceiverUserId(Long receiverUserId) {
        this.receiverUserId = receiverUserId;
    }

    public String getDeliveryChannel() {
        return deliveryChannel;
    }

    public void setDeliveryChannel(String deliveryChannel) {
        this.deliveryChannel = deliveryChannel;
    }

    public String getDeliveryStatus() {
        return deliveryStatus;
    }

    public void setDeliveryStatus(String deliveryStatus) {
        this.deliveryStatus = deliveryStatus;
    }

    public Date getDeliveryTime() {
        return deliveryTime;
    }

    public void setDeliveryTime(Date deliveryTime) {
        this.deliveryTime = deliveryTime;
    }

    public String getExternalMessageId() {
        return externalMessageId;
    }

    public void setExternalMessageId(String externalMessageId) {
        this.externalMessageId = externalMessageId;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Date getReadTime() {
        return readTime;
    }

    public void setReadTime(Date readTime) {
        this.readTime = readTime;
    }

    public Date getCreateTime() {
        return createTime;
    }

    public void setCreateTime(Date createTime) {
        this.createTime = createTime;
    }
}


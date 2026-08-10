package com.erp.system.domain;

import com.baomidou.mybatisplus.annotation.FieldStrategy;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.io.Serializable;
import java.util.Date;

/**
 * AI 每日简报对象 sys_ai_brief。
 */
@TableName("sys_ai_brief")
public class SysAiBrief implements Serializable {
    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long briefId;
    private String tenantId;
    private Long userId;
    private Date briefDate;
    private String briefType;
    /** 状态（PENDING/READY/FAILED） */
    private String status;
    /*
     * 下面四个字段必须用 ALWAYS 更新策略。
     *
     * MyBatis-Plus 默认的 NOT_NULL 策略会把 null 字段整个从 SET 子句里省掉，于是一次失败的重算
     * （blocksJson/model/generateMs 全为 null）无法覆盖上一轮成功生成留下的旧值——用户会看到一份
     * 状态是 FAILED、内容却还是上午那份数据的简报，甚至可能包含他此刻已无权查看的行。
     * saveResult 每次都会给这四个字段赋值，因此 ALWAYS 是安全的。
     */
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private String summary;
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private String blocksJson;
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private String model;
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private Long generateMs;
    private Date createTime;
    private Date updateTime;

    public Long getBriefId() {
        return briefId;
    }

    public void setBriefId(Long briefId) {
        this.briefId = briefId;
    }

    public String getTenantId() {
        return tenantId;
    }

    public void setTenantId(String tenantId) {
        this.tenantId = tenantId;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public Date getBriefDate() {
        return briefDate;
    }

    public void setBriefDate(Date briefDate) {
        this.briefDate = briefDate;
    }

    public String getBriefType() {
        return briefType;
    }

    public void setBriefType(String briefType) {
        this.briefType = briefType;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getSummary() {
        return summary;
    }

    public void setSummary(String summary) {
        this.summary = summary;
    }

    public String getBlocksJson() {
        return blocksJson;
    }

    public void setBlocksJson(String blocksJson) {
        this.blocksJson = blocksJson;
    }

    public String getModel() {
        return model;
    }

    public void setModel(String model) {
        this.model = model;
    }

    public Long getGenerateMs() {
        return generateMs;
    }

    public void setGenerateMs(Long generateMs) {
        this.generateMs = generateMs;
    }

    public Date getCreateTime() {
        return createTime;
    }

    public void setCreateTime(Date createTime) {
        this.createTime = createTime;
    }

    public Date getUpdateTime() {
        return updateTime;
    }

    public void setUpdateTime(Date updateTime) {
        this.updateTime = updateTime;
    }
}

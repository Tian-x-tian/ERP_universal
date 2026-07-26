package com.erp.common.mybatis;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.TableField;

import java.io.Serializable;
import java.util.Date;

/**
 * 审计字段基类。
 * 承载 create_by / create_time / update_by / update_time 四个留痕字段，
 * 由 {@link AuditMetaObjectHandlerSupport} 在插入与更新时自动填充，业务代码不必再手写赋值。
 *
 * <p>填充策略为「仅在字段为空时补齐」，因此显式赋值（如数据导入指定原始操作人）仍然生效。</p>
 *
 * <p>注意：自动填充依赖 MyBatis Plus 的实体参数解析，仅对 {@code insert} / {@code updateById}
 * 一类以实体为参数的操作生效；纯 {@code UpdateWrapper} 与手写 XML 更新语句不会被填充，
 * 这类场景由建表脚本中的 {@code ON UPDATE CURRENT_TIMESTAMP} 兜底。</p>
 */
public abstract class BaseAuditEntity implements Serializable {
    private static final long serialVersionUID = 1L;

    /**
     * 创建人账号。
     */
    @TableField(value = "create_by", fill = FieldFill.INSERT)
    private String createBy;

    /**
     * 创建时间。
     */
    @TableField(value = "create_time", fill = FieldFill.INSERT)
    private Date createTime;

    /**
     * 更新人账号。
     */
    @TableField(value = "update_by", fill = FieldFill.INSERT_UPDATE)
    private String updateBy;

    /**
     * 更新时间。
     */
    @TableField(value = "update_time", fill = FieldFill.INSERT_UPDATE)
    private Date updateTime;

    public String getCreateBy() {
        return createBy;
    }

    public void setCreateBy(String createBy) {
        this.createBy = createBy;
    }

    public Date getCreateTime() {
        return createTime;
    }

    public void setCreateTime(Date createTime) {
        this.createTime = createTime;
    }

    public String getUpdateBy() {
        return updateBy;
    }

    public void setUpdateBy(String updateBy) {
        this.updateBy = updateBy;
    }

    public Date getUpdateTime() {
        return updateTime;
    }

    public void setUpdateTime(Date updateTime) {
        this.updateTime = updateTime;
    }
}

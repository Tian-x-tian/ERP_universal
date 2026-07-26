package com.erp.workflow.contract.domain;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.io.Serializable;
import java.util.Date;

/**
 * 流程定义对象 sys_wf_definition
 */
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

    /** 创建者（由 MetaObjectHandler 自动填充） */
    @TableField(value = "create_by", fill = FieldFill.INSERT)
    private String createBy;

    /** 创建时间（由 MetaObjectHandler 自动填充） */
    @TableField(value = "create_time", fill = FieldFill.INSERT)
    private Date createTime;

    /** 更新者（由 MetaObjectHandler 自动填充） */
    @TableField(value = "update_by", fill = FieldFill.INSERT_UPDATE)
    private String updateBy;

    /** 更新时间（由 MetaObjectHandler 自动填充） */
    @TableField(value = "update_time", fill = FieldFill.INSERT_UPDATE)
    private Date updateTime;



    public Long getDefinitionId() {
        return definitionId;
    }

    public void setDefinitionId(Long definitionId) {
        this.definitionId = definitionId;
    }

    public String getTenantId() {
        return tenantId;
    }

    public void setTenantId(String tenantId) {
        this.tenantId = tenantId;
    }

    public String getProcessKey() {
        return processKey;
    }

    public void setProcessKey(String processKey) {
        this.processKey = processKey;
    }

    public String getProcessName() {
        return processName;
    }

    public void setProcessName(String processName) {
        this.processName = processName;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public Integer getVersion() {
        return version;
    }

    public void setVersion(Integer version) {
        this.version = version;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getFormSchema() {
        return formSchema;
    }

    public void setFormSchema(String formSchema) {
        this.formSchema = formSchema;
    }

    public String getModelContent() {
        return modelContent;
    }

    public void setModelContent(String modelContent) {
        this.modelContent = modelContent;
    }

    public String getPublishBy() {
        return publishBy;
    }

    public void setPublishBy(String publishBy) {
        this.publishBy = publishBy;
    }

    public Date getPublishTime() {
        return publishTime;
    }

    public void setPublishTime(Date publishTime) {
        this.publishTime = publishTime;
    }

    public String getRemark() {
        return remark;
    }

    public void setRemark(String remark) {
        this.remark = remark;
    }

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


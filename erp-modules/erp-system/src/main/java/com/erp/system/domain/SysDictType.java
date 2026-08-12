package com.erp.system.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.io.Serializable;
import java.util.Date;

/**
 * 字典类型表 sys_dict_type
 */
@TableName(value = "sys_dict_type", autoResultMap = true)
public class SysDictType implements Serializable {
    private static final long serialVersionUID = 1L;

    /** 字典主键 */
    @TableId(value = "dict_id", type = IdType.AUTO)
    private Long dict_id;

    /** 字典名称 */
    @TableField("dict_name")
    private String dict_name;

    /** 字典类型 */
    @TableField("dict_type")
    private String dict_type;

    /** 状态（0正常 1停用） */
    private String status;

    /** 创建者 */
    @TableField("create_by")
    private String create_by;

    /** 创建时间 */
    @TableField("create_time")
    private Date create_time;

    /** 更新者 */
    @TableField("update_by")
    private String update_by;

    /** 更新时间 */
    @TableField("update_time")
    private Date update_time;

    /** 备注 */
    private String remark;


    public Long getDict_id() {
        return dict_id;
    }

    public void setDict_id(Long dict_id) {
        this.dict_id = dict_id;
    }

    public String getDict_name() {
        return dict_name;
    }

    public void setDict_name(String dict_name) {
        this.dict_name = dict_name;
    }

    public String getDict_type() {
        return dict_type;
    }

    public void setDict_type(String dict_type) {
        this.dict_type = dict_type;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getCreate_by() {
        return create_by;
    }

    public void setCreate_by(String create_by) {
        this.create_by = create_by;
    }

    public Date getCreate_time() {
        return create_time;
    }

    public void setCreate_time(Date create_time) {
        this.create_time = create_time;
    }

    public String getUpdate_by() {
        return update_by;
    }

    public void setUpdate_by(String update_by) {
        this.update_by = update_by;
    }

    public Date getUpdate_time() {
        return update_time;
    }

    public void setUpdate_time(Date update_time) {
        this.update_time = update_time;
    }

    public String getRemark() {
        return remark;
    }

    public void setRemark(String remark) {
        this.remark = remark;
    }
}

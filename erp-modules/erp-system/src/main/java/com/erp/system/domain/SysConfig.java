package com.erp.system.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.io.Serializable;
import java.util.Date;

/**
 * 参数配置表 sys_config
 */
@TableName(value = "sys_config", autoResultMap = true)
public class SysConfig implements Serializable {
    private static final long serialVersionUID = 1L;

    /** 参数主键 */
    @TableId(value = "config_id", type = IdType.AUTO)
    private Integer config_id;

    /** 参数名称 */
    @TableField("config_name")
    private String config_name;

    /** 参数键名 */
    @TableField("config_key")
    private String config_key;

    /** 参数键值 */
    @TableField("config_value")
    private String config_value;

    /** 系统内置（Y是 N否） */
    @TableField("config_type")
    private String config_type;

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


    public Integer getConfig_id() {
        return config_id;
    }

    public void setConfig_id(Integer config_id) {
        this.config_id = config_id;
    }

    public String getConfig_name() {
        return config_name;
    }

    public void setConfig_name(String config_name) {
        this.config_name = config_name;
    }

    public String getConfig_key() {
        return config_key;
    }

    public void setConfig_key(String config_key) {
        this.config_key = config_key;
    }

    public String getConfig_value() {
        return config_value;
    }

    public void setConfig_value(String config_value) {
        this.config_value = config_value;
    }

    public String getConfig_type() {
        return config_type;
    }

    public void setConfig_type(String config_type) {
        this.config_type = config_type;
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

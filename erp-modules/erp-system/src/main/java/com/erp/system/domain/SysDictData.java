package com.erp.system.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.io.Serializable;
import java.util.Date;

/**
 * 字典数据表 sys_dict_data
 */
@TableName(value = "sys_dict_data", autoResultMap = true)
public class SysDictData implements Serializable {
    private static final long serialVersionUID = 1L;

    /** 字典编码 */
    @TableId(value = "dict_code", type = IdType.AUTO)
    private Long dict_code;

    /** 字典排序 */
    @TableField("dict_sort")
    private Integer dict_sort;

    /** 字典标签 */
    @TableField("dict_label")
    private String dict_label;

    /** 字典键值 */
    @TableField("dict_value")
    private String dict_value;

    /** 字典类型 */
    @TableField("dict_type")
    private String dict_type;

    /** 样式属性（其他样式扩展） */
    @TableField("css_class")
    private String css_class;

    /** 表格回显样式 */
    @TableField("list_class")
    private String list_class;

    /** 是否默认（Y是 N否） */
    @TableField("is_default")
    private String is_default;

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


    public Long getDict_code() {
        return dict_code;
    }

    public void setDict_code(Long dict_code) {
        this.dict_code = dict_code;
    }

    public Integer getDict_sort() {
        return dict_sort;
    }

    public void setDict_sort(Integer dict_sort) {
        this.dict_sort = dict_sort;
    }

    public String getDict_label() {
        return dict_label;
    }

    public void setDict_label(String dict_label) {
        this.dict_label = dict_label;
    }

    public String getDict_value() {
        return dict_value;
    }

    public void setDict_value(String dict_value) {
        this.dict_value = dict_value;
    }

    public String getDict_type() {
        return dict_type;
    }

    public void setDict_type(String dict_type) {
        this.dict_type = dict_type;
    }

    public String getCss_class() {
        return css_class;
    }

    public void setCss_class(String css_class) {
        this.css_class = css_class;
    }

    public String getList_class() {
        return list_class;
    }

    public void setList_class(String list_class) {
        this.list_class = list_class;
    }

    public String getIs_default() {
        return is_default;
    }

    public void setIs_default(String is_default) {
        this.is_default = is_default;
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

package com.erp.system.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.io.Serializable;
import java.util.Date;

/**
 * 字典数据表 sys_dict_data
 */
@Data
@TableName("sys_dict_data")
public class SysDictData implements Serializable {
    private static final long serialVersionUID = 1L;

    /** 字典编码 */
    @TableId(value = "dict_code", type = IdType.AUTO)
    private Long dict_code;

    /** 字典排序 */
    private Integer dict_sort;

    /** 字典标签 */
    private String dict_label;

    /** 字典键值 */
    private String dict_value;

    /** 字典类型 */
    private String dict_type;

    /** 样式属性（其他样式扩展） */
    private String css_class;

    /** 表格回显样式 */
    private String list_class;

    /** 是否默认（Y是 N否） */
    private String is_default;

    /** 状态（0正常 1停用） */
    private String status;

    /** 创建者 */
    private String create_by;

    /** 创建时间 */
    private Date create_time;

    /** 更新者 */
    private String update_by;

    /** 更新时间 */
    private Date update_time;

    /** 备注 */
    private String remark;
}

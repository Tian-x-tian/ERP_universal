package com.erp.system.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.io.Serializable;
import java.util.Date;

/**
 * 字典类型表 sys_dict_type
 */
@Data
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
}

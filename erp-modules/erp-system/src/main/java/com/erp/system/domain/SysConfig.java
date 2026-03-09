package com.erp.system.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.io.Serializable;
import java.util.Date;

/**
 * 参数配置表 sys_config
 */
@Data
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
}

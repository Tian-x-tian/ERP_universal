package com.erp.system.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.io.Serializable;
import java.util.Date;

/**
 * 参数配置表 sys_config
 */
@Data
@TableName("sys_config")
public class SysConfig implements Serializable {
    private static final long serialVersionUID = 1L;

    /** 参数主键 */
    @TableId(value = "config_id", type = IdType.AUTO)
    private Integer config_id;

    /** 参数名称 */
    private String config_name;

    /** 参数键名 */
    private String config_key;

    /** 参数键值 */
    private String config_value;

    /** 系统内置（Y是 N否） */
    private String config_type;

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

package com.erp.system.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.io.Serializable;
import java.util.Date;

/**
 * 租户管理对象 sys_tenant
 */
@Data
@TableName("sys_tenant")
public class SysTenant implements Serializable {
    private static final long serialVersionUID = 1L;

    /** 租户ID */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 租户编号 */
    private String tenantId;

    /** 租户名称 */
    private String name;

    /** 联系人 */
    private String contactUser;

    /** 联系电话 */
    private String contactPhone;

    /** 状态（0正常 1停用） */
    private String status;

    /** 删除标志（0代表存在 2代表删除） */
    private String delFlag;

    /** 创建者 */
    private String createBy;

    /** 创建时间 */
    private Date createTime;

    /** 更新者 */
    private String updateBy;

    /** 更新时间 */
    private Date updateTime;

    /** 备注 */
    private String remark;
}

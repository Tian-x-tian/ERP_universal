package com.erp.system.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 公司信息对象 sys_company
 */
@Data
@TableName("sys_company")
public class SysCompany implements Serializable {
    private static final long serialVersionUID = 1L;

    /** 公司ID */
    @TableId(type = IdType.AUTO)
    private Long companyId;

    /** 租户编号 */
    private String tenantId;

    /** 公司编码 */
    private String companyCode;

    /** 公司名称 */
    private String companyName;

    /** 父公司ID */
    private Long parentCompanyId;

    /** 祖级列表 */
    private String ancestors;

    /** 负责人 */
    private String leader;

    /** 联系电话 */
    private String phone;

    /** 公司状态（0正常 1停用） */
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

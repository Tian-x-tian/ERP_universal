package com.erp.business.hr.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;

/**
 * 用户镜像对象。
 */
@Data
@TableName("sys_user")
public class HrSysUser implements Serializable {
    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long userId;
    private String tenantId;
    private String userName;
    private String nickName;
    private String status;
    private String delFlag;
}

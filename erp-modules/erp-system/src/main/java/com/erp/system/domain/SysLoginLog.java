package com.erp.system.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 登录日志对象 sys_login_log
 */
@Data
@TableName("sys_login_log")
public class SysLoginLog implements Serializable {
    private static final long serialVersionUID = 1L;

    /** 日志主键 */
    @TableId(type = IdType.AUTO)
    private Long infoId;

    /** 租户编号 */
    private String tenantId;

    /** 用户账号 */
    private String userName;

    /** 登录IP地址 */
    private String ipaddr;

    /** 登录状态（0成功 1失败） */
    private String status;

    /** 提示消息 */
    private String msg;

    /** 访问时间 */
    private Date loginTime;
}

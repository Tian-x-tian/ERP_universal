package com.erp.system.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 审计日志对象 sys_audit_log
 */
@Data
@TableName("sys_audit_log")
public class SysAuditLog implements Serializable {
    private static final long serialVersionUID = 1L;

    /** 日志ID */
    @TableId(type = IdType.AUTO)
    private Long logId;

    /** 租户编号 */
    private String tenantId;

    /** 操作人账号 */
    private String operator;

    /** 操作类型 */
    private String operationType;

    /** 请求方法 */
    private String requestMethod;

    /** 请求URI */
    private String requestUri;

    /** 客户端IP */
    private String requestIp;

    /** 请求参数 */
    private String requestParams;

    /** 响应状态码 */
    private Integer responseCode;

    /** 是否成功（1成功 0失败） */
    private String successFlag;

    /** 错误信息 */
    private String errorMsg;

    /** 执行耗时（毫秒） */
    private Long costTime;

    /** 操作时间 */
    private Date operationTime;
}

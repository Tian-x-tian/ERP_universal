package com.erp.system.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * MDM 变更日志对象 mdm_change_log。
 */
@Data
@TableName("mdm_change_log")
public class MdmChangeLog implements Serializable {
    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long logId;
    private String tenantId;
    private String domainType;
    private Long bizId;
    private String changeType;
    private String beforeJson;
    private String afterJson;
    private String operator;
    private String traceId;
    private String source;
    private Date createTime;
}

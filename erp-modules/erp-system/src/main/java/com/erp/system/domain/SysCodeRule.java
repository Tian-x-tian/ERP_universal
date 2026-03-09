package com.erp.system.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 编码规则对象 sys_code_rule
 */
@Data
@TableName("sys_code_rule")
public class SysCodeRule implements Serializable {
    private static final long serialVersionUID = 1L;

    /** 规则ID */
    @TableId(type = IdType.AUTO)
    private Long ruleId;

    /** 租户编号 */
    private String tenantId;

    /** 规则编码 */
    private String ruleCode;

    /** 规则名称 */
    private String ruleName;

    /** 编码前缀 */
    private String prefix;

    /** 日期格式 */
    private String datePattern;

    /** 流水位数 */
    private Integer seqLength;

    /** 当前流水值 */
    private Long currentSeq;

    /** 状态（0启用 1停用） */
    private String status;

    /** 备注 */
    private String remark;

    /** 创建时间 */
    private Date createTime;

    /** 更新时间 */
    private Date updateTime;
}

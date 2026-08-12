package com.erp.business.hr.attendance.core.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.erp.common.mybatis.BaseAuditEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * 出勤定位规则。
 */
@Data
@EqualsAndHashCode(callSuper = false)
@TableName("hr_attendance_location_rule")
public class HrAttendanceLocationRule extends BaseAuditEntity implements Serializable {
    private static final long serialVersionUID = 1L;

    @TableId(value = "rule_id", type = IdType.AUTO)
    private Long ruleId;
    private String tenantId;
    private Long deptId;
    private String ruleName;
    private BigDecimal centerLatitude;
    private BigDecimal centerLongitude;
    private Integer radiusMeters;
    private String enabledFlag;
    private String remark;
}

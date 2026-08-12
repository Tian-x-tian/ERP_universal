package com.erp.business.hr.attendance.core.domain.vo;

import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 第三方出勤导入参数。
 */
@Data
public class HrAttendanceExternalRecordBody implements Serializable {
    private static final long serialVersionUID = 1L;

    private Long employeeId;
    private String externalBizNo;
    private String sourceSystem;
    private LocalDate workDate;
    private LocalDateTime signInTime;
    private LocalDateTime signOutTime;
    private BigDecimal signInLatitude;
    private BigDecimal signInLongitude;
    private BigDecimal signOutLatitude;
    private BigDecimal signOutLongitude;
    private String signInAddress;
    private String signOutAddress;
    private String remark;
}

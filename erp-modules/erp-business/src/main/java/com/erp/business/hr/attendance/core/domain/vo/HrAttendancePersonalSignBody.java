package com.erp.business.hr.attendance.core.domain.vo;

import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 个人签到签退入参。
 */
@Data
public class HrAttendancePersonalSignBody implements Serializable {
    private static final long serialVersionUID = 1L;

    private BigDecimal latitude;
    private BigDecimal longitude;
    private String address;
    private String deviceSource;
    private String remark;
    private LocalDateTime signTime;
}

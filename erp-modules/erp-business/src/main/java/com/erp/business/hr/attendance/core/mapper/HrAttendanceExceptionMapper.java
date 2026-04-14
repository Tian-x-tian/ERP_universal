package com.erp.business.hr.attendance.core.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.erp.business.hr.attendance.core.domain.HrAttendanceException;
import org.apache.ibatis.annotations.Mapper;

/**
 * 出勤异常 Mapper。
 */
@Mapper
public interface HrAttendanceExceptionMapper extends BaseMapper<HrAttendanceException> {
}

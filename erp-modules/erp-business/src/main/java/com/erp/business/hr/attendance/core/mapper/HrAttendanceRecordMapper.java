package com.erp.business.hr.attendance.core.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.erp.business.hr.attendance.core.domain.HrAttendanceRecord;
import org.apache.ibatis.annotations.Mapper;

/**
 * 出勤原子记录 Mapper。
 */
@Mapper
public interface HrAttendanceRecordMapper extends BaseMapper<HrAttendanceRecord> {
}

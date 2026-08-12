package com.erp.business.hr.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.erp.business.hr.domain.HrAttendanceSyncLog;
import org.apache.ibatis.annotations.Mapper;

/**
 * 出勤同步日志 Mapper。
 */
@Mapper
public interface HrAttendanceSyncLogMapper extends BaseMapper<HrAttendanceSyncLog> {
}


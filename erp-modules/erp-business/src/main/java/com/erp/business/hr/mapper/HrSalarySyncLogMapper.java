package com.erp.business.hr.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.erp.business.hr.domain.HrSalarySyncLog;
import org.apache.ibatis.annotations.Mapper;

/**
 * 薪资同步日志 Mapper。
 */
@Mapper
public interface HrSalarySyncLogMapper extends BaseMapper<HrSalarySyncLog> {
}

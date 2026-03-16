package com.erp.business.hr.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.erp.business.hr.domain.HrEmployeeChange;
import org.apache.ibatis.annotations.Mapper;

/**
 * 员工异动底座 Mapper。
 */
@Mapper
public interface HrEmployeeChangeMapper extends BaseMapper<HrEmployeeChange> {
}

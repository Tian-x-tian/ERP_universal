package com.erp.business.hr.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.erp.business.hr.domain.HrEmployeeContract;
import org.apache.ibatis.annotations.Mapper;

/**
 * 员工合同底座 Mapper。
 */
@Mapper
public interface HrEmployeeContractMapper extends BaseMapper<HrEmployeeContract> {
}

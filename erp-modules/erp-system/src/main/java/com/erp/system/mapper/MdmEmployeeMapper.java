package com.erp.system.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.erp.system.domain.MdmEmployee;
import org.apache.ibatis.annotations.Mapper;

/**
 * MDM 员工主数据 Mapper。
 */
@Mapper
public interface MdmEmployeeMapper extends BaseMapper<MdmEmployee> {
}

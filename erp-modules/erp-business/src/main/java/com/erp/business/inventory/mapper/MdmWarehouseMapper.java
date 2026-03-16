package com.erp.business.inventory.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.erp.business.inventory.domain.MdmWarehouse;
import org.apache.ibatis.annotations.Mapper;

/**
 * 仓库主数据只读 Mapper。
 */
@Mapper
public interface MdmWarehouseMapper extends BaseMapper<MdmWarehouse> {
}

package com.erp.business.inventory.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.erp.business.inventory.domain.InventoryBatchRecord;
import org.apache.ibatis.annotations.Mapper;

/**
 * 批次记录 Mapper。
 */
@Mapper
public interface InventoryBatchRecordMapper extends BaseMapper<InventoryBatchRecord> {
}

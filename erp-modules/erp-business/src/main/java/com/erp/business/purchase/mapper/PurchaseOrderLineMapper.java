package com.erp.business.purchase.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.erp.business.purchase.domain.PurchaseOrderLine;
import org.apache.ibatis.annotations.Mapper;

/**
 * PurchaseOrderLine Mapper。
 */
@Mapper
public interface PurchaseOrderLineMapper extends BaseMapper<PurchaseOrderLine> {
}

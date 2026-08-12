package com.erp.business.purchase.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.erp.business.purchase.domain.PurchaseOrder;
import org.apache.ibatis.annotations.Mapper;

/**
 * PurchaseOrder Mapper。
 */
@Mapper
public interface PurchaseOrderMapper extends BaseMapper<PurchaseOrder> {
}

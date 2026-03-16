package com.erp.system.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * 库存只读引用检查 Mapper。
 */
@Mapper
public interface InventoryReferenceMapper {

    /**
     * 统计仓库下可用库存余额记录数。
     *
     * @param warehouseId 仓库ID
     * @return 记录数
     */
    @Select("""
            SELECT COUNT(1)
            FROM inv_stock_balance
            WHERE warehouse_id = #{warehouseId}
              AND available_qty > 0
            """)
    Long countWarehouseAvailableStock(@Param("warehouseId") Long warehouseId);

    /**
     * 统计仓库下未完成入库单数量。
     *
     * @param warehouseId 仓库ID
     * @return 记录数
     */
    @Select("""
            SELECT COUNT(1)
            FROM inv_inbound_order
            WHERE warehouse_id = #{warehouseId}
              AND status NOT IN ('COMPLETED', 'CANCELLED')
            """)
    Long countWarehouseOpenInboundOrders(@Param("warehouseId") Long warehouseId);

    /**
     * 统计仓库下未完成出库单数量。
     *
     * @param warehouseId 仓库ID
     * @return 记录数
     */
    @Select("""
            SELECT COUNT(1)
            FROM inv_outbound_order
            WHERE warehouse_id = #{warehouseId}
              AND status NOT IN ('COMPLETED', 'CANCELLED')
            """)
    Long countWarehouseOpenOutboundOrders(@Param("warehouseId") Long warehouseId);
}

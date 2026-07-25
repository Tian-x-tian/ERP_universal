package com.erp.business.inventory.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * 仓库占用情况查询 Mapper。
 *
 * <p>这些统计原先由 erp-system 直接查询 {@code inv_*} 表实现，属于跨服务读库；
 * 现收归库存模块自身，通过内部接口对外提供。
 */
@Mapper
public interface InventoryWarehouseUsageMapper {

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
    Long countAvailableStock(@Param("warehouseId") Long warehouseId);

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
    Long countOpenInboundOrders(@Param("warehouseId") Long warehouseId);

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
    Long countOpenOutboundOrders(@Param("warehouseId") Long warehouseId);
}

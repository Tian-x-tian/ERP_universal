package com.erp.business.common.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.Map;

/**
 * 业务域 AI 只读数据集查询 Mapper。
 *
 * <p>租户条件由 MyBatis-Plus 租户拦截器自动补齐，语句中不手写 tenant_id。
 * 这里读取 mdm_warehouse / mdm_item / mdm_employee 属于既有惯例（见
 * {@code com.erp.business.inventory.domain.MdmWarehouse}），仅用于只读展示取名。</p>
 */
@Mapper
public interface AiDatasetMapper {

    /**
     * 按仓库统计库存概览。
     *
     * @param limit 返回条数
     * @return 库存概览
     */
    @Select("SELECT balance.warehouse_id AS warehouse_id, "
            + "MAX(warehouse.wh_name) AS warehouse_name, "
            + "COUNT(DISTINCT balance.item_id) AS item_kinds, "
            + "SUM(balance.on_hand_qty) AS on_hand_qty, "
            + "SUM(balance.available_qty) AS available_qty, "
            + "SUM(balance.frozen_qty) AS frozen_qty, "
            + "SUM(balance.in_transit_qty) AS in_transit_qty "
            + "FROM inv_stock_balance balance "
            + "LEFT JOIN mdm_warehouse warehouse ON warehouse.warehouse_id = balance.warehouse_id "
            + "GROUP BY balance.warehouse_id ORDER BY on_hand_qty DESC LIMIT #{limit}")
    List<Map<String, Object>> selectStockOverview(@Param("limit") int limit);

    /**
     * 查询可用库存最低的物料。
     *
     * @param limit 返回条数
     * @return 低库存物料
     */
    @Select("SELECT balance.item_id AS item_id, "
            + "MAX(item.item_name) AS item_name, "
            + "MAX(item.item_code) AS item_code, "
            + "SUM(balance.available_qty) AS available_qty, "
            + "SUM(balance.frozen_qty) AS frozen_qty "
            + "FROM inv_stock_balance balance "
            + "LEFT JOIN mdm_item item ON item.item_id = balance.item_id "
            + "GROUP BY balance.item_id HAVING available_qty >= 0 "
            + "ORDER BY available_qty ASC LIMIT #{limit}")
    List<Map<String, Object>> selectLowStockItems(@Param("limit") int limit);

    /**
     * 按类型统计未关闭的库存预警。
     *
     * @param limit 返回条数
     * @return 库存预警分布
     */
    @Select("SELECT warning_type AS warning_type, "
            + "COUNT(1) AS total_count, "
            + "SUM(CASE WHEN status = 'NEW' THEN 1 ELSE 0 END) AS new_count "
            + "FROM inv_warning_record WHERE status <> 'CLOSED' "
            + "GROUP BY warning_type ORDER BY total_count DESC LIMIT #{limit}")
    List<Map<String, Object>> selectStockWarning(@Param("limit") int limit);

    /**
     * 按岗位统计在岗人数。
     *
     * @param limit 返回条数
     * @return 在岗人数分布
     */
    @Select("SELECT COALESCE(NULLIF(TRIM(position), ''), '未设置') AS post_name, "
            + "COUNT(1) AS headcount "
            + "FROM mdm_employee WHERE del_flag = '0' AND status = 'ACTIVE' "
            + "GROUP BY post_name ORDER BY headcount DESC LIMIT #{limit}")
    List<Map<String, Object>> selectHeadcountByPost(@Param("limit") int limit);

    /**
     * 统计员工在岗与离职总量。
     *
     * @return 员工状态汇总
     */
    @Select("SELECT SUM(CASE WHEN status = 'ACTIVE' THEN 1 ELSE 0 END) AS active_count, "
            + "SUM(CASE WHEN status <> 'ACTIVE' THEN 1 ELSE 0 END) AS inactive_count, "
            + "COUNT(1) AS total_count "
            + "FROM mdm_employee WHERE del_flag = '0'")
    Map<String, Object> selectHeadcountSummary();

    /**
     * 按类型统计未关闭的 HR 预警。
     *
     * @param limit 返回条数
     * @return HR 预警分布
     */
    @Select("SELECT warning_type AS warning_type, "
            + "COUNT(1) AS total_count, "
            + "SUM(CASE WHEN status = 'NEW' THEN 1 ELSE 0 END) AS new_count "
            + "FROM hr_warning_record WHERE status <> 'CLOSED' "
            + "GROUP BY warning_type ORDER BY total_count DESC LIMIT #{limit}")
    List<Map<String, Object>> selectHrWarning(@Param("limit") int limit);

    /**
     * 查询即将到期的 HR 预警明细。
     *
     * @param limit 返回条数
     * @return 预警明细
     */
    @Select("SELECT warning_type AS warning_type, warning_title AS warning_title, "
            + "expire_date AS expire_date, status AS status "
            + "FROM hr_warning_record WHERE status <> 'CLOSED' AND expire_date IS NOT NULL "
            + "ORDER BY expire_date ASC LIMIT #{limit}")
    List<Map<String, Object>> selectHrWarningDetail(@Param("limit") int limit);
}

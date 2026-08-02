package com.erp.system.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * 平台域 AI 只读数据集查询 Mapper。
 *
 * <p>租户条件由 MyBatis-Plus 租户拦截器自动补齐，语句中不手写 tenant_id。</p>
 */
@Mapper
public interface AiDatasetMapper {

    /**
     * 按消息类型统计消息读取情况。
     *
     * @param receiverUserId 接收人用户ID，为空表示统计全租户
     * @param limit          返回条数
     * @return 消息分布统计
     */
    @Select("<script>"
            + "SELECT notice_type AS notice_type, "
            + "COUNT(1) AS total_count, "
            + "SUM(CASE WHEN status = '0' THEN 1 ELSE 0 END) AS unread_count, "
            + "SUM(CASE WHEN delivery_status = '3' THEN 1 ELSE 0 END) AS failed_count "
            + "FROM sys_notice "
            + "<if test='receiverUserId != null'> WHERE receiver_user_id = #{receiverUserId} </if>"
            + "GROUP BY notice_type ORDER BY unread_count DESC, total_count DESC LIMIT #{limit}"
            + "</script>")
    List<Map<String, Object>> selectNoticeOverview(@Param("receiverUserId") Long receiverUserId,
            @Param("limit") int limit);

    /**
     * 统计最近未读消息的来源分布。
     *
     * @param receiverUserId 接收人用户ID，为空表示统计全租户
     * @param limit          返回条数
     * @return 来源分布统计
     */
    @Select("<script>"
            + "SELECT COALESCE(NULLIF(TRIM(source), ''), '未标注') AS source_name, "
            + "COUNT(1) AS unread_count "
            + "FROM sys_notice WHERE status = '0' "
            + "<if test='receiverUserId != null'> AND receiver_user_id = #{receiverUserId} </if>"
            + "GROUP BY source_name ORDER BY unread_count DESC LIMIT #{limit}"
            + "</script>")
    List<Map<String, Object>> selectUnreadNoticeSource(@Param("receiverUserId") Long receiverUserId,
            @Param("limit") int limit);

    /**
     * 统计近期写操作趋势。
     *
     * @param sinceTime 统计起始时间
     * @return 按天聚合的操作趋势
     */
    @Select("SELECT DATE(operation_time) AS operation_date, "
            + "COUNT(1) AS total_count, "
            + "SUM(CASE WHEN success_flag = '0' THEN 1 ELSE 0 END) AS failed_count, "
            + "ROUND(AVG(cost_time), 0) AS avg_cost_ms "
            + "FROM sys_oper_log "
            + "WHERE operation_time >= #{sinceTime} "
            + "GROUP BY DATE(operation_time) ORDER BY operation_date ASC")
    List<Map<String, Object>> selectOperationTrend(@Param("sinceTime") Date sinceTime);

    /**
     * 统计近期 AI 使用量与 token 消耗。
     *
     * @param sinceTime 统计起始时间
     * @return 按天聚合的 AI 使用情况
     */
    /**
     * 统计当日 AI 用量，用于配额判定。
     *
     * @param userId    当前用户ID
     * @param sinceTime 当日起始时间
     * @return 用量统计
     */
    @Select("SELECT COUNT(1) AS tenant_request_count, "
            + "SUM(COALESCE(total_tokens, 0)) AS tenant_token_count, "
            + "SUM(CASE WHEN user_id = #{userId} THEN 1 ELSE 0 END) AS user_request_count "
            + "FROM sys_ai_audit WHERE create_time >= #{sinceTime}")
    Map<String, Object> selectQuotaUsage(@Param("userId") Long userId, @Param("sinceTime") Date sinceTime);

    @Select("SELECT DATE(create_time) AS stat_date, "
            + "COUNT(1) AS request_count, "
            + "SUM(CASE WHEN success_flag = '0' THEN 1 ELSE 0 END) AS failed_count, "
            + "SUM(COALESCE(total_tokens, 0)) AS total_tokens, "
            + "ROUND(AVG(duration_ms), 0) AS avg_duration_ms "
            + "FROM sys_ai_audit "
            + "WHERE create_time >= #{sinceTime} "
            + "GROUP BY DATE(create_time) ORDER BY stat_date ASC")
    List<Map<String, Object>> selectAiUsageTrend(@Param("sinceTime") Date sinceTime);
}

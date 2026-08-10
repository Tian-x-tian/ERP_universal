package com.erp.workflow.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * AI 只读数据集查询 Mapper。
 *
 * <p>这里只放聚合型只读统计，不承担任何写操作。租户条件由 MyBatis-Plus 租户拦截器
 * 自动补齐，因此下列语句一律不手写 tenant_id。</p>
 */
@Mapper
public interface AiDatasetMapper {

    /**
     * 按优先级统计未办结待办的积压与超期情况。
     *
     * @param assigneeUserId 办理人用户ID，为空表示统计全租户
     * @return 统计结果
     */
    @Select("<script>"
            + "SELECT priority AS priority, "
            + "COUNT(1) AS total_count, "
            + "SUM(CASE WHEN status = '0' THEN 1 ELSE 0 END) AS pending_count, "
            + "SUM(CASE WHEN status = '1' THEN 1 ELSE 0 END) AS processing_count, "
            + "SUM(CASE WHEN due_time IS NOT NULL AND due_time &lt; NOW() THEN 1 ELSE 0 END) AS overdue_count "
            + "FROM sys_todo_task "
            + "WHERE status &lt;&gt; '2' "
            + "<if test='assigneeUserId != null'> AND assignee_user_id = #{assigneeUserId} </if>"
            + "GROUP BY priority"
            + "</script>")
    List<Map<String, Object>> selectTodoBacklog(@Param("assigneeUserId") Long assigneeUserId);

    /**
     * 查询滞留时间最长的未办结待办。
     *
     * @param assigneeUserId 办理人用户ID，为空表示统计全租户
     * @param limit          返回条数
     * @return 待办列表
     */
    @Select("<script>"
            + "SELECT todo_id AS todo_id, process_name AS process_name, node_name AS node_name, "
            + "business_no AS business_no, priority AS priority, status AS status, "
            + "create_time AS create_time, due_time AS due_time, "
            + "TIMESTAMPDIFF(HOUR, create_time, NOW()) AS aging_hours "
            + "FROM sys_todo_task "
            + "WHERE status &lt;&gt; '2' AND create_time IS NOT NULL "
            + "<if test='assigneeUserId != null'> AND assignee_user_id = #{assigneeUserId} </if>"
            + "ORDER BY create_time ASC LIMIT #{limit}"
            + "</script>")
    List<Map<String, Object>> selectTodoAging(@Param("assigneeUserId") Long assigneeUserId,
            @Param("limit") int limit);

    /**
     * 统计各审批节点的办理耗时。
     *
     * @param sinceTime 统计起始时间
     * @param limit     返回条数
     * @return 节点耗时统计
     */
    @Select("SELECT node_name AS node_name, "
            + "COUNT(1) AS finished_count, "
            + "ROUND(AVG(TIMESTAMPDIFF(MINUTE, create_time, finish_time)), 1) AS avg_minutes, "
            + "MAX(TIMESTAMPDIFF(MINUTE, create_time, finish_time)) AS max_minutes "
            + "FROM sys_wf_task "
            + "WHERE finish_time IS NOT NULL AND create_time IS NOT NULL AND finish_time >= #{sinceTime} "
            + "GROUP BY node_name ORDER BY avg_minutes DESC LIMIT #{limit}")
    List<Map<String, Object>> selectApprovalDuration(@Param("sinceTime") Date sinceTime,
            @Param("limit") int limit);

    /**
     * 按流程统计实例状态分布。
     *
     * @param sinceTime 统计起始时间
     * @param limit     返回条数
     * @return 实例状态分布
     */
    @Select("SELECT process_name AS process_name, "
            + "COUNT(1) AS total_count, "
            + "SUM(CASE WHEN status = '0' THEN 1 ELSE 0 END) AS running_count, "
            + "SUM(CASE WHEN status = '1' THEN 1 ELSE 0 END) AS finished_count, "
            + "SUM(CASE WHEN status = '2' THEN 1 ELSE 0 END) AS rejected_count, "
            + "SUM(CASE WHEN status = '3' THEN 1 ELSE 0 END) AS aborted_count "
            + "FROM sys_wf_instance "
            + "WHERE start_time >= #{sinceTime} "
            + "GROUP BY process_name ORDER BY total_count DESC LIMIT #{limit}")
    List<Map<String, Object>> selectInstanceStats(@Param("sinceTime") Date sinceTime,
            @Param("limit") int limit);

    /**
     * 统计各办理人手上的在办任务量。
     *
     * @param limit 返回条数
     * @return 人员负载统计
     */
    @Select("SELECT assignee_user_id AS assignee_user_id, "
            + "MAX(assignee_nick_name) AS assignee_nick_name, "
            + "MAX(assignee_user_name) AS assignee_user_name, "
            + "COUNT(1) AS open_task_count, "
            + "SUM(CASE WHEN due_time IS NOT NULL AND due_time < NOW() THEN 1 ELSE 0 END) AS overdue_count "
            + "FROM sys_wf_task "
            + "WHERE status IN ('0', '1') AND assignee_user_id IS NOT NULL "
            + "GROUP BY assignee_user_id ORDER BY open_task_count DESC LIMIT #{limit}")
    List<Map<String, Object>> selectUserWorkload(@Param("limit") int limit);

    /**
     * 统计近期审批动作趋势。
     *
     * @param sinceTime 统计起始时间
     * @return 按天聚合的动作趋势
     */
    @Select("SELECT DATE(action_time) AS action_date, "
            + "COUNT(1) AS total_count, "
            + "SUM(CASE WHEN action_type = 'approve' THEN 1 ELSE 0 END) AS approve_count, "
            + "SUM(CASE WHEN action_type = 'reject' THEN 1 ELSE 0 END) AS reject_count "
            + "FROM sys_wf_task_action "
            + "WHERE action_time >= #{sinceTime} "
            + "GROUP BY DATE(action_time) ORDER BY action_date ASC")
    List<Map<String, Object>> selectApprovalTrend(@Param("sinceTime") Date sinceTime);
}

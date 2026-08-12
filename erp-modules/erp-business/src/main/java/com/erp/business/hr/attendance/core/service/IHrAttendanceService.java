package com.erp.business.hr.attendance.core.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.erp.business.hr.attendance.core.domain.HrAttendanceLeaveOrder;
import com.erp.business.hr.attendance.core.domain.HrAttendanceOvertimeOrder;
import com.erp.business.hr.attendance.core.domain.vo.HrAttendanceCompanySummaryVO;
import com.erp.business.hr.attendance.core.domain.vo.HrAttendanceDashboardVO;
import com.erp.business.hr.attendance.core.domain.vo.HrAttendanceDeptSummaryQuery;
import com.erp.business.hr.attendance.core.domain.vo.HrAttendanceDeptSummaryVO;
import com.erp.business.hr.attendance.core.domain.vo.HrAttendanceExternalRecordBody;
import com.erp.business.hr.attendance.core.domain.vo.HrAttendanceLeaveBody;
import com.erp.business.hr.attendance.core.domain.vo.HrAttendanceLeaveQuery;
import com.erp.business.hr.attendance.core.domain.vo.HrAttendanceOvertimeBody;
import com.erp.business.hr.attendance.core.domain.vo.HrAttendanceOvertimeQuery;
import com.erp.business.hr.attendance.core.domain.vo.HrAttendancePersonalDayVO;
import com.erp.business.hr.attendance.core.domain.vo.HrAttendancePersonalMonthVO;
import com.erp.business.hr.attendance.core.domain.vo.HrAttendancePersonalSignBody;

import java.util.List;

/**
 * 出勤核心服务接口。
 */
public interface IHrAttendanceService {

    /**
     * 构建出勤工作台数据。
     *
     * @return 工作台数据
     */
    HrAttendanceDashboardVO getDashboard();

    /**
     * 执行个人签到。
     *
     * @param body 签到参数
     * @return 当日出勤视图
     */
    HrAttendancePersonalDayVO signIn(HrAttendancePersonalSignBody body);

    /**
     * 执行个人签退。
     *
     * @param body 签退参数
     * @return 当日出勤视图
     */
    HrAttendancePersonalDayVO signOut(HrAttendancePersonalSignBody body);

    /**
     * 查询个人日出勤。
     *
     * @param workDate 工作日期
     * @return 当日出勤视图
     */
    HrAttendancePersonalDayVO getPersonalDay(String workDate);

    /**
     * 查询个人月出勤。
     *
     * @param monthCode 月份编码
     * @return 月出勤视图
     */
    HrAttendancePersonalMonthVO getPersonalMonth(String monthCode);

    /**
     * 查询部门汇总。
     *
     * @param query 查询参数
     * @return 汇总列表
     */
    List<HrAttendanceDeptSummaryVO> listDeptSummary(HrAttendanceDeptSummaryQuery query);

    /**
     * 查询公司级汇总。
     *
     * @param monthCode 月份编码
     * @return 公司汇总
     */
    HrAttendanceCompanySummaryVO getCompanySummary(String monthCode);

    /**
     * 分页查询请假单。
     *
     * @param query 查询参数
     * @return 分页结果
     */
    Page<HrAttendanceLeaveOrder> selectLeavePage(HrAttendanceLeaveQuery query);

    /**
     * 新增请假单草稿。
     *
     * @param body 请假参数
     * @return 请假单
     */
    HrAttendanceLeaveOrder saveLeave(HrAttendanceLeaveBody body);

    /**
     * 更新请假单草稿。
     *
     * @param orderId 单据ID
     * @param body 请假参数
     * @return 请假单
     */
    HrAttendanceLeaveOrder updateLeave(Long orderId, HrAttendanceLeaveBody body);

    /**
     * 提交请假单审批。
     *
     * @param orderId 单据ID
     * @return 最新请假单
     */
    HrAttendanceLeaveOrder submitLeave(Long orderId);

    /**
     * 分页查询加班单。
     *
     * @param query 查询参数
     * @return 分页结果
     */
    Page<HrAttendanceOvertimeOrder> selectOvertimePage(HrAttendanceOvertimeQuery query);

    /**
     * 新增加班单草稿。
     *
     * @param body 加班参数
     * @return 加班单
     */
    HrAttendanceOvertimeOrder saveOvertime(HrAttendanceOvertimeBody body);

    /**
     * 更新加班单草稿。
     *
     * @param orderId 单据ID
     * @param body 加班参数
     * @return 加班单
     */
    HrAttendanceOvertimeOrder updateOvertime(Long orderId, HrAttendanceOvertimeBody body);

    /**
     * 提交加班单审批。
     *
     * @param orderId 单据ID
     * @return 最新加班单
     */
    HrAttendanceOvertimeOrder submitOvertime(Long orderId);

    /**
     * 导入第三方出勤记录并重算汇总。
     *
     * @param body 第三方出勤参数
     */
    void importExternalRecord(HrAttendanceExternalRecordBody body);
}

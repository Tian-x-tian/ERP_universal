package com.erp.business.hr.attendance.core.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.erp.business.hr.attendance.core.domain.HrAttendanceLeaveOrder;
import com.erp.business.hr.attendance.core.domain.HrAttendanceOvertimeOrder;
import com.erp.business.hr.attendance.core.domain.vo.HrAttendanceCompanySummaryVO;
import com.erp.business.hr.attendance.core.domain.vo.HrAttendanceDashboardVO;
import com.erp.business.hr.attendance.core.domain.vo.HrAttendanceDeptSummaryQuery;
import com.erp.business.hr.attendance.core.domain.vo.HrAttendanceDeptSummaryVO;
import com.erp.business.hr.attendance.core.domain.vo.HrAttendanceLeaveBody;
import com.erp.business.hr.attendance.core.domain.vo.HrAttendanceLeaveQuery;
import com.erp.business.hr.attendance.core.domain.vo.HrAttendanceOvertimeBody;
import com.erp.business.hr.attendance.core.domain.vo.HrAttendanceOvertimeQuery;
import com.erp.business.hr.attendance.core.domain.vo.HrAttendancePersonalDayVO;
import com.erp.business.hr.attendance.core.domain.vo.HrAttendancePersonalMonthVO;
import com.erp.business.hr.attendance.core.domain.vo.HrAttendancePersonalSignBody;
import com.erp.business.hr.attendance.core.service.IHrAttendanceService;
import com.erp.common.core.domain.PageData;
import com.erp.common.core.domain.R;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 出勤核心控制层。
 */
@RestController
@RequestMapping("/business/hr/attendance")
public class HrAttendanceController {

    private final IHrAttendanceService attendanceService;

    public HrAttendanceController(IHrAttendanceService attendanceService) {
        this.attendanceService = attendanceService;
    }

    /**
     * 查询出勤工作台数据。
     *
     * @return 工作台数据
     */
    @GetMapping("/dashboard")
    @PreAuthorize("@ss.hasPermi('business:hr:attendance:list')")
    public R<HrAttendanceDashboardVO> dashboard() {
        return R.success(attendanceService.getDashboard());
    }

    /**
     * 执行个人签到。
     *
     * @param body 签到参数
     * @return 当日出勤视图
     */
    @PostMapping("/personal/sign-in")
    @PreAuthorize("@ss.hasPermi('business:hr:attendance:sign')")
    public R<HrAttendancePersonalDayVO> signIn(@RequestBody HrAttendancePersonalSignBody body) {
        return R.success(attendanceService.signIn(body));
    }

    /**
     * 执行个人签退。
     *
     * @param body 签退参数
     * @return 当日出勤视图
     */
    @PostMapping("/personal/sign-out")
    @PreAuthorize("@ss.hasPermi('business:hr:attendance:sign')")
    public R<HrAttendancePersonalDayVO> signOut(@RequestBody HrAttendancePersonalSignBody body) {
        return R.success(attendanceService.signOut(body));
    }

    /**
     * 查询个人日出勤。
     *
     * @param workDate 工作日期
     * @return 当日出勤视图
     */
    @GetMapping("/personal/day")
    @PreAuthorize("@ss.hasPermi('business:hr:attendance:personal')")
    public R<HrAttendancePersonalDayVO> personalDay(@RequestParam(value = "workDate", required = false) String workDate) {
        return R.success(attendanceService.getPersonalDay(workDate));
    }

    /**
     * 查询个人月出勤。
     *
     * @param month 月份编码
     * @return 月出勤视图
     */
    @GetMapping("/personal/month")
    @PreAuthorize("@ss.hasPermi('business:hr:attendance:personal')")
    public R<HrAttendancePersonalMonthVO> personalMonth(@RequestParam(value = "month", required = false) String month) {
        return R.success(attendanceService.getPersonalMonth(month));
    }

    /**
     * 查询部门汇总。
     *
     * @param query 查询参数
     * @return 汇总列表
     */
    @GetMapping("/dept/summary")
    @PreAuthorize("@ss.hasPermi('business:hr:attendance:dept')")
    public R<List<HrAttendanceDeptSummaryVO>> deptSummary(HrAttendanceDeptSummaryQuery query) {
        return R.success(attendanceService.listDeptSummary(query));
    }

    /**
     * 查询公司级汇总。
     *
     * @param month 月份编码
     * @return 公司汇总
     */
    @GetMapping("/company/summary")
    @PreAuthorize("@ss.hasPermi('business:hr:attendance:company')")
    public R<HrAttendanceCompanySummaryVO> companySummary(@RequestParam(value = "month", required = false) String month) {
        return R.success(attendanceService.getCompanySummary(month));
    }

    /**
     * 分页查询请假单。
     *
     * @param query 查询参数
     * @return 分页结果
     */
    @GetMapping("/leave")
    @PreAuthorize("@ss.hasPermi('business:hr:attendance:leave')")
    public R<PageData<HrAttendanceLeaveOrder>> leave(HrAttendanceLeaveQuery query) {
        Page<HrAttendanceLeaveOrder> page = attendanceService.selectLeavePage(query);
        return R.page(page.getRecords(), page.getCurrent(), page.getSize(), page.getTotal());
    }

    /**
     * 保存请假单草稿。
     *
     * @param body 请假参数
     * @return 请假单
     */
    @PostMapping("/leave")
    @PreAuthorize("@ss.hasPermi('business:hr:attendance:leave')")
    public R<HrAttendanceLeaveOrder> saveLeave(@RequestBody HrAttendanceLeaveBody body) {
        return R.success(attendanceService.saveLeave(body));
    }

    /**
     * 更新请假单草稿。
     *
     * @param orderId 单据ID
     * @param body 请假参数
     * @return 请假单
     */
    @PutMapping("/leave/{orderId}")
    @PreAuthorize("@ss.hasPermi('business:hr:attendance:leave')")
    public R<HrAttendanceLeaveOrder> updateLeave(@PathVariable("orderId") Long orderId,
            @RequestBody HrAttendanceLeaveBody body) {
        return R.success(attendanceService.updateLeave(orderId, body));
    }

    /**
     * 提交请假审批。
     *
     * @param orderId 单据ID
     * @return 最新请假单
     */
    @PostMapping("/leave/submit/{orderId}")
    @PreAuthorize("@ss.hasPermi('business:hr:attendance:leave:submit')")
    public R<HrAttendanceLeaveOrder> submitLeave(@PathVariable("orderId") Long orderId) {
        return R.success(attendanceService.submitLeave(orderId));
    }

    /**
     * 分页查询加班单。
     *
     * @param query 查询参数
     * @return 分页结果
     */
    @GetMapping("/overtime")
    @PreAuthorize("@ss.hasPermi('business:hr:attendance:overtime')")
    public R<PageData<HrAttendanceOvertimeOrder>> overtime(HrAttendanceOvertimeQuery query) {
        Page<HrAttendanceOvertimeOrder> page = attendanceService.selectOvertimePage(query);
        return R.page(page.getRecords(), page.getCurrent(), page.getSize(), page.getTotal());
    }

    /**
     * 保存加班单草稿。
     *
     * @param body 加班参数
     * @return 加班单
     */
    @PostMapping("/overtime")
    @PreAuthorize("@ss.hasPermi('business:hr:attendance:overtime')")
    public R<HrAttendanceOvertimeOrder> saveOvertime(@RequestBody HrAttendanceOvertimeBody body) {
        return R.success(attendanceService.saveOvertime(body));
    }

    /**
     * 更新加班单草稿。
     *
     * @param orderId 单据ID
     * @param body 加班参数
     * @return 加班单
     */
    @PutMapping("/overtime/{orderId}")
    @PreAuthorize("@ss.hasPermi('business:hr:attendance:overtime')")
    public R<HrAttendanceOvertimeOrder> updateOvertime(@PathVariable("orderId") Long orderId,
            @RequestBody HrAttendanceOvertimeBody body) {
        return R.success(attendanceService.updateOvertime(orderId, body));
    }

    /**
     * 提交加班审批。
     *
     * @param orderId 单据ID
     * @return 最新加班单
     */
    @PostMapping("/overtime/submit/{orderId}")
    @PreAuthorize("@ss.hasPermi('business:hr:attendance:overtime:submit')")
    public R<HrAttendanceOvertimeOrder> submitOvertime(@PathVariable("orderId") Long orderId) {
        return R.success(attendanceService.submitOvertime(orderId));
    }
}

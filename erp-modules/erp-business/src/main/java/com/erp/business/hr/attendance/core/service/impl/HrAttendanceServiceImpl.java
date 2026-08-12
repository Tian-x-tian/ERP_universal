package com.erp.business.hr.attendance.core.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.erp.business.hr.attendance.core.domain.HrAttendanceDaySummary;
import com.erp.business.hr.attendance.core.domain.HrAttendanceException;
import com.erp.business.hr.attendance.core.domain.HrAttendanceLeaveOrder;
import com.erp.business.hr.attendance.core.domain.HrAttendanceLocationRule;
import com.erp.business.hr.attendance.core.domain.HrAttendanceMonthSummary;
import com.erp.business.hr.attendance.core.domain.HrAttendanceOvertimeOrder;
import com.erp.business.hr.attendance.core.domain.HrAttendanceRecord;
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
import com.erp.business.hr.attendance.core.mapper.HrAttendanceDaySummaryMapper;
import com.erp.business.hr.attendance.core.mapper.HrAttendanceExceptionMapper;
import com.erp.business.hr.attendance.core.mapper.HrAttendanceLeaveOrderMapper;
import com.erp.business.hr.attendance.core.mapper.HrAttendanceLocationRuleMapper;
import com.erp.business.hr.attendance.core.mapper.HrAttendanceMonthSummaryMapper;
import com.erp.business.hr.attendance.core.mapper.HrAttendanceOvertimeOrderMapper;
import com.erp.business.hr.attendance.core.mapper.HrAttendanceRecordMapper;
import com.erp.business.hr.attendance.core.service.AttendanceWorkflowGateway;
import com.erp.business.hr.attendance.core.service.IHrAttendanceAggregationService;
import com.erp.business.hr.attendance.core.service.IHrAttendanceService;
import com.erp.business.hr.attendance.core.support.HrAttendanceSupport;
import com.erp.business.hr.domain.HrEmployeeCore;
import com.erp.business.hr.mapper.HrEmployeeCoreMapper;
import com.erp.business.hr.support.HrEmployeeSupport;
import com.erp.business.security.service.SecurityUserResolver;
import com.erp.common.client.internal.InternalPlatformClient;
import com.erp.common.core.domain.ResultCode;
import com.erp.common.core.exception.ServiceException;
import com.erp.platform.contract.model.PlatformDeptView;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 出勤核心服务实现。
 */
@Service
public class HrAttendanceServiceImpl implements IHrAttendanceService {

    private final HrAttendanceRecordMapper recordMapper;
    private final HrAttendanceDaySummaryMapper daySummaryMapper;
    private final HrAttendanceMonthSummaryMapper monthSummaryMapper;
    private final HrAttendanceExceptionMapper exceptionMapper;
    private final HrAttendanceLocationRuleMapper locationRuleMapper;
    private final HrAttendanceLeaveOrderMapper leaveOrderMapper;
    private final HrAttendanceOvertimeOrderMapper overtimeOrderMapper;
    private final HrEmployeeCoreMapper employeeCoreMapper;
    private final SecurityUserResolver securityUserResolver;
    private final InternalPlatformClient internalPlatformClient;
    private final IHrAttendanceAggregationService aggregationService;
    private final AttendanceWorkflowGateway workflowGateway;

    public HrAttendanceServiceImpl(HrAttendanceRecordMapper recordMapper,
            HrAttendanceDaySummaryMapper daySummaryMapper,
            HrAttendanceMonthSummaryMapper monthSummaryMapper,
            HrAttendanceExceptionMapper exceptionMapper,
            HrAttendanceLocationRuleMapper locationRuleMapper,
            HrAttendanceLeaveOrderMapper leaveOrderMapper,
            HrAttendanceOvertimeOrderMapper overtimeOrderMapper,
            HrEmployeeCoreMapper employeeCoreMapper,
            SecurityUserResolver securityUserResolver,
            InternalPlatformClient internalPlatformClient,
            IHrAttendanceAggregationService aggregationService,
            AttendanceWorkflowGateway workflowGateway) {
        this.recordMapper = recordMapper;
        this.daySummaryMapper = daySummaryMapper;
        this.monthSummaryMapper = monthSummaryMapper;
        this.exceptionMapper = exceptionMapper;
        this.locationRuleMapper = locationRuleMapper;
        this.leaveOrderMapper = leaveOrderMapper;
        this.overtimeOrderMapper = overtimeOrderMapper;
        this.employeeCoreMapper = employeeCoreMapper;
        this.securityUserResolver = securityUserResolver;
        this.internalPlatformClient = internalPlatformClient;
        this.aggregationService = aggregationService;
        this.workflowGateway = workflowGateway;
    }

    /**
     * 构建出勤工作台数据。
     *
     * @return 工作台数据
     */
    @Override
    public HrAttendanceDashboardVO getDashboard() {
        HrAttendanceDashboardVO dashboardVO = new HrAttendanceDashboardVO();
        LocalDate today = LocalDate.now();
        String monthCode = HrAttendanceSupport.monthCode(today);
        dashboardVO.setToday(getPersonalDay(today.toString()));
        dashboardVO.setPersonalMonth(getPersonalMonth(monthCode).getSummary());
        dashboardVO.setCompanySummary(getCompanySummary(monthCode));
        HrEmployeeCore employee = requireCurrentEmployee();
        long pendingExceptionCount = exceptionMapper.selectCount(new LambdaQueryWrapper<HrAttendanceException>()
                .eq(HrAttendanceException::getEmployeeId, employee.getEmployeeId())
                .ge(HrAttendanceException::getWorkDate, HrAttendanceSupport.toDate(today.withDayOfMonth(1)))
                .le(HrAttendanceException::getWorkDate, HrAttendanceSupport.toDate(today.withDayOfMonth(today.lengthOfMonth()))));
        dashboardVO.setPendingExceptionCount(pendingExceptionCount);
        return dashboardVO;
    }

    /**
     * 执行个人签到。
     *
     * @param body 签到参数
     * @return 当日出勤视图
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public HrAttendancePersonalDayVO signIn(HrAttendancePersonalSignBody body) {
        HrEmployeeCore employee = requireCurrentEmployee();
        LocalDateTime signTime = body != null && body.getSignTime() != null ? body.getSignTime() : LocalDateTime.now();
        HrAttendanceRecord record = loadRecord(employee.getEmployeeId(), signTime.toLocalDate(), HrAttendanceSupport.SOURCE_INTERNAL);
        boolean newRecord = record == null;
        if (newRecord) {
            record = new HrAttendanceRecord();
            record.setTenantId(employee.getTenantId());
            record.setEmployeeId(employee.getEmployeeId());
            record.setOrgId(employee.getOrgId());
            record.setDeptId(employee.getDeptId());
            record.setWorkDate(HrAttendanceSupport.toDate(signTime.toLocalDate()));
            record.setSourceType(HrAttendanceSupport.SOURCE_INTERNAL);
            record.setAuthorityFlag(HrAttendanceSupport.FLAG_NO);
        }
        record.setSignInTime(HrAttendanceSupport.toDate(signTime));
        record.setSignInLatitude(body == null ? null : body.getLatitude());
        record.setSignInLongitude(body == null ? null : body.getLongitude());
        record.setSignInAddress(body == null ? null : HrAttendanceSupport.trimToNull(body.getAddress()));
        record.setDeviceSource(body == null ? null : HrAttendanceSupport.trimToNull(body.getDeviceSource()));
        record.setRemark(body == null ? null : HrAttendanceSupport.trimToNull(body.getRemark()));
        record.setSignInInRange(resolveRangeFlag(employee, body == null ? null : body.getLatitude(), body == null ? null : body.getLongitude()));
        if (newRecord) {
            recordMapper.insert(record);
        } else {
            recordMapper.updateById(record);
        }
        aggregationService.recalculateEmployeeDay(employee.getEmployeeId(), signTime.toLocalDate(), resolveOperator());
        aggregationService.recalculateEmployeeMonth(employee.getEmployeeId(), HrAttendanceSupport.monthCode(signTime.toLocalDate()), resolveOperator());
        return getPersonalDay(signTime.toLocalDate().toString());
    }

    /**
     * 执行个人签退。
     *
     * @param body 签退参数
     * @return 当日出勤视图
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public HrAttendancePersonalDayVO signOut(HrAttendancePersonalSignBody body) {
        HrEmployeeCore employee = requireCurrentEmployee();
        LocalDateTime signTime = body != null && body.getSignTime() != null ? body.getSignTime() : LocalDateTime.now();
        HrAttendanceRecord record = loadRecord(employee.getEmployeeId(), signTime.toLocalDate(), HrAttendanceSupport.SOURCE_INTERNAL);
        boolean newRecord = record == null;
        if (newRecord) {
            record = new HrAttendanceRecord();
            record.setTenantId(employee.getTenantId());
            record.setEmployeeId(employee.getEmployeeId());
            record.setOrgId(employee.getOrgId());
            record.setDeptId(employee.getDeptId());
            record.setWorkDate(HrAttendanceSupport.toDate(signTime.toLocalDate()));
            record.setSourceType(HrAttendanceSupport.SOURCE_INTERNAL);
            record.setAuthorityFlag(HrAttendanceSupport.FLAG_NO);
        }
        record.setSignOutTime(HrAttendanceSupport.toDate(signTime));
        record.setSignOutLatitude(body == null ? null : body.getLatitude());
        record.setSignOutLongitude(body == null ? null : body.getLongitude());
        record.setSignOutAddress(body == null ? null : HrAttendanceSupport.trimToNull(body.getAddress()));
        record.setDeviceSource(body == null ? null : HrAttendanceSupport.trimToNull(body.getDeviceSource()));
        record.setRemark(body == null ? null : HrAttendanceSupport.trimToNull(body.getRemark()));
        record.setSignOutInRange(resolveRangeFlag(employee, body == null ? null : body.getLatitude(), body == null ? null : body.getLongitude()));
        if (newRecord) {
            recordMapper.insert(record);
        } else {
            recordMapper.updateById(record);
        }
        aggregationService.recalculateEmployeeDay(employee.getEmployeeId(), signTime.toLocalDate(), resolveOperator());
        aggregationService.recalculateEmployeeMonth(employee.getEmployeeId(), HrAttendanceSupport.monthCode(signTime.toLocalDate()), resolveOperator());
        return getPersonalDay(signTime.toLocalDate().toString());
    }

    /**
     * 查询个人日出勤。
     *
     * @param workDate 工作日期
     * @return 当日出勤视图
     */
    @Override
    public HrAttendancePersonalDayVO getPersonalDay(String workDate) {
        HrEmployeeCore employee = requireCurrentEmployee();
        LocalDate localDate = parseWorkDate(workDate);
        HrAttendanceDaySummary summary = daySummaryMapper.selectOne(new LambdaQueryWrapper<HrAttendanceDaySummary>()
                .eq(HrAttendanceDaySummary::getEmployeeId, employee.getEmployeeId())
                .eq(HrAttendanceDaySummary::getWorkDate, HrAttendanceSupport.toDate(localDate)));
        List<HrAttendanceRecord> recordList = recordMapper.selectList(new LambdaQueryWrapper<HrAttendanceRecord>()
                .eq(HrAttendanceRecord::getEmployeeId, employee.getEmployeeId())
                .eq(HrAttendanceRecord::getWorkDate, HrAttendanceSupport.toDate(localDate)));
        List<HrAttendanceException> exceptionList = exceptionMapper.selectList(new LambdaQueryWrapper<HrAttendanceException>()
                .eq(HrAttendanceException::getEmployeeId, employee.getEmployeeId())
                .eq(HrAttendanceException::getWorkDate, HrAttendanceSupport.toDate(localDate)));
        HrAttendancePersonalDayVO dayVO = new HrAttendancePersonalDayVO();
        dayVO.setSummary(summary);
        dayVO.setRecords(recordList);
        dayVO.setExceptions(exceptionList);
        dayVO.setSignedIn(recordList != null && recordList.stream().anyMatch(item -> item.getSignInTime() != null));
        dayVO.setSignedOut(recordList != null && recordList.stream().anyMatch(item -> item.getSignOutTime() != null));
        return dayVO;
    }

    /**
     * 查询个人月出勤。
     *
     * @param monthCode 月份编码
     * @return 月出勤视图
     */
    @Override
    public HrAttendancePersonalMonthVO getPersonalMonth(String monthCode) {
        HrEmployeeCore employee = requireCurrentEmployee();
        String normalizedMonthCode = HrAttendanceSupport.normalizeMonthCode(monthCode);
        if (normalizedMonthCode == null) {
            normalizedMonthCode = HrAttendanceSupport.monthCode(LocalDate.now());
        }
        HrAttendanceMonthSummary summary = monthSummaryMapper.selectOne(new LambdaQueryWrapper<HrAttendanceMonthSummary>()
                .eq(HrAttendanceMonthSummary::getEmployeeId, employee.getEmployeeId())
                .eq(HrAttendanceMonthSummary::getMonthCode, normalizedMonthCode));
        List<HrAttendanceDaySummary> dayList = daySummaryMapper.selectList(new LambdaQueryWrapper<HrAttendanceDaySummary>()
                .eq(HrAttendanceDaySummary::getEmployeeId, employee.getEmployeeId())
                .eq(HrAttendanceDaySummary::getMonthCode, normalizedMonthCode)
                .orderByDesc(HrAttendanceDaySummary::getWorkDate));
        HrAttendancePersonalMonthVO monthVO = new HrAttendancePersonalMonthVO();
        monthVO.setSummary(summary);
        monthVO.setDayList(dayList);
        return monthVO;
    }

    /**
     * 查询部门汇总。
     *
     * @param query 查询参数
     * @return 汇总列表
     */
    @Override
    public List<HrAttendanceDeptSummaryVO> listDeptSummary(HrAttendanceDeptSummaryQuery query) {
        HrEmployeeCore employee = requireCurrentEmployee();
        HrAttendanceDeptSummaryQuery safeQuery = query == null ? new HrAttendanceDeptSummaryQuery() : query;
        boolean includeChildren = safeQuery.getIncludeChildren() == null || safeQuery.getIncludeChildren();
        List<PlatformDeptView> departmentList = listTenantDepartments();
        Set<Long> scopedDeptIds = resolveDeptScope(safeQuery.getDeptId() == null ? employee.getDeptId() : safeQuery.getDeptId(),
                includeChildren, departmentList);
        Map<Long, PlatformDeptView> departmentMap = departmentList.stream()
                .collect(Collectors.toMap(PlatformDeptView::getDeptId, item -> item, (left, right) -> left));
        if (StringUtils.hasText(safeQuery.getDate())) {
            LocalDate workDate = parseWorkDate(safeQuery.getDate());
            List<HrAttendanceDaySummary> summaryList = daySummaryMapper.selectList(new LambdaQueryWrapper<HrAttendanceDaySummary>()
                    .eq(HrAttendanceDaySummary::getWorkDate, HrAttendanceSupport.toDate(workDate))
                    .in(!scopedDeptIds.isEmpty(), HrAttendanceDaySummary::getDeptId, scopedDeptIds));
            return buildDeptSummaryByDay(summaryList, departmentMap, workDate.toString());
        }
        String monthCode = HrAttendanceSupport.normalizeMonthCode(safeQuery.getMonth());
        if (monthCode == null) {
            monthCode = HrAttendanceSupport.monthCode(LocalDate.now());
        }
        List<HrAttendanceMonthSummary> summaryList = monthSummaryMapper.selectList(new LambdaQueryWrapper<HrAttendanceMonthSummary>()
                .eq(HrAttendanceMonthSummary::getMonthCode, monthCode)
                .in(!scopedDeptIds.isEmpty(), HrAttendanceMonthSummary::getDeptId, scopedDeptIds));
        return buildDeptSummaryByMonth(summaryList, departmentMap, monthCode);
    }

    /**
     * 查询公司级汇总。
     *
     * @param monthCode 月份编码
     * @return 公司汇总
     */
    @Override
    public HrAttendanceCompanySummaryVO getCompanySummary(String monthCode) {
        String normalizedMonthCode = HrAttendanceSupport.normalizeMonthCode(monthCode);
        if (normalizedMonthCode == null) {
            normalizedMonthCode = HrAttendanceSupport.monthCode(LocalDate.now());
        }
        List<HrAttendanceMonthSummary> summaryList = monthSummaryMapper.selectList(new LambdaQueryWrapper<HrAttendanceMonthSummary>()
                .eq(HrAttendanceMonthSummary::getMonthCode, normalizedMonthCode));
        HrAttendanceCompanySummaryVO companySummaryVO = new HrAttendanceCompanySummaryVO();
        companySummaryVO.setDateLabel(normalizedMonthCode);
        companySummaryVO.setDeptCount(summaryList.stream().map(HrAttendanceMonthSummary::getDeptId).filter(item -> item != null).distinct().count());
        companySummaryVO.setEmployeeCount(summaryList.stream().map(HrAttendanceMonthSummary::getEmployeeId).filter(item -> item != null).distinct().count());
        companySummaryVO.setAttendanceDays(summaryList.stream().map(HrAttendanceMonthSummary::getAttendanceDays)
                .reduce(BigDecimal.ZERO, BigDecimal::add));
        companySummaryVO.setActualMinutes(summaryList.stream().mapToInt(item -> nullSafeInt(item.getActualMinutes())).sum());
        companySummaryVO.setLeaveMinutes(summaryList.stream().mapToInt(item -> nullSafeInt(item.getLeaveMinutes())).sum());
        companySummaryVO.setLeaveDays(summaryList.stream().map(HrAttendanceMonthSummary::getLeaveDays)
                .reduce(BigDecimal.ZERO, BigDecimal::add));
        companySummaryVO.setOvertimeMinutes(summaryList.stream().mapToInt(item -> nullSafeInt(item.getOvertimeMinutes())).sum());
        companySummaryVO.setLateCount(summaryList.stream().mapToInt(item -> nullSafeInt(item.getLateCount())).sum());
        companySummaryVO.setEarlyLeaveCount(summaryList.stream().mapToInt(item -> nullSafeInt(item.getEarlyLeaveCount())).sum());
        companySummaryVO.setMissingCardCount(summaryList.stream().mapToInt(item -> nullSafeInt(item.getMissingCardCount())).sum());
        companySummaryVO.setAbsenteeismDays(summaryList.stream().map(HrAttendanceMonthSummary::getAbsenteeismDays)
                .reduce(BigDecimal.ZERO, BigDecimal::add));
        companySummaryVO.setAbnormalCount(summaryList.stream().mapToInt(item -> nullSafeInt(item.getAbnormalCount())).sum());
        return companySummaryVO;
    }

    /**
     * 分页查询请假单。
     *
     * @param query 查询参数
     * @return 分页结果
     */
    @Override
    public Page<HrAttendanceLeaveOrder> selectLeavePage(HrAttendanceLeaveQuery query) {
        HrAttendanceLeaveQuery safeQuery = query == null ? new HrAttendanceLeaveQuery() : query;
        Long employeeId = safeQuery.getEmployeeId() == null ? requireCurrentEmployee().getEmployeeId() : safeQuery.getEmployeeId();
        Page<HrAttendanceLeaveOrder> page = new Page<>(
                HrAttendanceSupport.normalizePageNum(safeQuery.getPageNum()),
                HrAttendanceSupport.normalizePageSize(safeQuery.getPageSize()));
        return leaveOrderMapper.selectPage(page, new LambdaQueryWrapper<HrAttendanceLeaveOrder>()
                .eq(HrAttendanceLeaveOrder::getEmployeeId, employeeId)
                .eq(StringUtils.hasText(safeQuery.getStatus()), HrAttendanceLeaveOrder::getStatus,
                        HrAttendanceSupport.normalizeStatus(safeQuery.getStatus()))
                .ge(safeQuery.getBeginDate() != null, HrAttendanceLeaveOrder::getStartTime, HrAttendanceSupport.toDate(safeQuery.getBeginDate()))
                .le(safeQuery.getEndDate() != null, HrAttendanceLeaveOrder::getEndTime, HrAttendanceSupport.toDate(safeQuery.getEndDate().plusDays(1)))
                .orderByDesc(HrAttendanceLeaveOrder::getCreateTime));
    }

    /**
     * 新增请假单草稿。
     *
     * @param body 请假参数
     * @return 请假单
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public HrAttendanceLeaveOrder saveLeave(HrAttendanceLeaveBody body) {
        HrEmployeeCore employee = requireCurrentEmployee();
        HrAttendanceLeaveOrder order = new HrAttendanceLeaveOrder();
        fillLeaveOrder(order, body, employee);
        order.setOrderNo(HrAttendanceSupport.generateOrderNo("AL", LocalDateTime.now()));
        order.setStatus(HrAttendanceSupport.ORDER_STATUS_DRAFT);
        order.setProcessKey(HrAttendanceSupport.WORKFLOW_PROCESS_KEY_LEAVE);
        leaveOrderMapper.insert(order);
        return leaveOrderMapper.selectById(order.getOrderId());
    }

    /**
     * 更新请假单草稿。
     *
     * @param orderId 单据ID
     * @param body 请假参数
     * @return 请假单
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public HrAttendanceLeaveOrder updateLeave(Long orderId, HrAttendanceLeaveBody body) {
        HrAttendanceLeaveOrder existing = requireLeaveOrder(orderId);
        if (!HrAttendanceSupport.ORDER_STATUS_DRAFT.equals(existing.getStatus())) {
            throw new ServiceException("仅草稿状态允许修改请假单", (int) ResultCode.CONFLICT.getCode());
        }
        fillLeaveOrder(existing, body, requireCurrentEmployee());
        leaveOrderMapper.updateById(existing);
        return leaveOrderMapper.selectById(orderId);
    }

    /**
     * 提交请假单审批。
     *
     * @param orderId 单据ID
     * @return 最新请假单
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public HrAttendanceLeaveOrder submitLeave(Long orderId) {
        HrAttendanceLeaveOrder order = requireLeaveOrder(orderId);
        if (!HrAttendanceSupport.ORDER_STATUS_DRAFT.equals(order.getStatus())) {
            throw new ServiceException("仅草稿状态允许提交请假审批", (int) ResultCode.CONFLICT.getCode());
        }
        order.setStatus(HrAttendanceSupport.ORDER_STATUS_SUBMITTED);
        leaveOrderMapper.updateById(order);
        if (!workflowGateway.startLeaveWorkflow(order)) {
            throw new ServiceException("请假流程发起失败", (int) ResultCode.ERROR.getCode());
        }
        return leaveOrderMapper.selectById(orderId);
    }

    /**
     * 分页查询加班单。
     *
     * @param query 查询参数
     * @return 分页结果
     */
    @Override
    public Page<HrAttendanceOvertimeOrder> selectOvertimePage(HrAttendanceOvertimeQuery query) {
        HrAttendanceOvertimeQuery safeQuery = query == null ? new HrAttendanceOvertimeQuery() : query;
        Long employeeId = safeQuery.getEmployeeId() == null ? requireCurrentEmployee().getEmployeeId() : safeQuery.getEmployeeId();
        Page<HrAttendanceOvertimeOrder> page = new Page<>(
                HrAttendanceSupport.normalizePageNum(safeQuery.getPageNum()),
                HrAttendanceSupport.normalizePageSize(safeQuery.getPageSize()));
        return overtimeOrderMapper.selectPage(page, new LambdaQueryWrapper<HrAttendanceOvertimeOrder>()
                .eq(HrAttendanceOvertimeOrder::getEmployeeId, employeeId)
                .eq(StringUtils.hasText(safeQuery.getStatus()), HrAttendanceOvertimeOrder::getStatus,
                        HrAttendanceSupport.normalizeStatus(safeQuery.getStatus()))
                .ge(safeQuery.getBeginDate() != null, HrAttendanceOvertimeOrder::getStartTime, HrAttendanceSupport.toDate(safeQuery.getBeginDate()))
                .le(safeQuery.getEndDate() != null, HrAttendanceOvertimeOrder::getEndTime, HrAttendanceSupport.toDate(safeQuery.getEndDate().plusDays(1)))
                .orderByDesc(HrAttendanceOvertimeOrder::getCreateTime));
    }

    /**
     * 新增加班单草稿。
     *
     * @param body 加班参数
     * @return 加班单
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public HrAttendanceOvertimeOrder saveOvertime(HrAttendanceOvertimeBody body) {
        HrEmployeeCore employee = requireCurrentEmployee();
        HrAttendanceOvertimeOrder order = new HrAttendanceOvertimeOrder();
        fillOvertimeOrder(order, body, employee);
        order.setOrderNo(HrAttendanceSupport.generateOrderNo("AO", LocalDateTime.now()));
        order.setStatus(HrAttendanceSupport.ORDER_STATUS_DRAFT);
        order.setProcessKey(HrAttendanceSupport.WORKFLOW_PROCESS_KEY_OVERTIME);
        overtimeOrderMapper.insert(order);
        return overtimeOrderMapper.selectById(order.getOrderId());
    }

    /**
     * 更新加班单草稿。
     *
     * @param orderId 单据ID
     * @param body 加班参数
     * @return 加班单
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public HrAttendanceOvertimeOrder updateOvertime(Long orderId, HrAttendanceOvertimeBody body) {
        HrAttendanceOvertimeOrder existing = requireOvertimeOrder(orderId);
        if (!HrAttendanceSupport.ORDER_STATUS_DRAFT.equals(existing.getStatus())) {
            throw new ServiceException("仅草稿状态允许修改加班单", (int) ResultCode.CONFLICT.getCode());
        }
        fillOvertimeOrder(existing, body, requireCurrentEmployee());
        overtimeOrderMapper.updateById(existing);
        return overtimeOrderMapper.selectById(orderId);
    }

    /**
     * 提交加班单审批。
     *
     * @param orderId 单据ID
     * @return 最新加班单
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public HrAttendanceOvertimeOrder submitOvertime(Long orderId) {
        HrAttendanceOvertimeOrder order = requireOvertimeOrder(orderId);
        if (!HrAttendanceSupport.ORDER_STATUS_DRAFT.equals(order.getStatus())) {
            throw new ServiceException("仅草稿状态允许提交加班审批", (int) ResultCode.CONFLICT.getCode());
        }
        order.setStatus(HrAttendanceSupport.ORDER_STATUS_SUBMITTED);
        overtimeOrderMapper.updateById(order);
        if (!workflowGateway.startOvertimeWorkflow(order)) {
            throw new ServiceException("加班流程发起失败", (int) ResultCode.ERROR.getCode());
        }
        return overtimeOrderMapper.selectById(orderId);
    }

    /**
     * 导入第三方出勤记录并重算汇总。
     *
     * @param body 第三方出勤参数
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void importExternalRecord(HrAttendanceExternalRecordBody body) {
        if (body == null || body.getEmployeeId() == null) {
            return;
        }
        HrEmployeeCore employee = requireEmployee(body.getEmployeeId());
        LocalDate workDate = body.getWorkDate();
        if (workDate == null) {
            if (body.getSignInTime() != null) {
                workDate = body.getSignInTime().toLocalDate();
            } else if (body.getSignOutTime() != null) {
                workDate = body.getSignOutTime().toLocalDate();
            }
        }
        if (workDate == null) {
            throw new IllegalArgumentException("第三方出勤导入必须提供工作日期");
        }
        HrAttendanceRecord record = loadRecord(employee.getEmployeeId(), workDate, HrAttendanceSupport.SOURCE_INTEGRATION);
        boolean newRecord = record == null;
        if (newRecord) {
            record = new HrAttendanceRecord();
            record.setTenantId(employee.getTenantId());
            record.setEmployeeId(employee.getEmployeeId());
            record.setOrgId(employee.getOrgId());
            record.setDeptId(employee.getDeptId());
            record.setWorkDate(HrAttendanceSupport.toDate(workDate));
            record.setSourceType(HrAttendanceSupport.SOURCE_INTEGRATION);
        }
        record.setAuthorityFlag(HrAttendanceSupport.FLAG_YES);
        record.setExternalBizNo(HrAttendanceSupport.trimToNull(body.getExternalBizNo()));
        record.setSignInTime(HrAttendanceSupport.toDate(body.getSignInTime()));
        record.setSignOutTime(HrAttendanceSupport.toDate(body.getSignOutTime()));
        record.setSignInLatitude(body.getSignInLatitude());
        record.setSignInLongitude(body.getSignInLongitude());
        record.setSignOutLatitude(body.getSignOutLatitude());
        record.setSignOutLongitude(body.getSignOutLongitude());
        record.setSignInAddress(HrAttendanceSupport.trimToNull(body.getSignInAddress()));
        record.setSignOutAddress(HrAttendanceSupport.trimToNull(body.getSignOutAddress()));
        record.setSignInInRange(HrAttendanceSupport.FLAG_YES);
        record.setSignOutInRange(HrAttendanceSupport.FLAG_YES);
        record.setRemark(HrAttendanceSupport.trimToNull(body.getRemark()));
        if (newRecord) {
            recordMapper.insert(record);
        } else {
            recordMapper.updateById(record);
        }
        aggregationService.recalculateEmployeeDay(employee.getEmployeeId(), workDate, resolveOperator());
        aggregationService.recalculateEmployeeMonth(employee.getEmployeeId(), HrAttendanceSupport.monthCode(workDate), resolveOperator());
    }

    /**
     * 根据部门日汇总构建部门视图。
     *
     * @param summaryList 日汇总列表
     * @param departmentMap 部门映射
     * @param dateLabel 日期标签
     * @return 部门汇总列表
     */
    private List<HrAttendanceDeptSummaryVO> buildDeptSummaryByDay(List<HrAttendanceDaySummary> summaryList,
            Map<Long, PlatformDeptView> departmentMap, String dateLabel) {
        Map<Long, List<HrAttendanceDaySummary>> grouped = (summaryList == null ? Collections.<HrAttendanceDaySummary>emptyList() : summaryList)
                .stream()
                .filter(item -> item.getDeptId() != null)
                .collect(Collectors.groupingBy(HrAttendanceDaySummary::getDeptId));
        return grouped.entrySet().stream()
                .map(entry -> buildDeptSummaryVO(entry.getKey(), dateLabel, entry.getValue(), departmentMap))
                .sorted((left, right) -> Long.compare(left.getDeptId(), right.getDeptId()))
                .toList();
    }

    /**
     * 根据部门月汇总构建部门视图。
     *
     * @param summaryList 月汇总列表
     * @param departmentMap 部门映射
     * @param dateLabel 日期标签
     * @return 部门汇总列表
     */
    private List<HrAttendanceDeptSummaryVO> buildDeptSummaryByMonth(List<HrAttendanceMonthSummary> summaryList,
            Map<Long, PlatformDeptView> departmentMap, String dateLabel) {
        Map<Long, List<HrAttendanceMonthSummary>> grouped = (summaryList == null ? Collections.<HrAttendanceMonthSummary>emptyList() : summaryList)
                .stream()
                .filter(item -> item.getDeptId() != null)
                .collect(Collectors.groupingBy(HrAttendanceMonthSummary::getDeptId));
        return grouped.entrySet().stream()
                .map(entry -> buildDeptSummaryVO(entry.getKey(), dateLabel, entry.getValue(), departmentMap))
                .sorted((left, right) -> Long.compare(left.getDeptId(), right.getDeptId()))
                .toList();
    }

    /**
     * 构造部门汇总视图。
     *
     * @param deptId 部门ID
     * @param dateLabel 日期标签
     * @param summaryList 汇总列表
     * @param departmentMap 部门映射
     * @return 部门汇总视图
     */
    private HrAttendanceDeptSummaryVO buildDeptSummaryVO(Long deptId, String dateLabel, Collection<?> summaryList,
            Map<Long, PlatformDeptView> departmentMap) {
        HrAttendanceDeptSummaryVO summaryVO = new HrAttendanceDeptSummaryVO();
        summaryVO.setDeptId(deptId);
        summaryVO.setDeptName(departmentMap.containsKey(deptId) ? departmentMap.get(deptId).getDeptName() : "未知部门");
        summaryVO.setDateLabel(dateLabel);
        summaryVO.setEmployeeCount(summaryList.stream().map(item -> {
            if (item instanceof HrAttendanceDaySummary daySummary) {
                return daySummary.getEmployeeId();
            }
            return ((HrAttendanceMonthSummary) item).getEmployeeId();
        }).filter(item -> item != null).distinct().count());
        summaryVO.setAttendanceDays(summaryList.stream().map(item -> {
            if (item instanceof HrAttendanceDaySummary daySummary) {
                return daySummary.getAttendanceDays();
            }
            return ((HrAttendanceMonthSummary) item).getAttendanceDays();
        }).reduce(BigDecimal.ZERO, BigDecimal::add));
        summaryVO.setActualMinutes(summaryList.stream().mapToInt(item -> {
            if (item instanceof HrAttendanceDaySummary daySummary) {
                return nullSafeInt(daySummary.getActualMinutes());
            }
            return nullSafeInt(((HrAttendanceMonthSummary) item).getActualMinutes());
        }).sum());
        summaryVO.setLeaveMinutes(summaryList.stream().mapToInt(item -> {
            if (item instanceof HrAttendanceDaySummary daySummary) {
                return nullSafeInt(daySummary.getLeaveMinutes());
            }
            return nullSafeInt(((HrAttendanceMonthSummary) item).getLeaveMinutes());
        }).sum());
        summaryVO.setLeaveDays(summaryList.stream().map(item -> {
            if (item instanceof HrAttendanceDaySummary daySummary) {
                return daySummary.getLeaveDays();
            }
            return ((HrAttendanceMonthSummary) item).getLeaveDays();
        }).reduce(BigDecimal.ZERO, BigDecimal::add));
        summaryVO.setOvertimeMinutes(summaryList.stream().mapToInt(item -> {
            if (item instanceof HrAttendanceDaySummary daySummary) {
                return nullSafeInt(daySummary.getOvertimeMinutes());
            }
            return nullSafeInt(((HrAttendanceMonthSummary) item).getOvertimeMinutes());
        }).sum());
        summaryVO.setLateCount(summaryList.stream().mapToInt(item -> {
            if (item instanceof HrAttendanceDaySummary daySummary) {
                return nullSafeInt(daySummary.getLateCount());
            }
            return nullSafeInt(((HrAttendanceMonthSummary) item).getLateCount());
        }).sum());
        summaryVO.setEarlyLeaveCount(summaryList.stream().mapToInt(item -> {
            if (item instanceof HrAttendanceDaySummary daySummary) {
                return nullSafeInt(daySummary.getEarlyLeaveCount());
            }
            return nullSafeInt(((HrAttendanceMonthSummary) item).getEarlyLeaveCount());
        }).sum());
        summaryVO.setMissingCardCount(summaryList.stream().mapToInt(item -> {
            if (item instanceof HrAttendanceDaySummary daySummary) {
                return nullSafeInt(daySummary.getMissingCardCount());
            }
            return nullSafeInt(((HrAttendanceMonthSummary) item).getMissingCardCount());
        }).sum());
        summaryVO.setAbsenteeismDays(summaryList.stream().map(item -> {
            if (item instanceof HrAttendanceDaySummary daySummary) {
                return daySummary.getAbsenteeismDays();
            }
            return ((HrAttendanceMonthSummary) item).getAbsenteeismDays();
        }).reduce(BigDecimal.ZERO, BigDecimal::add));
        summaryVO.setAbnormalCount(summaryList.stream().mapToInt(item -> {
            if (item instanceof HrAttendanceDaySummary daySummary) {
                return nullSafeInt(daySummary.getAbnormalCount());
            }
            return nullSafeInt(((HrAttendanceMonthSummary) item).getAbnormalCount());
        }).sum());
        return summaryVO;
    }

    /**
     * 查询租户下的部门列表。
     *
     * @return 部门列表
     */
    private List<PlatformDeptView> listTenantDepartments() {
        String tenantId = currentTenantId();
        return internalPlatformClient.listDepartments(null).stream()
                .filter(item -> item != null && StringUtils.hasText(item.getTenantId()))
                .filter(item -> item.getTenantId().trim().equals(tenantId))
                .toList();
    }

    /**
     * 解析部门查询范围。
     *
     * @param deptId 根部门ID
     * @param includeChildren 是否包含下级
     * @param departmentList 部门列表
     * @return 部门ID集合
     */
    private Set<Long> resolveDeptScope(Long deptId, boolean includeChildren, List<PlatformDeptView> departmentList) {
        if (departmentList == null || departmentList.isEmpty()) {
            return Collections.emptySet();
        }
        if (deptId == null) {
            return departmentList.stream().map(PlatformDeptView::getDeptId).collect(Collectors.toSet());
        }
        if (!includeChildren) {
            return Collections.singleton(deptId);
        }
        Map<Long, List<PlatformDeptView>> parentMap = new HashMap<>();
        for (PlatformDeptView item : departmentList) {
            parentMap.computeIfAbsent(item.getParentId(), key -> new ArrayList<>()).add(item);
        }
        Set<Long> result = new HashSet<>();
        ArrayDeque<Long> queue = new ArrayDeque<>();
        queue.add(deptId);
        while (!queue.isEmpty()) {
            Long currentDeptId = queue.poll();
            if (!result.add(currentDeptId)) {
                continue;
            }
            for (PlatformDeptView child : parentMap.getOrDefault(currentDeptId, Collections.emptyList())) {
                if (child.getDeptId() != null) {
                    queue.add(child.getDeptId());
                }
            }
        }
        return result;
    }

    /**
     * 填充请假单字段。
     *
     * @param order 请假单
     * @param body 请求参数
     * @param employee 当前员工
     */
    private void fillLeaveOrder(HrAttendanceLeaveOrder order, HrAttendanceLeaveBody body, HrEmployeeCore employee) {
        validateTimeRange(body == null ? null : body.getStartTime(), body == null ? null : body.getEndTime(), "请假时间范围");
        int minutes = HrAttendanceSupport.calculateWorkMinutes(body.getStartTime(), body.getEndTime());
        order.setTenantId(employee.getTenantId());
        order.setEmployeeId(employee.getEmployeeId());
        order.setOrgId(employee.getOrgId());
        order.setDeptId(employee.getDeptId());
        order.setLeaveType(HrAttendanceSupport.normalizeStatus(body == null ? null : body.getLeaveType()) == null
                ? HrAttendanceSupport.LEAVE_TYPE_ANNUAL
                : HrAttendanceSupport.normalizeStatus(body.getLeaveType()));
        order.setStartTime(HrAttendanceSupport.toDate(body.getStartTime()));
        order.setEndTime(HrAttendanceSupport.toDate(body.getEndTime()));
        order.setLeaveMinutes(minutes);
        order.setLeaveDays(HrAttendanceSupport.minutesToDays(minutes));
        order.setReason(body == null ? null : HrAttendanceSupport.trimToNull(body.getReason()));
        order.setRemark(body == null ? null : HrAttendanceSupport.trimToNull(body.getRemark()));
    }

    /**
     * 填充加班单字段。
     *
     * @param order 加班单
     * @param body 请求参数
     * @param employee 当前员工
     */
    private void fillOvertimeOrder(HrAttendanceOvertimeOrder order, HrAttendanceOvertimeBody body, HrEmployeeCore employee) {
        validateTimeRange(body == null ? null : body.getStartTime(), body == null ? null : body.getEndTime(), "加班时间范围");
        int minutes = HrAttendanceSupport.calculateWorkMinutes(body.getStartTime(), body.getEndTime());
        order.setTenantId(employee.getTenantId());
        order.setEmployeeId(employee.getEmployeeId());
        order.setOrgId(employee.getOrgId());
        order.setDeptId(employee.getDeptId());
        order.setOvertimeType(HrAttendanceSupport.normalizeStatus(body == null ? null : body.getOvertimeType()) == null
                ? HrAttendanceSupport.OVERTIME_TYPE_WORKDAY
                : HrAttendanceSupport.normalizeStatus(body.getOvertimeType()));
        order.setStartTime(HrAttendanceSupport.toDate(body.getStartTime()));
        order.setEndTime(HrAttendanceSupport.toDate(body.getEndTime()));
        order.setOvertimeMinutes(minutes);
        order.setReason(body == null ? null : HrAttendanceSupport.trimToNull(body.getReason()));
        order.setRemark(body == null ? null : HrAttendanceSupport.trimToNull(body.getRemark()));
    }

    /**
     * 根据经纬度解析定位范围标识。
     *
     * @param employee 当前员工
     * @param latitude 纬度
     * @param longitude 经度
     * @return 范围标识
     */
    private String resolveRangeFlag(HrEmployeeCore employee, BigDecimal latitude, BigDecimal longitude) {
        HrAttendanceLocationRule rule = findApplicableRule(employee.getDeptId());
        if (rule == null) {
            return HrAttendanceSupport.FLAG_YES;
        }
        int distance = HrAttendanceSupport.calculateDistanceMeters(latitude, longitude,
                rule.getCenterLatitude(), rule.getCenterLongitude());
        return distance <= (rule.getRadiusMeters() == null ? HrAttendanceSupport.DEFAULT_RADIUS_METERS : rule.getRadiusMeters())
                ? HrAttendanceSupport.FLAG_YES : HrAttendanceSupport.FLAG_NO;
    }

    /**
     * 查找适用的定位规则。
     *
     * @param deptId 部门ID
     * @return 定位规则
     */
    private HrAttendanceLocationRule findApplicableRule(Long deptId) {
        List<HrAttendanceLocationRule> ruleList = locationRuleMapper.selectList(new LambdaQueryWrapper<HrAttendanceLocationRule>()
                .eq(HrAttendanceLocationRule::getEnabledFlag, HrAttendanceSupport.RULE_ENABLED));
        if (ruleList == null || ruleList.isEmpty()) {
            return null;
        }
        for (HrAttendanceLocationRule rule : ruleList) {
            if (rule != null && deptId != null && deptId.equals(rule.getDeptId())) {
                return rule;
            }
        }
        return ruleList.stream().filter(item -> item != null && item.getDeptId() == null).findFirst().orElse(null);
    }

    /**
     * 按来源加载出勤原子记录。
     *
     * @param employeeId 员工ID
     * @param workDate 工作日期
     * @param sourceType 数据来源
     * @return 出勤记录
     */
    private HrAttendanceRecord loadRecord(Long employeeId, LocalDate workDate, String sourceType) {
        return recordMapper.selectOne(new LambdaQueryWrapper<HrAttendanceRecord>()
                .eq(HrAttendanceRecord::getEmployeeId, employeeId)
                .eq(HrAttendanceRecord::getWorkDate, HrAttendanceSupport.toDate(workDate))
                .eq(HrAttendanceRecord::getSourceType, sourceType));
    }

    /**
     * 校验时间范围合法性。
     *
     * @param startTime 开始时间
     * @param endTime 结束时间
     * @param label 提示语
     */
    private void validateTimeRange(LocalDateTime startTime, LocalDateTime endTime, String label) {
        if (startTime == null || endTime == null || !endTime.isAfter(startTime)) {
            throw new ServiceException(label + "不合法", (int) ResultCode.PARAM_ERROR.getCode());
        }
    }

    /**
     * 解析工作日期。
     *
     * @param workDate 日期字符串
     * @return 工作日期
     */
    private LocalDate parseWorkDate(String workDate) {
        if (!StringUtils.hasText(workDate)) {
            return LocalDate.now();
        }
        return LocalDate.parse(workDate.trim());
    }

    /**
     * 校验并加载当前员工主档。
     *
     * @return 当前员工主档
     */
    private HrEmployeeCore requireCurrentEmployee() {
        Long userId = securityUserResolver.getCurrentUserId();
        if (userId == null) {
            throw new ServiceException("当前账号未登录或未绑定员工", (int) ResultCode.UNAUTHORIZED.getCode());
        }
        HrEmployeeCore employee = employeeCoreMapper.selectOne(new LambdaQueryWrapper<HrEmployeeCore>()
                .eq(HrEmployeeCore::getTenantId, currentTenantId())
                .eq(HrEmployeeCore::getUserId, userId)
                .eq(HrEmployeeCore::getDelFlag, HrEmployeeSupport.EXIST_DEL_FLAG));
        if (employee == null) {
            throw new ServiceException("当前账号未绑定员工档案", (int) ResultCode.NOT_FOUND.getCode());
        }
        return employee;
    }

    /**
     * 校验并加载员工主档。
     *
     * @param employeeId 员工ID
     * @return 员工主档
     */
    private HrEmployeeCore requireEmployee(Long employeeId) {
        HrEmployeeCore employee = employeeCoreMapper.selectOne(new LambdaQueryWrapper<HrEmployeeCore>()
                .eq(HrEmployeeCore::getEmployeeId, employeeId)
                .eq(HrEmployeeCore::getTenantId, currentTenantId())
                .eq(HrEmployeeCore::getDelFlag, HrEmployeeSupport.EXIST_DEL_FLAG));
        if (employee == null) {
            throw new ServiceException("员工不存在", (int) ResultCode.NOT_FOUND.getCode());
        }
        return employee;
    }

    /**
     * 校验并加载请假单。
     *
     * @param orderId 单据ID
     * @return 请假单
     */
    private HrAttendanceLeaveOrder requireLeaveOrder(Long orderId) {
        HrAttendanceLeaveOrder order = leaveOrderMapper.selectOne(new LambdaQueryWrapper<HrAttendanceLeaveOrder>()
                .eq(HrAttendanceLeaveOrder::getOrderId, orderId));
        if (order == null) {
            throw new ServiceException("请假单不存在", (int) ResultCode.NOT_FOUND.getCode());
        }
        return order;
    }

    /**
     * 校验并加载加班单。
     *
     * @param orderId 单据ID
     * @return 加班单
     */
    private HrAttendanceOvertimeOrder requireOvertimeOrder(Long orderId) {
        HrAttendanceOvertimeOrder order = overtimeOrderMapper.selectOne(new LambdaQueryWrapper<HrAttendanceOvertimeOrder>()
                .eq(HrAttendanceOvertimeOrder::getOrderId, orderId));
        if (order == null) {
            throw new ServiceException("加班单不存在", (int) ResultCode.NOT_FOUND.getCode());
        }
        return order;
    }

    /**
     * 返回当前租户编号。
     *
     * @return 当前租户编号
     */
    private String currentTenantId() {
        return securityUserResolver.getCurrentTenantId();
    }

    /**
     * 返回当前操作人。
     *
     * @return 当前操作人
     */
    private String resolveOperator() {
        String username = securityUserResolver.getCurrentUsername();
        return StringUtils.hasText(username) ? username.trim() : "system";
    }

    /**
     * 将可空整数转为非空值。
     *
     * @param value 原始值
     * @return 非空整数
     */
    private int nullSafeInt(Integer value) {
        return value == null ? 0 : value;
    }
}

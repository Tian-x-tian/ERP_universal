package com.erp.business.hr.attendance.core.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.erp.business.hr.attendance.core.domain.HrAttendanceLeaveOrder;
import com.erp.business.hr.attendance.core.domain.HrAttendanceOvertimeOrder;
import com.erp.business.hr.attendance.core.mapper.HrAttendanceLeaveOrderMapper;
import com.erp.business.hr.attendance.core.mapper.HrAttendanceOvertimeOrderMapper;
import com.erp.business.hr.attendance.core.service.IHrAttendanceAggregationService;
import com.erp.business.hr.attendance.core.service.IHrAttendanceWorkflowBridgeService;
import com.erp.business.hr.attendance.core.support.HrAttendanceSupport;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.util.Date;

/**
 * 出勤工作流桥接实现。
 */
@Service
public class HrAttendanceWorkflowBridgeServiceImpl implements IHrAttendanceWorkflowBridgeService {

    private final HrAttendanceLeaveOrderMapper leaveOrderMapper;
    private final HrAttendanceOvertimeOrderMapper overtimeOrderMapper;
    private final IHrAttendanceAggregationService aggregationService;

    public HrAttendanceWorkflowBridgeServiceImpl(HrAttendanceLeaveOrderMapper leaveOrderMapper,
            HrAttendanceOvertimeOrderMapper overtimeOrderMapper,
            IHrAttendanceAggregationService aggregationService) {
        this.leaveOrderMapper = leaveOrderMapper;
        this.overtimeOrderMapper = overtimeOrderMapper;
        this.aggregationService = aggregationService;
    }

    /**
     * 回写请假审批通过结果。
     *
     * @param orderId 单据ID
     * @param operator 操作人
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void onLeaveApproved(Long orderId, String operator) {
        HrAttendanceLeaveOrder order = loadLeaveOrder(orderId);
        updateLeaveStatus(order, HrAttendanceSupport.ORDER_STATUS_APPROVED, operator);
        recalculateOrderScope(order.getEmployeeId(), order.getStartTime(), order.getEndTime(), operator);
    }

    /**
     * 回写请假审批驳回或撤回结果。
     *
     * @param orderId 单据ID
     * @param operator 操作人
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void onLeaveRejected(Long orderId, String operator) {
        HrAttendanceLeaveOrder order = loadLeaveOrder(orderId);
        updateLeaveStatus(order, HrAttendanceSupport.ORDER_STATUS_REJECTED, operator);
        recalculateOrderScope(order.getEmployeeId(), order.getStartTime(), order.getEndTime(), operator);
    }

    /**
     * 回写加班审批通过结果。
     *
     * @param orderId 单据ID
     * @param operator 操作人
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void onOvertimeApproved(Long orderId, String operator) {
        HrAttendanceOvertimeOrder order = loadOvertimeOrder(orderId);
        updateOvertimeStatus(order, HrAttendanceSupport.ORDER_STATUS_APPROVED, operator);
        recalculateOrderScope(order.getEmployeeId(), order.getStartTime(), order.getEndTime(), operator);
    }

    /**
     * 回写加班审批驳回或撤回结果。
     *
     * @param orderId 单据ID
     * @param operator 操作人
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void onOvertimeRejected(Long orderId, String operator) {
        HrAttendanceOvertimeOrder order = loadOvertimeOrder(orderId);
        updateOvertimeStatus(order, HrAttendanceSupport.ORDER_STATUS_REJECTED, operator);
        recalculateOrderScope(order.getEmployeeId(), order.getStartTime(), order.getEndTime(), operator);
    }

    /**
     * 加载请假单。
     *
     * @param orderId 单据ID
     * @return 请假单
     */
    private HrAttendanceLeaveOrder loadLeaveOrder(Long orderId) {
        if (orderId == null) {
            return null;
        }
        return leaveOrderMapper.selectOne(new LambdaQueryWrapper<HrAttendanceLeaveOrder>()
                .eq(HrAttendanceLeaveOrder::getOrderId, orderId));
    }

    /**
     * 加载加班单。
     *
     * @param orderId 单据ID
     * @return 加班单
     */
    private HrAttendanceOvertimeOrder loadOvertimeOrder(Long orderId) {
        if (orderId == null) {
            return null;
        }
        return overtimeOrderMapper.selectOne(new LambdaQueryWrapper<HrAttendanceOvertimeOrder>()
                .eq(HrAttendanceOvertimeOrder::getOrderId, orderId));
    }

    /**
     * 更新请假单状态。
     *
     * @param order 请假单
     * @param status 目标状态
     * @param operator 操作人
     */
    private void updateLeaveStatus(HrAttendanceLeaveOrder order, String status, String operator) {
        if (order == null) {
            return;
        }
        HrAttendanceLeaveOrder updateEntity = new HrAttendanceLeaveOrder();
        updateEntity.setOrderId(order.getOrderId());
        updateEntity.setStatus(status);
        updateEntity.setUpdateBy(StringUtils.hasText(operator) ? operator.trim() : "system");
        updateEntity.setUpdateTime(new Date());
        leaveOrderMapper.updateById(updateEntity);
    }

    /**
     * 更新加班单状态。
     *
     * @param order 加班单
     * @param status 目标状态
     * @param operator 操作人
     */
    private void updateOvertimeStatus(HrAttendanceOvertimeOrder order, String status, String operator) {
        if (order == null) {
            return;
        }
        HrAttendanceOvertimeOrder updateEntity = new HrAttendanceOvertimeOrder();
        updateEntity.setOrderId(order.getOrderId());
        updateEntity.setStatus(status);
        updateEntity.setUpdateBy(StringUtils.hasText(operator) ? operator.trim() : "system");
        updateEntity.setUpdateTime(new Date());
        overtimeOrderMapper.updateById(updateEntity);
    }

    /**
     * 重算单据覆盖的出勤范围。
     *
     * @param employeeId 员工ID
     * @param startTime 开始时间
     * @param endTime 结束时间
     * @param operator 操作人
     */
    private void recalculateOrderScope(Long employeeId, Date startTime, Date endTime, String operator) {
        if (employeeId == null || startTime == null || endTime == null) {
            return;
        }
        LocalDate startDate = HrAttendanceSupport.toLocalDate(startTime);
        LocalDate endDate = HrAttendanceSupport.toLocalDate(endTime);
        if (startDate == null || endDate == null || endDate.isBefore(startDate)) {
            return;
        }
        LocalDate current = startDate;
        while (!current.isAfter(endDate)) {
            aggregationService.recalculateEmployeeDay(employeeId, current, operator);
            aggregationService.recalculateEmployeeMonth(employeeId, HrAttendanceSupport.monthCode(current), operator);
            current = current.plusDays(1);
        }
    }
}

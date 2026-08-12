package com.erp.business.controller;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.erp.business.hr.attendance.core.service.IHrAttendanceWorkflowBridgeService;
import com.erp.business.hr.service.IHrEmployeeWorkflowBridgeService;
import com.erp.business.inventory.service.IInventoryInboundService;
import com.erp.business.inventory.service.IInventoryOutboundService;
import com.erp.business.inventory.service.IInventoryStockAdjustService;
import com.erp.business.inventory.service.IInventoryStockFreezeService;
import com.erp.business.inventory.service.IInventoryStockMoveService;
import com.erp.business.inventory.service.IInventoryStocktakeService;
import com.erp.business.inventory.service.IInventoryTransferService;
import com.erp.common.core.context.TenantContextHolder;
import com.erp.workflow.contract.domain.vo.WorkflowCallbackEvent;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * 业务模块工作流终态回调控制层。
 */
@RestController
@RequestMapping("/business/internal/workflow/callbacks")
public class BusinessWorkflowCallbackController {
    private static final String INSTANCE_STATUS_COMPLETED = "1";
    private static final String INSTANCE_STATUS_REJECTED = "2";
    private static final String INSTANCE_STATUS_WITHDRAWN = "3";
    private static final String BUSINESS_TYPE_PURCHASE_INBOUND = "PURCHASE_INBOUND";
    private static final String BUSINESS_TYPE_SALES_OUTBOUND = "SALES_OUTBOUND";
    private static final String BUSINESS_TYPE_INVENTORY_ADJUST = "INVENTORY_ADJUST";
    private static final String BUSINESS_TYPE_INVENTORY_FREEZE = "INVENTORY_FREEZE";
    private static final String BUSINESS_TYPE_INVENTORY_MOVE = "INVENTORY_MOVE";
    private static final String BUSINESS_TYPE_INVENTORY_STOCKTAKE = "INVENTORY_STOCKTAKE";
    private static final String BUSINESS_TYPE_INVENTORY_TRANSFER = "INVENTORY_TRANSFER";
    private static final String BUSINESS_TYPE_ATTENDANCE_LEAVE = "HR_ATTENDANCE_LEAVE";
    private static final String BUSINESS_TYPE_ATTENDANCE_OVERTIME = "HR_ATTENDANCE_OVERTIME";

    private final IInventoryInboundService inboundService;
    private final IInventoryOutboundService outboundService;
    private final IInventoryStockAdjustService stockAdjustService;
    private final IInventoryStockFreezeService stockFreezeService;
    private final IInventoryStockMoveService stockMoveService;
    private final IInventoryStocktakeService stocktakeService;
    private final IInventoryTransferService transferService;
    private final IHrEmployeeWorkflowBridgeService employeeWorkflowBridgeService;
    private final IHrAttendanceWorkflowBridgeService attendanceWorkflowBridgeService;
    private final ObjectMapper objectMapper;

    public BusinessWorkflowCallbackController(IInventoryInboundService inboundService,
            IInventoryOutboundService outboundService,
            IInventoryStockAdjustService stockAdjustService,
            IInventoryStockFreezeService stockFreezeService,
            IInventoryStockMoveService stockMoveService,
            IInventoryStocktakeService stocktakeService,
            IInventoryTransferService transferService,
            IHrEmployeeWorkflowBridgeService employeeWorkflowBridgeService,
            IHrAttendanceWorkflowBridgeService attendanceWorkflowBridgeService) {
        this.inboundService = inboundService;
        this.outboundService = outboundService;
        this.stockAdjustService = stockAdjustService;
        this.stockFreezeService = stockFreezeService;
        this.stockMoveService = stockMoveService;
        this.stocktakeService = stocktakeService;
        this.transferService = transferService;
        this.employeeWorkflowBridgeService = employeeWorkflowBridgeService;
        this.attendanceWorkflowBridgeService = attendanceWorkflowBridgeService;
        this.objectMapper = new ObjectMapper();
    }

    /**
     * 处理工作流终态回调事件。
     *
     * @param event 回调事件
     */
    @PostMapping("/terminal")
    public void terminal(@RequestBody WorkflowCallbackEvent event) {
        if (event == null || !StringUtils.hasText(event.getTenantId())) {
            return;
        }
        TenantContextHolder.setTenantId(event.getTenantId().trim());
        try {
            if (handleInventoryCallback(event)) {
                return;
            }
            handleHrCallback(event);
        } finally {
            TenantContextHolder.clear();
        }
    }

    /**
     * 处理库存单据回调。
     *
     * @param event 回调事件
     * @return true 表示已处理
     */
    private boolean handleInventoryCallback(WorkflowCallbackEvent event) {
        Long orderId = readLong(readFormValue(event.getFormData(), "orderId"));
        if (orderId == null || !StringUtils.hasText(event.getBusinessType())) {
            return false;
        }
        String businessType = event.getBusinessType().trim().toUpperCase();
        boolean approved = INSTANCE_STATUS_COMPLETED.equals(event.getStatus());
        boolean rejected = INSTANCE_STATUS_REJECTED.equals(event.getStatus()) || INSTANCE_STATUS_WITHDRAWN.equals(event.getStatus());
        if (!approved && !rejected) {
            return false;
        }
        if (BUSINESS_TYPE_PURCHASE_INBOUND.equals(businessType)) {
            dispatchInventoryResult(approved, orderId, inboundService);
            return true;
        }
        if (BUSINESS_TYPE_SALES_OUTBOUND.equals(businessType)) {
            dispatchInventoryResult(approved, orderId, outboundService);
            return true;
        }
        if (BUSINESS_TYPE_INVENTORY_ADJUST.equals(businessType)) {
            dispatchInventoryResult(approved, orderId, stockAdjustService);
            return true;
        }
        if (BUSINESS_TYPE_INVENTORY_FREEZE.equals(businessType)) {
            dispatchInventoryResult(approved, orderId, stockFreezeService);
            return true;
        }
        if (BUSINESS_TYPE_INVENTORY_MOVE.equals(businessType)) {
            dispatchInventoryResult(approved, orderId, stockMoveService);
            return true;
        }
        if (BUSINESS_TYPE_INVENTORY_STOCKTAKE.equals(businessType)) {
            dispatchInventoryResult(approved, orderId, stocktakeService);
            return true;
        }
        if (BUSINESS_TYPE_INVENTORY_TRANSFER.equals(businessType)) {
            dispatchInventoryResult(approved, orderId, transferService);
            return true;
        }
        return false;
    }

    /**
     * 处理 HR 回调。
     *
     * @param event 回调事件
     */
    private void handleHrCallback(WorkflowCallbackEvent event) {
        if (handleAttendanceCallback(event)) {
            return;
        }
        Long changeRecordId = readLong(readFormValue(event.getFormData(), "changeRecordId"));
        if (changeRecordId == null) {
            return;
        }
        String archivePayloadJson = readString(readFormValue(event.getFormData(), "archivePayloadJson"));
        String operator = StringUtils.hasText(event.getOperator()) ? event.getOperator().trim() : "system";
        if (INSTANCE_STATUS_COMPLETED.equals(event.getStatus())) {
            employeeWorkflowBridgeService.onChangeApproved(changeRecordId, archivePayloadJson, operator);
            return;
        }
        if (INSTANCE_STATUS_REJECTED.equals(event.getStatus()) || INSTANCE_STATUS_WITHDRAWN.equals(event.getStatus())) {
            employeeWorkflowBridgeService.onChangeRejected(changeRecordId, operator);
        }
    }

    /**
     * 处理出勤审批回调。
     *
     * @param event 回调事件
     * @return true 表示已处理
     */
    private boolean handleAttendanceCallback(WorkflowCallbackEvent event) {
        Long orderId = readLong(readFormValue(event.getFormData(), "orderId"));
        if (orderId == null || !StringUtils.hasText(event.getBusinessType())) {
            return false;
        }
        String businessType = event.getBusinessType().trim().toUpperCase();
        String operator = StringUtils.hasText(event.getOperator()) ? event.getOperator().trim() : "system";
        boolean approved = INSTANCE_STATUS_COMPLETED.equals(event.getStatus());
        boolean rejected = INSTANCE_STATUS_REJECTED.equals(event.getStatus()) || INSTANCE_STATUS_WITHDRAWN.equals(event.getStatus());
        if (!approved && !rejected) {
            return false;
        }
        if (BUSINESS_TYPE_ATTENDANCE_LEAVE.equals(businessType)) {
            if (approved) {
                attendanceWorkflowBridgeService.onLeaveApproved(orderId, operator);
            } else {
                attendanceWorkflowBridgeService.onLeaveRejected(orderId, operator);
            }
            return true;
        }
        if (BUSINESS_TYPE_ATTENDANCE_OVERTIME.equals(businessType)) {
            if (approved) {
                attendanceWorkflowBridgeService.onOvertimeApproved(orderId, operator);
            } else {
                attendanceWorkflowBridgeService.onOvertimeRejected(orderId, operator);
            }
            return true;
        }
        return false;
    }

    /**
     * 派发库存审批回写结果。
     *
     * @param approved 是否审批通过
     * @param orderId 单据ID
     * @param service 单据服务
     */
    private void dispatchInventoryResult(boolean approved, Long orderId, Object service) {
        if (service instanceof IInventoryInboundService inbound) {
            if (approved) {
                inbound.approve(orderId);
            } else {
                inbound.reject(orderId);
            }
            return;
        }
        if (service instanceof IInventoryOutboundService outbound) {
            if (approved) {
                outbound.approve(orderId);
            } else {
                outbound.reject(orderId);
            }
            return;
        }
        if (service instanceof IInventoryStockAdjustService stockAdjust) {
            if (approved) {
                stockAdjust.approve(orderId);
            } else {
                stockAdjust.reject(orderId);
            }
            return;
        }
        if (service instanceof IInventoryStockFreezeService stockFreeze) {
            if (approved) {
                stockFreeze.approve(orderId);
            } else {
                stockFreeze.reject(orderId);
            }
            return;
        }
        if (service instanceof IInventoryStockMoveService stockMove) {
            if (approved) {
                stockMove.approve(orderId);
            } else {
                stockMove.reject(orderId);
            }
            return;
        }
        if (service instanceof IInventoryStocktakeService stocktake) {
            if (approved) {
                stocktake.approve(orderId);
            } else {
                stocktake.reject(orderId);
            }
            return;
        }
        if (service instanceof IInventoryTransferService transfer) {
            if (approved) {
                transfer.approve(orderId);
            } else {
                transfer.reject(orderId);
            }
        }
    }

    /**
     * 读取表单字段。
     *
     * @param formData 表单 JSON
     * @param key 字段名
     * @return 字段值
     */
    private Object readFormValue(String formData, String key) {
        if (!StringUtils.hasText(formData) || !StringUtils.hasText(key)) {
            return null;
        }
        try {
            Map<String, Object> valueMap = objectMapper.readValue(formData, new TypeReference<Map<String, Object>>() {
            });
            return valueMap.get(key);
        } catch (Exception ex) {
            return null;
        }
    }

    /**
     * 读取字符串。
     *
     * @param value 原始值
     * @return 字符串
     */
    private String readString(Object value) {
        return value == null ? null : String.valueOf(value);
    }

    /**
     * 读取长整型。
     *
     * @param value 原始值
     * @return 长整型
     */
    private Long readLong(Object value) {
        if (value == null) {
            return null;
        }
        try {
            return Long.valueOf(String.valueOf(value));
        } catch (NumberFormatException ex) {
            return null;
        }
    }
}

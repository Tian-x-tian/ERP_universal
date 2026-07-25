package com.erp.business.purchase.support;

import com.erp.common.core.exception.ServiceException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * 采购单据状态机单元测试。
 *
 * <p>状态流转是采购链路的核心约束，这里把「允许」与「禁止」都锁住，
 * 避免后续加功能时无意打开非法路径。
 */
class PurchaseBillStatusSupportTest {

    @Test
    @DisplayName("采购申请：草稿可提交，提交后可通过或驳回")
    void shouldAllowValidRequisitionTransitions() {
        Assertions.assertDoesNotThrow(() -> PurchaseBillStatusSupport.ensureRequisitionTransition(
                PurchaseBillStatusSupport.REQ_DRAFT, PurchaseBillStatusSupport.REQ_SUBMITTED));
        Assertions.assertDoesNotThrow(() -> PurchaseBillStatusSupport.ensureRequisitionTransition(
                PurchaseBillStatusSupport.REQ_SUBMITTED, PurchaseBillStatusSupport.REQ_APPROVED));
        Assertions.assertDoesNotThrow(() -> PurchaseBillStatusSupport.ensureRequisitionTransition(
                PurchaseBillStatusSupport.REQ_SUBMITTED, PurchaseBillStatusSupport.REQ_REJECTED));
        Assertions.assertDoesNotThrow(() -> PurchaseBillStatusSupport.ensureRequisitionTransition(
                PurchaseBillStatusSupport.REQ_APPROVED, PurchaseBillStatusSupport.REQ_CONVERTED));
    }

    @Test
    @DisplayName("采购申请：草稿不可直接转订单，已转订单不可再流转")
    void shouldRejectInvalidRequisitionTransitions() {
        Assertions.assertThrows(ServiceException.class, () -> PurchaseBillStatusSupport.ensureRequisitionTransition(
                PurchaseBillStatusSupport.REQ_DRAFT, PurchaseBillStatusSupport.REQ_CONVERTED));
        Assertions.assertThrows(ServiceException.class, () -> PurchaseBillStatusSupport.ensureRequisitionTransition(
                PurchaseBillStatusSupport.REQ_CONVERTED, PurchaseBillStatusSupport.REQ_SUBMITTED));
    }

    @Test
    @DisplayName("采购订单：草稿可进审批或直接生效，生效后可收货")
    void shouldAllowValidOrderTransitions() {
        Assertions.assertDoesNotThrow(() -> PurchaseBillStatusSupport.ensureOrderTransition(
                PurchaseBillStatusSupport.ORDER_DRAFT, PurchaseBillStatusSupport.ORDER_PENDING_APPROVAL));
        Assertions.assertDoesNotThrow(() -> PurchaseBillStatusSupport.ensureOrderTransition(
                PurchaseBillStatusSupport.ORDER_DRAFT, PurchaseBillStatusSupport.ORDER_APPROVED));
        Assertions.assertDoesNotThrow(() -> PurchaseBillStatusSupport.ensureOrderTransition(
                PurchaseBillStatusSupport.ORDER_APPROVED, PurchaseBillStatusSupport.ORDER_PARTIAL_RECEIVED));
        Assertions.assertDoesNotThrow(() -> PurchaseBillStatusSupport.ensureOrderTransition(
                PurchaseBillStatusSupport.ORDER_PARTIAL_RECEIVED, PurchaseBillStatusSupport.ORDER_RECEIVED));
        Assertions.assertDoesNotThrow(() -> PurchaseBillStatusSupport.ensureOrderTransition(
                PurchaseBillStatusSupport.ORDER_RECEIVED, PurchaseBillStatusSupport.ORDER_CLOSED));
    }

    @Test
    @DisplayName("采购订单：草稿不可直接收货，已收货不可取消")
    void shouldRejectInvalidOrderTransitions() {
        Assertions.assertThrows(ServiceException.class, () -> PurchaseBillStatusSupport.ensureOrderTransition(
                PurchaseBillStatusSupport.ORDER_DRAFT, PurchaseBillStatusSupport.ORDER_RECEIVED));
        Assertions.assertThrows(ServiceException.class, () -> PurchaseBillStatusSupport.ensureOrderTransition(
                PurchaseBillStatusSupport.ORDER_PARTIAL_RECEIVED, PurchaseBillStatusSupport.ORDER_CANCELLED));
        Assertions.assertThrows(ServiceException.class, () -> PurchaseBillStatusSupport.ensureOrderTransition(
                PurchaseBillStatusSupport.ORDER_CLOSED, PurchaseBillStatusSupport.ORDER_APPROVED));
    }

    @Test
    @DisplayName("已产生收货的订单会被识别，用于拒绝取消")
    void shouldDetectOrdersWithReceiving() {
        Assertions.assertTrue(PurchaseBillStatusSupport.orderHasReceiving(PurchaseBillStatusSupport.ORDER_PARTIAL_RECEIVED));
        Assertions.assertTrue(PurchaseBillStatusSupport.orderHasReceiving(PurchaseBillStatusSupport.ORDER_RECEIVED));
        Assertions.assertTrue(PurchaseBillStatusSupport.orderHasReceiving(PurchaseBillStatusSupport.ORDER_CLOSED));
        Assertions.assertFalse(PurchaseBillStatusSupport.orderHasReceiving(PurchaseBillStatusSupport.ORDER_APPROVED));
        Assertions.assertFalse(PurchaseBillStatusSupport.orderHasReceiving(PurchaseBillStatusSupport.ORDER_DRAFT));
    }

    @Test
    @DisplayName("状态为空或未知时一律拒绝流转")
    void shouldRejectUnknownStatus() {
        Assertions.assertThrows(ServiceException.class,
                () -> PurchaseBillStatusSupport.ensureOrderTransition(null, PurchaseBillStatusSupport.ORDER_APPROVED));
        Assertions.assertThrows(ServiceException.class,
                () -> PurchaseBillStatusSupport.ensureOrderTransition("WHATEVER", PurchaseBillStatusSupport.ORDER_APPROVED));
    }
}

package com.erp.business.purchase.support;

import com.erp.common.core.exception.ServiceException;

import java.util.Collections;
import java.util.Map;
import java.util.Set;

/**
 * 采购单据状态机。
 *
 * <p>把「哪些状态可以流转到哪些状态」集中在这里声明，各 service 只调用
 * {@link #ensureRequisitionTransition} / {@link #ensureOrderTransition} 做校验，
 * 避免状态规则散落在增删改各处、改一个状态要翻遍代码。
 */
public final class PurchaseBillStatusSupport {

    // ---- 采购申请状态 ----
    /** 草稿 */
    public static final String REQ_DRAFT = "DRAFT";
    /** 审批中 */
    public static final String REQ_SUBMITTED = "SUBMITTED";
    /** 审批通过 */
    public static final String REQ_APPROVED = "APPROVED";
    /** 审批驳回 */
    public static final String REQ_REJECTED = "REJECTED";
    /** 已转采购订单 */
    public static final String REQ_CONVERTED = "CONVERTED";
    /** 已取消 */
    public static final String REQ_CANCELLED = "CANCELLED";

    // ---- 采购订单状态 ----
    /** 草稿 */
    public static final String ORDER_DRAFT = "DRAFT";
    /** 待审批 */
    public static final String ORDER_PENDING_APPROVAL = "PENDING_APPROVAL";
    /** 已生效 */
    public static final String ORDER_APPROVED = "APPROVED";
    /** 部分收货 */
    public static final String ORDER_PARTIAL_RECEIVED = "PARTIAL_RECEIVED";
    /** 收货完成 */
    public static final String ORDER_RECEIVED = "RECEIVED";
    /** 已关闭 */
    public static final String ORDER_CLOSED = "CLOSED";
    /** 已取消 */
    public static final String ORDER_CANCELLED = "CANCELLED";

    /** 采购申请允许的流转 */
    private static final Map<String, Set<String>> REQUISITION_TRANSITIONS = Map.of(
            REQ_DRAFT, Set.of(REQ_SUBMITTED, REQ_CANCELLED),
            REQ_SUBMITTED, Set.of(REQ_APPROVED, REQ_REJECTED, REQ_CANCELLED),
            REQ_REJECTED, Set.of(REQ_DRAFT, REQ_CANCELLED),
            REQ_APPROVED, Set.of(REQ_CONVERTED, REQ_CANCELLED),
            REQ_CONVERTED, Set.of(),
            REQ_CANCELLED, Set.of());

    /** 采购订单允许的流转 */
    private static final Map<String, Set<String>> ORDER_TRANSITIONS = Map.of(
            ORDER_DRAFT, Set.of(ORDER_PENDING_APPROVAL, ORDER_APPROVED, ORDER_CANCELLED),
            ORDER_PENDING_APPROVAL, Set.of(ORDER_APPROVED, ORDER_DRAFT, ORDER_CANCELLED),
            ORDER_APPROVED, Set.of(ORDER_PARTIAL_RECEIVED, ORDER_RECEIVED, ORDER_CANCELLED),
            ORDER_PARTIAL_RECEIVED, Set.of(ORDER_PARTIAL_RECEIVED, ORDER_RECEIVED),
            ORDER_RECEIVED, Set.of(ORDER_CLOSED),
            ORDER_CLOSED, Set.of(),
            ORDER_CANCELLED, Set.of());

    private PurchaseBillStatusSupport() {
    }

    /**
     * 校验采购申请状态流转是否合法，非法时抛业务异常。
     *
     * @param from 当前状态
     * @param to   目标状态
     */
    public static void ensureRequisitionTransition(String from, String to) {
        ensureTransition(REQUISITION_TRANSITIONS, from, to, "采购申请");
    }

    /**
     * 校验采购订单状态流转是否合法，非法时抛业务异常。
     *
     * @param from 当前状态
     * @param to   目标状态
     */
    public static void ensureOrderTransition(String from, String to) {
        ensureTransition(ORDER_TRANSITIONS, from, to, "采购订单");
    }

    /**
     * 判断采购订单是否已产生收货（此后不允许取消）。
     *
     * @param status 订单状态
     * @return true 表示已有收货动作
     */
    public static boolean orderHasReceiving(String status) {
        return ORDER_PARTIAL_RECEIVED.equals(status)
                || ORDER_RECEIVED.equals(status)
                || ORDER_CLOSED.equals(status);
    }

    /**
     * 判断采购单据是否处于可编辑状态（仅草稿可改）。
     *
     * @param status 状态
     * @return true 表示可编辑
     */
    public static boolean isEditable(String status) {
        return REQ_DRAFT.equals(status);
    }

    /**
     * 通用流转校验。
     *
     * @param transitions 流转表
     * @param from        当前状态
     * @param to          目标状态
     * @param billLabel   单据名称，用于错误提示
     */
    private static void ensureTransition(Map<String, Set<String>> transitions, String from, String to, String billLabel) {
        String currentStatus = from == null ? "" : from.trim();
        String targetStatus = to == null ? "" : to.trim();
        Set<String> allowed = transitions.getOrDefault(currentStatus, Collections.emptySet());
        if (!allowed.contains(targetStatus)) {
            throw new ServiceException(billLabel + "当前状态[" + currentStatus + "]不允许流转到[" + targetStatus + "]");
        }
    }
}

package com.erp.system.domain.vo;

import java.io.Serializable;

/**
 * 系统首页健康汇总对象。
 */
public class SystemHomeHealthSummaryVO implements Serializable {
    private static final long serialVersionUID = 1L;

    /** 24小时成功率 */
    private double successRate24h;

    /** 24小时事件总数 */
    private long totalEventCount24h;

    /** 24小时失败事件数 */
    private long failedEventCount24h;

    /** 24小时登录成功率 */
    private double loginSuccessRate24h;

    /** 24小时操作成功率 */
    private double operSuccessRate24h;

    public double getSuccessRate24h() {
        return successRate24h;
    }

    public void setSuccessRate24h(double successRate24h) {
        this.successRate24h = successRate24h;
    }

    public long getTotalEventCount24h() {
        return totalEventCount24h;
    }

    public void setTotalEventCount24h(long totalEventCount24h) {
        this.totalEventCount24h = totalEventCount24h;
    }

    public long getFailedEventCount24h() {
        return failedEventCount24h;
    }

    public void setFailedEventCount24h(long failedEventCount24h) {
        this.failedEventCount24h = failedEventCount24h;
    }

    public double getLoginSuccessRate24h() {
        return loginSuccessRate24h;
    }

    public void setLoginSuccessRate24h(double loginSuccessRate24h) {
        this.loginSuccessRate24h = loginSuccessRate24h;
    }

    public double getOperSuccessRate24h() {
        return operSuccessRate24h;
    }

    public void setOperSuccessRate24h(double operSuccessRate24h) {
        this.operSuccessRate24h = operSuccessRate24h;
    }
}

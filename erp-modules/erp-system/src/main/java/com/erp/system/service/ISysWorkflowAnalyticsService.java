package com.erp.system.service;

import com.erp.system.domain.vo.WorkflowDashboardVO;
import com.erp.system.domain.vo.WorkflowDashboardQueryVO;

/**
 * 流程分析服务接口。
 */
public interface ISysWorkflowAnalyticsService {

    /**
     * 查询流程效率看板。
     *
     * @return 看板结果
     */
    WorkflowDashboardVO buildDashboard(WorkflowDashboardQueryVO queryVO);
}

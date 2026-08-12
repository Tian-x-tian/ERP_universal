package com.erp.business.hr.service;

import com.erp.business.hr.domain.vo.HrWarningHomeSummaryVO;

/**
 * HR 首页汇总服务接口。
 */
public interface IHrHomeSummaryService {

    /**
     * 构建预警首页汇总数据。
     *
     * @return 汇总数据
     */
    HrWarningHomeSummaryVO buildWarningSummary();
}

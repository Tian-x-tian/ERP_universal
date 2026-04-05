package com.erp.system.service;

import com.erp.system.domain.vo.SystemHomeHealthSummaryVO;

/**
 * 系统首页汇总服务接口。
 */
public interface ISysHomeSummaryService {

    /**
     * 构建系统首页健康汇总数据。
     *
     * @return 健康汇总
     */
    SystemHomeHealthSummaryVO buildHealthSummary();
}

package com.erp.business.hr.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.erp.business.hr.domain.HrWarningRecord;
import com.erp.business.hr.domain.vo.HrWarningHandleBody;
import com.erp.business.hr.domain.vo.HrWarningQuery;

/**
 * HR 预警服务接口。
 */
public interface IHrWarningService {

    /**
     * 分页查询预警。
     *
     * @param query 查询参数
     * @return 分页结果
     */
    Page<HrWarningRecord> selectPage(HrWarningQuery query);

    /**
     * 触发预警扫描。
     */
    void scanWarnings();

    /**
     * 处理预警。
     *
     * @param warningId 预警ID
     * @param body 处理参数
     * @return 处理后的预警
     */
    HrWarningRecord handleWarning(Long warningId, HrWarningHandleBody body);
}

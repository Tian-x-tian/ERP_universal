package com.erp.system.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.erp.system.domain.SysImexJob;

/**
 * 导入导出任务服务接口。
 */
public interface ISysImexJobService {

    /**
     * 查询导入导出任务分页。
     *
     * @param jobType 任务类型
     * @param status 状态
     * @param moduleCode 模块编码
     * @param pageNum 页码
     * @param pageSize 页长
     * @return 分页结果
     */
    Page<SysImexJob> selectPage(String jobType, String status, String moduleCode, Long pageNum, Long pageSize);

    /**
     * 查询任务详情。
     *
     * @param jobId 任务ID
     * @return 任务详情
     */
    SysImexJob getDetail(Long jobId);
}

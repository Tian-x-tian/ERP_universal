package com.erp.system.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.erp.system.domain.SysImexJob;
import com.erp.platform.contract.model.PlatformImexJobCreateRequest;
import com.erp.platform.contract.model.PlatformImexJobUpdateRequest;

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

    /**
     * 通过内部契约创建导入导出任务。
     *
     * @param request 创建参数
     * @return 新建任务
     */
    SysImexJob createInternalJob(PlatformImexJobCreateRequest request);

    /**
     * 通过内部契约更新导入导出任务。
     *
     * @param jobId   任务ID
     * @param request 更新参数
     * @return 更新后的任务
     */
    SysImexJob updateInternalJob(Long jobId, PlatformImexJobUpdateRequest request);
}


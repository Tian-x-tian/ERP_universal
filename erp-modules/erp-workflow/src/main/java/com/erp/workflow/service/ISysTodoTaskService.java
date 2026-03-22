package com.erp.workflow.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.erp.workflow.contract.domain.SysTodoTask;

import java.util.List;

/**
 * 流程待办任务服务接口
 */
public interface ISysTodoTaskService extends IService<SysTodoTask> {

    /**
     * 查询当前用户待办任务。
     *
     * @param userId 当前用户ID
     * @param status 任务状态
     * @return 待办任务列表
     */
    List<SysTodoTask> selectByCurrentUser(Long userId, String status);

    /**
     * 签收待办任务。
     *
     * @param todoId 待办ID
     * @param userId 当前用户ID
     * @return 是否更新成功
     */
    boolean claim(Long todoId, Long userId);

    /**
     * 办结待办任务。
     *
     * @param todoId 待办ID
     * @param userId 当前用户ID
     * @return 是否更新成功
     */
    boolean finish(Long todoId, Long userId);
}



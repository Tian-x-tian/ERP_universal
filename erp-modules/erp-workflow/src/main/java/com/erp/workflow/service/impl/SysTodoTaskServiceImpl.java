package com.erp.workflow.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.erp.workflow.contract.domain.SysTodoTask;
import com.erp.workflow.mapper.SysTodoTaskMapper;
import com.erp.workflow.service.ISysTodoTaskService;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.Date;
import java.util.List;

/**
 * 流程待办任务服务实现
 */
@Service
public class SysTodoTaskServiceImpl extends ServiceImpl<SysTodoTaskMapper, SysTodoTask> implements ISysTodoTaskService {

    /**
     * 查询当前用户待办任务。
     *
     * @param userId 当前用户ID
     * @param status 任务状态
     * @return 待办任务列表
     */
    @Override
    public List<SysTodoTask> selectByCurrentUser(Long userId, String status) {
        LambdaQueryWrapper<SysTodoTask> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(SysTodoTask::getAssigneeUserId, userId);
        if (StringUtils.hasText(status)) {
            queryWrapper.eq(SysTodoTask::getStatus, status.trim());
        }
        queryWrapper.orderByAsc(SysTodoTask::getStatus)
                .orderByAsc(SysTodoTask::getDueTime)
                .orderByDesc(SysTodoTask::getCreateTime);
        return list(queryWrapper);
    }

    /**
     * 签收待办任务。
     *
     * @param todoId 待办ID
     * @param userId 当前用户ID
     * @return 是否更新成功
     */
    @Override
    public boolean claim(Long todoId, Long userId) {
        if (todoId == null || userId == null) {
            return false;
        }
        LambdaUpdateWrapper<SysTodoTask> updateWrapper = new LambdaUpdateWrapper<>();
        updateWrapper.eq(SysTodoTask::getTodoId, todoId)
                .eq(SysTodoTask::getAssigneeUserId, userId)
                .eq(SysTodoTask::getStatus, "0")
                .set(SysTodoTask::getStatus, "1")
                .set(SysTodoTask::getClaimTime, new Date());
        return update(updateWrapper);
    }

    /**
     * 办结待办任务。
     *
     * @param todoId 待办ID
     * @param userId 当前用户ID
     * @return 是否更新成功
     */
    @Override
    public boolean finish(Long todoId, Long userId) {
        if (todoId == null || userId == null) {
            return false;
        }
        LambdaUpdateWrapper<SysTodoTask> updateWrapper = new LambdaUpdateWrapper<>();
        updateWrapper.eq(SysTodoTask::getTodoId, todoId)
                .eq(SysTodoTask::getAssigneeUserId, userId)
                .in(SysTodoTask::getStatus, "0", "1")
                .set(SysTodoTask::getStatus, "2")
                .set(SysTodoTask::getFinishTime, new Date());
        return update(updateWrapper);
    }
}



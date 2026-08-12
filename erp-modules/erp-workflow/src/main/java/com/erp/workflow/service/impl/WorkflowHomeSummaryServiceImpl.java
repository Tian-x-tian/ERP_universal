package com.erp.workflow.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.erp.workflow.contract.domain.SysTodoTask;
import com.erp.workflow.contract.domain.vo.WorkflowHomeTodoSummaryVO;
import com.erp.workflow.mapper.SysTodoTaskMapper;
import com.erp.workflow.security.service.PermissionService;
import com.erp.workflow.security.service.SecurityUserResolver;
import com.erp.workflow.service.IWorkflowHomeSummaryService;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Date;
import java.util.List;

/**
 * 工作流首页汇总服务实现。
 */
@Service
public class WorkflowHomeSummaryServiceImpl implements IWorkflowHomeSummaryService {
    private static final String TODO_STATUS_PENDING = "0";
    private static final String TODO_STATUS_PROCESSING = "1";
    private static final String TODO_STATUS_COMPLETED = "2";

    private final SysTodoTaskMapper todoTaskMapper;
    private final PermissionService permissionService;
    private final SecurityUserResolver securityUserResolver;

    public WorkflowHomeSummaryServiceImpl(SysTodoTaskMapper todoTaskMapper,
            PermissionService permissionService,
            SecurityUserResolver securityUserResolver) {
        this.todoTaskMapper = todoTaskMapper;
        this.permissionService = permissionService;
        this.securityUserResolver = securityUserResolver;
    }

    /**
     * 构建当前用户首页待办汇总数据。
     *
     * @return 待办汇总
     */
    @Override
    public WorkflowHomeTodoSummaryVO buildTodoSummary() {
        if (!permissionService.hasPermi("workflow:todo:list")) {
            return emptySummary();
        }
        Long currentUserId = securityUserResolver.getCurrentUserId();
        if (currentUserId == null) {
            return emptySummary();
        }
        List<SysTodoTask> todoTaskList = todoTaskMapper.selectList(new LambdaQueryWrapper<SysTodoTask>()
                .eq(SysTodoTask::getAssigneeUserId, currentUserId));
        if (todoTaskList == null || todoTaskList.isEmpty()) {
            return emptySummary();
        }
        Date now = new Date();
        long pendingCount = 0L;
        long processingCount = 0L;
        long completedCount = 0L;
        long overdueCount = 0L;
        for (SysTodoTask todoTask : todoTaskList) {
            if (todoTask == null) {
                continue;
            }
            String status = todoTask.getStatus();
            if (TODO_STATUS_PENDING.equals(status)) {
                pendingCount += 1L;
            } else if (TODO_STATUS_PROCESSING.equals(status)) {
                processingCount += 1L;
            } else if (TODO_STATUS_COMPLETED.equals(status)) {
                completedCount += 1L;
            }
            if ((TODO_STATUS_PENDING.equals(status) || TODO_STATUS_PROCESSING.equals(status))
                    && todoTask.getDueTime() != null
                    && now.after(todoTask.getDueTime())) {
                overdueCount += 1L;
            }
        }
        long collaborationTotal = pendingCount + processingCount + completedCount;
        long collaborationDone = completedCount;
        WorkflowHomeTodoSummaryVO summaryVO = new WorkflowHomeTodoSummaryVO();
        summaryVO.setPendingCount(pendingCount);
        summaryVO.setProcessingCount(processingCount);
        summaryVO.setCompletedCount(completedCount);
        summaryVO.setOverdueCount(overdueCount);
        summaryVO.setCollaborationDone(collaborationDone);
        summaryVO.setCollaborationTotal(collaborationTotal);
        summaryVO.setCollaborationRate(calculateRate(collaborationDone, collaborationTotal));
        return summaryVO;
    }

    /**
     * 构建空安全汇总对象。
     *
     * @return 汇总对象
     */
    private WorkflowHomeTodoSummaryVO emptySummary() {
        WorkflowHomeTodoSummaryVO summaryVO = new WorkflowHomeTodoSummaryVO();
        summaryVO.setPendingCount(0L);
        summaryVO.setProcessingCount(0L);
        summaryVO.setCompletedCount(0L);
        summaryVO.setOverdueCount(0L);
        summaryVO.setCollaborationDone(0L);
        summaryVO.setCollaborationTotal(0L);
        summaryVO.setCollaborationRate(0D);
        return summaryVO;
    }

    /**
     * 计算百分比并统一保留两位小数。
     *
     * @param numerator 分子
     * @param denominator 分母
     * @return 百分比
     */
    private double calculateRate(long numerator, long denominator) {
        if (denominator <= 0L) {
            return 0D;
        }
        return BigDecimal.valueOf(numerator)
                .multiply(BigDecimal.valueOf(100))
                .divide(BigDecimal.valueOf(denominator), 2, RoundingMode.HALF_UP)
                .doubleValue();
    }
}

package com.erp.workflow.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.erp.workflow.domain.platform.SysDept;
import com.erp.workflow.domain.platform.SysUser;
import com.erp.workflow.contract.domain.SysWorkflowInstance;
import com.erp.workflow.contract.domain.SysWorkflowTask;
import com.erp.workflow.contract.domain.vo.WorkflowDashboardQueryVO;
import com.erp.workflow.contract.domain.vo.WorkflowDashboardSummaryVO;
import com.erp.workflow.contract.domain.vo.WorkflowDashboardVO;
import com.erp.workflow.contract.domain.vo.WorkflowDeptMetricVO;
import com.erp.workflow.contract.domain.vo.WorkflowNodeMetricVO;
import com.erp.workflow.contract.domain.vo.WorkflowProcessMetricVO;
import com.erp.workflow.mapper.SysWorkflowInstanceMapper;
import com.erp.workflow.mapper.SysWorkflowTaskMapper;
import com.erp.workflow.service.ISysDeptService;
import com.erp.workflow.service.ISysUserService;
import com.erp.workflow.service.ISysWorkflowAnalyticsService;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 流程分析服务实现。
 */
@Service
public class SysWorkflowAnalyticsServiceImpl implements ISysWorkflowAnalyticsService {

    private static final String INSTANCE_STATUS_RUNNING = "0";
    private static final String INSTANCE_STATUS_COMPLETED = "1";
    private static final String INSTANCE_STATUS_REJECTED = "2";

    private static final String TASK_STATUS_PENDING = "0";
    private static final String TASK_STATUS_PROCESSING = "1";
    private static final String TASK_STATUS_REJECTED = "3";

    private final SysWorkflowInstanceMapper workflowInstanceMapper;
    private final SysWorkflowTaskMapper workflowTaskMapper;
    private final ISysUserService userService;
    private final ISysDeptService deptService;

    public SysWorkflowAnalyticsServiceImpl(SysWorkflowInstanceMapper workflowInstanceMapper,
                                           SysWorkflowTaskMapper workflowTaskMapper,
                                           ISysUserService userService,
                                           ISysDeptService deptService) {
        this.workflowInstanceMapper = workflowInstanceMapper;
        this.workflowTaskMapper = workflowTaskMapper;
        this.userService = userService;
        this.deptService = deptService;
    }

    /**
     * 查询流程效率看板。
     *
     * @param queryVO 查询条件
     * @return 看板结果
     */
    @Override
    public WorkflowDashboardVO buildDashboard(WorkflowDashboardQueryVO queryVO) {
        LambdaQueryWrapper<SysWorkflowInstance> instanceWrapper = new LambdaQueryWrapper<SysWorkflowInstance>()
                .orderByDesc(SysWorkflowInstance::getStartTime)
                .orderByDesc(SysWorkflowInstance::getInstanceId);
        if (queryVO != null) {
            if (queryVO.getStartTime() != null) {
                instanceWrapper.ge(SysWorkflowInstance::getStartTime, queryVO.getStartTime());
            }
            if (queryVO.getEndTime() != null) {
                instanceWrapper.le(SysWorkflowInstance::getStartTime, queryVO.getEndTime());
            }
            if (queryVO.getCategory() != null && !queryVO.getCategory().trim().isEmpty()) {
                instanceWrapper.eq(SysWorkflowInstance::getCategory, queryVO.getCategory().trim());
            }
            if (queryVO.getProcessKey() != null && !queryVO.getProcessKey().trim().isEmpty()) {
                instanceWrapper.like(SysWorkflowInstance::getProcessKey, queryVO.getProcessKey().trim());
            }
        }
        List<SysWorkflowInstance> instanceList = workflowInstanceMapper.selectList(instanceWrapper);
        Map<Long, SysUser> userMap = loadUserMap(instanceList);
        if (queryVO != null && queryVO.getDeptId() != null) {
            instanceList = filterInstanceListByDept(instanceList, userMap, queryVO.getDeptId());
        }
        List<SysWorkflowTask> taskList = loadTaskList(instanceList);

        Map<Long, List<SysWorkflowTask>> taskMapByInstance = taskList.stream()
                .filter(task -> task.getInstanceId() != null)
                .collect(Collectors.groupingBy(SysWorkflowTask::getInstanceId));
        Map<Long, SysDept> deptMap = loadDeptMap(userMap.values());

        WorkflowDashboardVO dashboard = new WorkflowDashboardVO();
        dashboard.setNodeMetrics(buildNodeMetrics(taskList));
        dashboard.setProcessMetrics(buildProcessMetrics(instanceList, taskMapByInstance));
        dashboard.setDeptMetrics(buildDeptMetrics(instanceList, taskMapByInstance, userMap, deptMap));
        dashboard.setSummary(buildSummary(instanceList, taskList, dashboard.getNodeMetrics()));
        return dashboard;
    }

    /**
     * 按部门筛选流程实例。
     *
     * @param instanceList 实例列表
     * @param userMap      用户映射
     * @param deptId       部门ID
     * @return 过滤后的实例列表
     */
    private List<SysWorkflowInstance> filterInstanceListByDept(List<SysWorkflowInstance> instanceList,
                                                               Map<Long, SysUser> userMap,
                                                               Long deptId) {
        if (deptId == null || instanceList == null || instanceList.isEmpty()) {
            return instanceList;
        }
        return instanceList.stream()
                .filter(instance -> {
                    if (instance == null || instance.getInitiatorUserId() == null) {
                        return false;
                    }
                    SysUser user = userMap.get(instance.getInitiatorUserId());
                    return user != null && Objects.equals(user.getDeptId(), deptId);
                })
                .collect(Collectors.toList());
    }

    /**
     * 按实例范围加载任务列表。
     *
     * @param instanceList 实例列表
     * @return 任务列表
     */
    private List<SysWorkflowTask> loadTaskList(List<SysWorkflowInstance> instanceList) {
        if (instanceList == null || instanceList.isEmpty()) {
            return Collections.emptyList();
        }
        Set<Long> instanceIdSet = instanceList.stream()
                .map(SysWorkflowInstance::getInstanceId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        if (instanceIdSet.isEmpty()) {
            return Collections.emptyList();
        }
        return workflowTaskMapper.selectList(new LambdaQueryWrapper<SysWorkflowTask>()
                .in(SysWorkflowTask::getInstanceId, instanceIdSet)
                .orderByDesc(SysWorkflowTask::getCreateTime)
                .orderByDesc(SysWorkflowTask::getTaskId));
    }

    /**
     * 构建汇总指标。
     *
     * @param instanceList 实例列表
     * @param taskList     任务列表
     * @param nodeMetrics  节点指标
     * @return 汇总指标
     */
    private WorkflowDashboardSummaryVO buildSummary(List<SysWorkflowInstance> instanceList,
                                                    List<SysWorkflowTask> taskList,
                                                    List<WorkflowNodeMetricVO> nodeMetrics) {
        WorkflowDashboardSummaryVO summary = new WorkflowDashboardSummaryVO();
        summary.setTotalInstances(instanceList.size());
        summary.setRunningInstances(instanceList.stream()
                .filter(instance -> INSTANCE_STATUS_RUNNING.equals(instance.getStatus()))
                .count());
        summary.setCompletedInstances(instanceList.stream()
                .filter(instance -> INSTANCE_STATUS_COMPLETED.equals(instance.getStatus()))
                .count());
        summary.setAverageApprovalHours(round(calculateAverageTaskHours(taskList)));
        long rejectedInstances = instanceList.stream()
                .filter(instance -> INSTANCE_STATUS_REJECTED.equals(instance.getStatus()))
                .count();
        summary.setRejectRate(calculateRate(rejectedInstances, instanceList.size()));
        long overduePendingCount = taskList.stream()
                .filter(this::isPendingTask)
                .filter(this::isTaskOvertime)
                .count();
        summary.setOverduePendingCount(overduePendingCount);
        long overtimeTaskCount = taskList.stream().filter(this::isTaskOvertime).count();
        long timedTaskCount = taskList.stream().filter(task -> task.getDueTime() != null).count();
        summary.setOvertimeRate(calculateRate(overtimeTaskCount, timedTaskCount));
        if (!nodeMetrics.isEmpty()) {
            summary.setBottleneckNodeName(nodeMetrics.get(0).getNodeName());
        }
        return summary;
    }

    /**
     * 构建节点维度效率指标。
     *
     * @param taskList 任务列表
     * @return 节点指标列表
     */
    private List<WorkflowNodeMetricVO> buildNodeMetrics(List<SysWorkflowTask> taskList) {
        Map<String, MetricAccumulator> accumulatorMap = new LinkedHashMap<>();
        for (SysWorkflowTask task : taskList) {
            String nodeKey = task.getNodeKey() == null ? "-" : task.getNodeKey();
            String mapKey = nodeKey + "#" + (task.getNodeName() == null ? "-" : task.getNodeName());
            MetricAccumulator accumulator = accumulatorMap.computeIfAbsent(mapKey, key -> new MetricAccumulator());
            accumulator.key = nodeKey;
            accumulator.name = task.getNodeName() == null ? nodeKey : task.getNodeName();
            accumulator.taskCount += 1;
            if (task.getFinishTime() != null && task.getCreateTime() != null) {
                accumulator.totalHours += calculateHours(task.getCreateTime(), task.getFinishTime());
                accumulator.finishedCount += 1;
            }
            if (isTaskOvertime(task)) {
                accumulator.overtimeCount += 1;
            }
            if (TASK_STATUS_REJECTED.equals(task.getStatus())) {
                accumulator.rejectCount += 1;
            }
        }
        List<WorkflowNodeMetricVO> metricList = new ArrayList<>();
        for (MetricAccumulator accumulator : accumulatorMap.values()) {
            WorkflowNodeMetricVO metricVO = new WorkflowNodeMetricVO();
            metricVO.setNodeKey(accumulator.key);
            metricVO.setNodeName(accumulator.name);
            metricVO.setTaskCount(accumulator.taskCount);
            metricVO.setAverageHours(round(accumulator.finishedCount == 0 ? 0D : accumulator.totalHours / accumulator.finishedCount));
            metricVO.setOvertimeCount(accumulator.overtimeCount);
            metricVO.setRejectCount(accumulator.rejectCount);
            metricList.add(metricVO);
        }
        metricList.sort(Comparator.comparingDouble(WorkflowNodeMetricVO::getAverageHours).reversed()
                .thenComparingLong(WorkflowNodeMetricVO::getTaskCount).reversed());
        return metricList;
    }

    /**
     * 构建流程维度效率指标。
     *
     * @param instanceList       实例列表
     * @param taskMapByInstance  任务映射
     * @return 流程指标列表
     */
    private List<WorkflowProcessMetricVO> buildProcessMetrics(List<SysWorkflowInstance> instanceList,
                                                              Map<Long, List<SysWorkflowTask>> taskMapByInstance) {
        Map<String, ProcessAccumulator> accumulatorMap = new LinkedHashMap<>();
        for (SysWorkflowInstance instance : instanceList) {
            String processKey = instance.getProcessKey() == null ? "-" : instance.getProcessKey();
            ProcessAccumulator accumulator = accumulatorMap.computeIfAbsent(processKey, key -> new ProcessAccumulator());
            accumulator.processKey = processKey;
            accumulator.processName = instance.getProcessName() == null ? processKey : instance.getProcessName();
            accumulator.instanceCount += 1;
            accumulator.totalInstanceHours += calculateInstanceHours(instance);
            if (INSTANCE_STATUS_REJECTED.equals(instance.getStatus())) {
                accumulator.rejectedCount += 1;
            }
            List<SysWorkflowTask> taskList = taskMapByInstance.getOrDefault(instance.getInstanceId(), Collections.emptyList());
            accumulator.timedTaskCount += taskList.stream().filter(task -> task.getDueTime() != null).count();
            accumulator.overtimeTaskCount += taskList.stream().filter(this::isTaskOvertime).count();
        }
        List<WorkflowProcessMetricVO> metricList = new ArrayList<>();
        for (ProcessAccumulator accumulator : accumulatorMap.values()) {
            WorkflowProcessMetricVO metricVO = new WorkflowProcessMetricVO();
            metricVO.setProcessKey(accumulator.processKey);
            metricVO.setProcessName(accumulator.processName);
            metricVO.setInstanceCount(accumulator.instanceCount);
            metricVO.setAverageHours(round(accumulator.instanceCount == 0 ? 0D : accumulator.totalInstanceHours / accumulator.instanceCount));
            metricVO.setRejectRate(calculateRate(accumulator.rejectedCount, accumulator.instanceCount));
            metricVO.setOvertimeRate(calculateRate(accumulator.overtimeTaskCount, accumulator.timedTaskCount));
            metricList.add(metricVO);
        }
        metricList.sort(Comparator.comparingLong(WorkflowProcessMetricVO::getInstanceCount).reversed()
                .thenComparingDouble(WorkflowProcessMetricVO::getAverageHours).reversed());
        return metricList;
    }

    /**
     * 构建部门对比指标。
     *
     * @param instanceList      实例列表
     * @param taskMapByInstance 任务映射
     * @param userMap           用户映射
     * @param deptMap           部门映射
     * @return 部门指标列表
     */
    private List<WorkflowDeptMetricVO> buildDeptMetrics(List<SysWorkflowInstance> instanceList,
                                                        Map<Long, List<SysWorkflowTask>> taskMapByInstance,
                                                        Map<Long, SysUser> userMap,
                                                        Map<Long, SysDept> deptMap) {
        Map<Long, DeptAccumulator> accumulatorMap = new HashMap<>();
        for (SysWorkflowInstance instance : instanceList) {
            Long initiatorUserId = instance.getInitiatorUserId();
            SysUser user = initiatorUserId == null ? null : userMap.get(initiatorUserId);
            Long deptId = user == null ? 0L : (user.getDeptId() == null ? 0L : user.getDeptId());
            DeptAccumulator accumulator = accumulatorMap.computeIfAbsent(deptId, key -> new DeptAccumulator());
            accumulator.deptId = deptId;
            SysDept dept = deptMap.get(deptId);
            accumulator.deptName = dept == null ? "未分配部门" : dept.getDeptName();
            accumulator.instanceCount += 1;
            accumulator.totalInstanceHours += calculateInstanceHours(instance);
            if (INSTANCE_STATUS_REJECTED.equals(instance.getStatus())) {
                accumulator.rejectedCount += 1;
            }
            List<SysWorkflowTask> taskList = taskMapByInstance.getOrDefault(instance.getInstanceId(), Collections.emptyList());
            accumulator.timedTaskCount += taskList.stream().filter(task -> task.getDueTime() != null).count();
            accumulator.overtimeTaskCount += taskList.stream().filter(this::isTaskOvertime).count();
        }
        List<WorkflowDeptMetricVO> metricList = new ArrayList<>();
        for (DeptAccumulator accumulator : accumulatorMap.values()) {
            WorkflowDeptMetricVO metricVO = new WorkflowDeptMetricVO();
            metricVO.setDeptId(accumulator.deptId);
            metricVO.setDeptName(accumulator.deptName);
            metricVO.setInstanceCount(accumulator.instanceCount);
            metricVO.setAverageHours(round(accumulator.instanceCount == 0 ? 0D : accumulator.totalInstanceHours / accumulator.instanceCount));
            metricVO.setRejectRate(calculateRate(accumulator.rejectedCount, accumulator.instanceCount));
            metricVO.setOvertimeRate(calculateRate(accumulator.overtimeTaskCount, accumulator.timedTaskCount));
            metricList.add(metricVO);
        }
        metricList.sort(Comparator.comparingLong(WorkflowDeptMetricVO::getInstanceCount).reversed()
                .thenComparingDouble(WorkflowDeptMetricVO::getAverageHours).reversed());
        return metricList;
    }

    /**
     * 加载实例发起人映射。
     *
     * @param instanceList 实例列表
     * @return 用户映射
     */
    private Map<Long, SysUser> loadUserMap(List<SysWorkflowInstance> instanceList) {
        Set<Long> userIdSet = instanceList.stream()
                .map(SysWorkflowInstance::getInitiatorUserId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        if (userIdSet.isEmpty()) {
            return Collections.emptyMap();
        }
        return userService.listByIds(userIdSet).stream()
                .collect(Collectors.toMap(SysUser::getUserId, user -> user, (left, right) -> left));
    }

    /**
     * 加载部门映射。
     *
     * @param userCollection 用户集合
     * @return 部门映射
     */
    private Map<Long, SysDept> loadDeptMap(Collection<SysUser> userCollection) {
        Set<Long> deptIdSet = userCollection.stream()
                .map(SysUser::getDeptId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        if (deptIdSet.isEmpty()) {
            return Collections.emptyMap();
        }
        return deptService.listByIds(deptIdSet).stream()
                .collect(Collectors.toMap(SysDept::getDeptId, dept -> dept, (left, right) -> left));
    }

    /**
     * 计算流程实例耗时。
     *
     * @param instance 实例对象
     * @return 耗时小时
     */
    private double calculateInstanceHours(SysWorkflowInstance instance) {
        if (instance == null || instance.getStartTime() == null) {
            return 0D;
        }
        Date endTime = instance.getFinishTime() == null ? new Date() : instance.getFinishTime();
        return calculateHours(instance.getStartTime(), endTime);
    }

    /**
     * 计算任务平均耗时。
     *
     * @param taskList 任务列表
     * @return 平均小时数
     */
    private double calculateAverageTaskHours(List<SysWorkflowTask> taskList) {
        double totalHours = 0D;
        long finishedCount = 0L;
        for (SysWorkflowTask task : taskList) {
            if (task.getCreateTime() == null || task.getFinishTime() == null) {
                continue;
            }
            totalHours += calculateHours(task.getCreateTime(), task.getFinishTime());
            finishedCount += 1;
        }
        if (finishedCount == 0L) {
            return 0D;
        }
        return totalHours / finishedCount;
    }

    /**
     * 判断任务是否仍处于待处理状态。
     *
     * @param task 任务对象
     * @return true 表示待处理
     */
    private boolean isPendingTask(SysWorkflowTask task) {
        return task != null && (TASK_STATUS_PENDING.equals(task.getStatus()) || TASK_STATUS_PROCESSING.equals(task.getStatus()));
    }

    /**
     * 判断任务是否发生超时。
     *
     * @param task 任务对象
     * @return true 表示超时
     */
    private boolean isTaskOvertime(SysWorkflowTask task) {
        if (task == null || task.getDueTime() == null) {
            return false;
        }
        Date compareTime = task.getFinishTime() == null ? new Date() : task.getFinishTime();
        return compareTime.after(task.getDueTime());
    }

    /**
     * 计算百分比。
     *
     * @param numerator   分子
     * @param denominator 分母
     * @return 百分比
     */
    private double calculateRate(long numerator, long denominator) {
        if (denominator <= 0L) {
            return 0D;
        }
        return round((double) numerator * 100D / (double) denominator);
    }

    /**
     * 计算小时差。
     *
     * @param startTime 开始时间
     * @param endTime   结束时间
     * @return 小时差
     */
    private double calculateHours(Date startTime, Date endTime) {
        if (startTime == null || endTime == null) {
            return 0D;
        }
        long diffMillis = Math.max(0L, endTime.getTime() - startTime.getTime());
        return (double) diffMillis / 3600000D;
    }

    /**
     * 统一保留两位小数。
     *
     * @param value 原始值
     * @return 处理后的数值
     */
    private double round(double value) {
        return Math.round(value * 100D) / 100D;
    }

    /**
     * 通用节点指标累计器。
     */
    private static class MetricAccumulator {
        private String key;
        private String name;
        private long taskCount;
        private long finishedCount;
        private double totalHours;
        private long overtimeCount;
        private long rejectCount;
    }

    /**
     * 流程指标累计器。
     */
    private static class ProcessAccumulator {
        private String processKey;
        private String processName;
        private long instanceCount;
        private double totalInstanceHours;
        private long rejectedCount;
        private long timedTaskCount;
        private long overtimeTaskCount;
    }

    /**
     * 部门指标累计器。
     */
    private static class DeptAccumulator {
        private Long deptId;
        private String deptName;
        private long instanceCount;
        private double totalInstanceHours;
        private long rejectedCount;
        private long timedTaskCount;
        private long overtimeTaskCount;
    }
}




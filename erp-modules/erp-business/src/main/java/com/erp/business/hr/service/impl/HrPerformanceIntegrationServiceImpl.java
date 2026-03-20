package com.erp.business.hr.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.erp.business.hr.domain.HrEmployeeCore;
import com.erp.business.hr.domain.HrPerformanceFieldMapping;
import com.erp.business.hr.domain.HrPerformanceRetryTask;
import com.erp.business.hr.domain.HrPerformanceSyncLog;
import com.erp.business.hr.domain.vo.HrPerformanceCallbackBody;
import com.erp.business.hr.domain.vo.HrPerformancePushBody;
import com.erp.business.hr.mapper.HrEmployeeCoreMapper;
import com.erp.business.hr.mapper.HrPerformanceFieldMappingMapper;
import com.erp.business.hr.mapper.HrPerformanceRetryTaskMapper;
import com.erp.business.hr.mapper.HrPerformanceSyncLogMapper;
import com.erp.business.hr.service.IHrPerformanceIntegrationService;
import com.erp.business.hr.support.HrEmployeeSupport;
import com.erp.business.security.service.SecurityUserResolver;
import com.erp.common.core.domain.ResultCode;
import com.erp.common.core.exception.ServiceException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 绩效考核同步服务实现。
 */
@Service
public class HrPerformanceIntegrationServiceImpl implements IHrPerformanceIntegrationService {
    private final HrPerformanceFieldMappingMapper fieldMappingMapper;
    private final HrPerformanceSyncLogMapper syncLogMapper;
    private final HrPerformanceRetryTaskMapper retryTaskMapper;
    private final HrEmployeeCoreMapper employeeCoreMapper;
    private final SecurityUserResolver securityUserResolver;
    private final ObjectMapper objectMapper;
    private final RestTemplate restTemplate;

    @Value("${hr.integration.performance.push-url:}")
    private String pushUrl;

    @Value("${hr.integration.performance.callback-token:}")
    private String callbackToken;

    @Autowired
    public HrPerformanceIntegrationServiceImpl(HrPerformanceFieldMappingMapper fieldMappingMapper,
            HrPerformanceSyncLogMapper syncLogMapper,
            HrPerformanceRetryTaskMapper retryTaskMapper,
            HrEmployeeCoreMapper employeeCoreMapper,
            SecurityUserResolver securityUserResolver) {
        this(fieldMappingMapper, syncLogMapper, retryTaskMapper, employeeCoreMapper, securityUserResolver,
                new ObjectMapper(), new RestTemplate());
    }

    HrPerformanceIntegrationServiceImpl(HrPerformanceFieldMappingMapper fieldMappingMapper,
            HrPerformanceSyncLogMapper syncLogMapper,
            HrPerformanceRetryTaskMapper retryTaskMapper,
            HrEmployeeCoreMapper employeeCoreMapper,
            SecurityUserResolver securityUserResolver,
            ObjectMapper objectMapper,
            RestTemplate restTemplate) {
        this.fieldMappingMapper = fieldMappingMapper;
        this.syncLogMapper = syncLogMapper;
        this.retryTaskMapper = retryTaskMapper;
        this.employeeCoreMapper = employeeCoreMapper;
        this.securityUserResolver = securityUserResolver;
        this.objectMapper = objectMapper == null ? new ObjectMapper() : objectMapper;
        this.restTemplate = restTemplate == null ? new RestTemplate() : restTemplate;
    }

    /**
     * 查询映射配置。
     *
     * @return 映射配置列表
     */
    @Override
    public List<HrPerformanceFieldMapping> listMappings() {
        return fieldMappingMapper.selectList(new LambdaQueryWrapper<HrPerformanceFieldMapping>()
                .eq(StringUtils.hasText(currentTenantId()), HrPerformanceFieldMapping::getTenantId, currentTenantId())
                .orderByAsc(HrPerformanceFieldMapping::getDirection)
                .orderByAsc(HrPerformanceFieldMapping::getSortNo)
                .orderByAsc(HrPerformanceFieldMapping::getMappingId));
    }

    /**
     * 保存映射配置。
     *
     * @param mappings 映射配置
     * @return 最新配置
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public List<HrPerformanceFieldMapping> saveMappings(List<HrPerformanceFieldMapping> mappings) {
        fieldMappingMapper.delete(new LambdaQueryWrapper<HrPerformanceFieldMapping>()
                .eq(StringUtils.hasText(currentTenantId()), HrPerformanceFieldMapping::getTenantId, currentTenantId()));
        Date now = new Date();
        if (mappings != null) {
            int sortNo = 1;
            for (HrPerformanceFieldMapping mapping : mappings) {
                if (mapping == null) {
                    continue;
                }
                mapping.setMappingId(null);
                mapping.setTenantId(currentTenantId());
                mapping.setSortNo(mapping.getSortNo() == null ? sortNo++ : mapping.getSortNo());
                mapping.setStatus(HrEmployeeSupport.defaultIfBlank(
                        HrEmployeeSupport.normalizeStatus(mapping.getStatus()), HrEmployeeSupport.STATUS_ACTIVE));
                mapping.setCreateBy(resolveOperator());
                mapping.setCreateTime(now);
                mapping.setUpdateBy(resolveOperator());
                mapping.setUpdateTime(now);
                fieldMappingMapper.insert(mapping);
            }
        }
        return listMappings();
    }

    /**
     * 发起绩效推送。
     *
     * @param body 推送参数
     * @return 生成的同步日志
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public List<HrPerformanceSyncLog> pushPerformance(HrPerformancePushBody body) {
        List<HrEmployeeCore> employees = loadTargetEmployees(body);
        List<HrPerformanceSyncLog> logs = new ArrayList<>();
        for (HrEmployeeCore employee : employees) {
            HrPerformanceSyncLog log = createPendingLog(employee, body);
            executePush(log, employee, body);
            logs.add(syncLogMapper.selectById(log.getLogId()));
        }
        return logs;
    }

    /**
     * 处理绩效回传。
     *
     * @param body 回传参数
     * @return 最新日志
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public HrPerformanceSyncLog callback(HrPerformanceCallbackBody body) {
        HrPerformanceSyncLog log = loadLog(body);
        HrPerformanceSyncLog updateEntity = new HrPerformanceSyncLog();
        updateEntity.setLogId(log.getLogId());
        updateEntity.setSyncStatus(HrEmployeeSupport.defaultIfBlank(
                HrEmployeeSupport.normalizeStatus(body == null ? null : body.getSyncStatus()),
                HrEmployeeSupport.PERFORMANCE_SYNC_STATUS_SUCCESS));
        updateEntity.setResponseJson(HrEmployeeSupport.trimToNull(body == null ? null : body.getPayloadJson()));
        updateEntity.setLastError(HrEmployeeSupport.trimToNull(body == null ? null : body.getResultMessage()));
        updateEntity.setUpdateBy(resolveOperator());
        updateEntity.setUpdateTime(new Date());
        syncLogMapper.updateById(updateEntity);
        return syncLogMapper.selectById(log.getLogId());
    }

    /**
     * 分页查询同步日志。
     *
     * @param periodCode 期间
     * @param syncStatus 状态
     * @param employeeId 员工ID
     * @param pageNum 页码
     * @param pageSize 页长
     * @return 分页结果
     */
    @Override
    public Page<HrPerformanceSyncLog> selectLogPage(String periodCode, String syncStatus, Long employeeId, Long pageNum, Long pageSize) {
        Page<HrPerformanceSyncLog> page = new Page<>(
                HrEmployeeSupport.normalizePageNum(pageNum),
                HrEmployeeSupport.normalizePageSize(pageSize));
        return syncLogMapper.selectPage(page, new LambdaQueryWrapper<HrPerformanceSyncLog>()
                .eq(StringUtils.hasText(currentTenantId()), HrPerformanceSyncLog::getTenantId, currentTenantId())
                .eq(StringUtils.hasText(periodCode), HrPerformanceSyncLog::getPeriodCode, HrEmployeeSupport.trimToNull(periodCode))
                .eq(StringUtils.hasText(syncStatus), HrPerformanceSyncLog::getSyncStatus, HrEmployeeSupport.normalizeStatus(syncStatus))
                .eq(employeeId != null, HrPerformanceSyncLog::getEmployeeId, employeeId)
                .orderByDesc(HrPerformanceSyncLog::getUpdateTime)
                .orderByDesc(HrPerformanceSyncLog::getCreateTime));
    }

    /**
     * 重试指定日志。
     *
     * @param logId 日志ID
     * @return 最新日志
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public HrPerformanceSyncLog retry(Long logId) {
        HrPerformanceSyncLog log = syncLogMapper.selectOne(new LambdaQueryWrapper<HrPerformanceSyncLog>()
                .eq(HrPerformanceSyncLog::getLogId, logId)
                .eq(StringUtils.hasText(currentTenantId()), HrPerformanceSyncLog::getTenantId, currentTenantId()));
        if (log == null) {
            throw new ServiceException("绩效同步日志不存在", (int) ResultCode.NOT_FOUND.getCode());
        }
        HrPerformanceRetryTask retryTask = new HrPerformanceRetryTask();
        retryTask.setTenantId(log.getTenantId());
        retryTask.setLogId(logId);
        retryTask.setTaskStatus(HrEmployeeSupport.PERFORMANCE_SYNC_STATUS_RETRYING);
        retryTask.setRetryCount(log.getRetryCount() == null ? 1 : log.getRetryCount() + 1);
        retryTask.setNextRetryTime(new Date());
        retryTask.setCreateBy(resolveOperator());
        retryTask.setCreateTime(new Date());
        retryTask.setUpdateBy(resolveOperator());
        retryTask.setUpdateTime(new Date());
        retryTaskMapper.insert(retryTask);
        HrPerformanceSyncLog updateEntity = new HrPerformanceSyncLog();
        updateEntity.setLogId(logId);
        updateEntity.setSyncStatus(HrEmployeeSupport.PERFORMANCE_SYNC_STATUS_RETRYING);
        updateEntity.setRetryCount(retryTask.getRetryCount());
        updateEntity.setUpdateBy(resolveOperator());
        updateEntity.setUpdateTime(new Date());
        syncLogMapper.updateById(updateEntity);
        executePush(syncLogMapper.selectById(logId), null, null);
        return syncLogMapper.selectById(logId);
    }

    /**
     * 加载目标员工。
     *
     * @param body 推送参数
     * @return 员工列表
     */
    private List<HrEmployeeCore> loadTargetEmployees(HrPerformancePushBody body) {
        LambdaQueryWrapper<HrEmployeeCore> queryWrapper = new LambdaQueryWrapper<HrEmployeeCore>()
                .eq(HrEmployeeCore::getDelFlag, HrEmployeeSupport.EXIST_DEL_FLAG)
                .eq(HrEmployeeCore::getStatus, HrEmployeeSupport.STATUS_ACTIVE)
                .eq(StringUtils.hasText(currentTenantId()), HrEmployeeCore::getTenantId, currentTenantId());
        if (body != null && body.getEmployeeIds() != null && !body.getEmployeeIds().isEmpty()) {
            queryWrapper.in(HrEmployeeCore::getEmployeeId, body.getEmployeeIds());
        }
        return employeeCoreMapper.selectList(queryWrapper);
    }

    /**
     * 创建待推送日志。
     *
     * @param employee 员工主档
     * @param body 推送参数
     * @return 日志
     */
    private HrPerformanceSyncLog createPendingLog(HrEmployeeCore employee, HrPerformancePushBody body) {
        Date now = new Date();
        HrPerformanceSyncLog log = new HrPerformanceSyncLog();
        log.setTenantId(employee.getTenantId());
        log.setEmployeeId(employee.getEmployeeId());
        log.setDirection("ERP_TO_PERFORMANCE");
        log.setPeriodCode(body == null ? null : HrEmployeeSupport.trimToNull(body.getPeriodCode()));
        log.setSyncStatus(HrEmployeeSupport.PERFORMANCE_SYNC_STATUS_PENDING);
        log.setRequestNo("PERFORMANCE-" + employee.getEmployeeId() + "-" + System.currentTimeMillis());
        log.setPayloadJson(writePayload(buildPayload(employee, body)));
        log.setRetryCount(0);
        log.setCreateBy(resolveOperator());
        log.setCreateTime(now);
        log.setUpdateBy(resolveOperator());
        log.setUpdateTime(now);
        syncLogMapper.insert(log);
        return log;
    }

    /**
     * 执行推送。
     *
     * @param log 日志
     * @param employee 员工主档
     * @param body 推送参数
     */
    private void executePush(HrPerformanceSyncLog log, HrEmployeeCore employee, HrPerformancePushBody body) {
        if (log == null) {
            return;
        }
        if (!StringUtils.hasText(pushUrl)) {
            markLogFailed(log.getLogId(), "未配置绩效推送地址");
            return;
        }
        try {
            String payload = StringUtils.hasText(log.getPayloadJson())
                    ? log.getPayloadJson()
                    : writePayload(buildPayload(employee, body));
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            if (StringUtils.hasText(callbackToken)) {
                headers.setBearerAuth(callbackToken.trim());
            }
            String response = restTemplate.postForObject(pushUrl.trim(), new HttpEntity<>(payload, headers), String.class);
            HrPerformanceSyncLog updateEntity = new HrPerformanceSyncLog();
            updateEntity.setLogId(log.getLogId());
            updateEntity.setSyncStatus(HrEmployeeSupport.PERFORMANCE_SYNC_STATUS_SUCCESS);
            updateEntity.setResponseJson(HrEmployeeSupport.trimToNull(response));
            updateEntity.setUpdateBy(resolveOperator());
            updateEntity.setUpdateTime(new Date());
            syncLogMapper.updateById(updateEntity);
        } catch (Exception ex) {
            markLogFailed(log.getLogId(), ex.getMessage());
        }
    }

    /**
     * 构建推送载荷。
     *
     * @param employee 员工主档
     * @param body 推送参数
     * @return 载荷
     */
    private Map<String, Object> buildPayload(HrEmployeeCore employee, HrPerformancePushBody body) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("employeeId", employee == null ? null : employee.getEmployeeId());
        payload.put("empCode", employee == null ? null : employee.getEmpCode());
        payload.put("empName", employee == null ? null : employee.getEmpName());
        payload.put("orgId", employee == null ? null : employee.getOrgId());
        payload.put("deptId", employee == null ? null : employee.getDeptId());
        payload.put("position", employee == null ? null : employee.getPosition());
        payload.put("periodCode", body == null ? null : body.getPeriodCode());
        payload.put("triggerType", body == null ? null : body.getTriggerType());
        payload.put("mappings", listMappings());
        return payload;
    }

    /**
     * 标记日志失败。
     *
     * @param logId 日志ID
     * @param message 错误消息
     */
    private void markLogFailed(Long logId, String message) {
        HrPerformanceSyncLog updateEntity = new HrPerformanceSyncLog();
        updateEntity.setLogId(logId);
        updateEntity.setSyncStatus(HrEmployeeSupport.PERFORMANCE_SYNC_STATUS_FAILED);
        updateEntity.setLastError(HrEmployeeSupport.trimToNull(message));
        updateEntity.setUpdateBy(resolveOperator());
        updateEntity.setUpdateTime(new Date());
        syncLogMapper.updateById(updateEntity);
    }

    /**
     * 加载目标日志。
     *
     * @param body 回传参数
     * @return 同步日志
     */
    private HrPerformanceSyncLog loadLog(HrPerformanceCallbackBody body) {
        LambdaQueryWrapper<HrPerformanceSyncLog> queryWrapper = new LambdaQueryWrapper<HrPerformanceSyncLog>()
                .eq(StringUtils.hasText(currentTenantId()), HrPerformanceSyncLog::getTenantId, currentTenantId());
        if (body != null && body.getLogId() != null) {
            queryWrapper.eq(HrPerformanceSyncLog::getLogId, body.getLogId());
        } else if (body != null && StringUtils.hasText(body.getExternalBizNo())) {
            queryWrapper.eq(HrPerformanceSyncLog::getRequestNo, body.getExternalBizNo().trim());
        } else {
            throw new IllegalArgumentException("绩效回传必须提供日志ID或外部业务号");
        }
        HrPerformanceSyncLog log = syncLogMapper.selectOne(queryWrapper.last("limit 1"));
        if (log == null) {
            throw new ServiceException("绩效同步日志不存在", (int) ResultCode.NOT_FOUND.getCode());
        }
        return log;
    }

    /**
     * 序列化载荷。
     *
     * @param payload 载荷对象
     * @return JSON
     */
    private String writePayload(Object payload) {
        try {
            return objectMapper.writeValueAsString(payload);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("绩效同步载荷序列化失败", ex);
        }
    }

    /**
     * 获取当前租户编号。
     *
     * @return 租户编号
     */
    private String currentTenantId() {
        return securityUserResolver.getCurrentTenantId();
    }

    /**
     * 获取当前操作人。
     *
     * @return 操作人
     */
    private String resolveOperator() {
        String username = securityUserResolver.getCurrentUsername();
        return StringUtils.hasText(username) ? username.trim() : "system";
    }
}


package com.erp.business.hr.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.erp.business.hr.domain.HrEmployeeCore;
import com.erp.business.hr.domain.HrSalaryFieldMapping;
import com.erp.business.hr.domain.HrSalaryRetryTask;
import com.erp.business.hr.domain.HrSalarySyncLog;
import com.erp.business.hr.domain.vo.HrSalaryCallbackBody;
import com.erp.business.hr.domain.vo.HrSalaryPushBody;
import com.erp.business.hr.mapper.HrEmployeeCoreMapper;
import com.erp.business.hr.mapper.HrSalaryFieldMappingMapper;
import com.erp.business.hr.mapper.HrSalaryRetryTaskMapper;
import com.erp.business.hr.mapper.HrSalarySyncLogMapper;
import com.erp.business.hr.service.IHrSalaryIntegrationService;
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
 * 薪酬核算同步服务实现。
 */
@Service
public class HrSalaryIntegrationServiceImpl implements IHrSalaryIntegrationService {
    private final HrSalaryFieldMappingMapper fieldMappingMapper;
    private final HrSalarySyncLogMapper syncLogMapper;
    private final HrSalaryRetryTaskMapper retryTaskMapper;
    private final HrEmployeeCoreMapper employeeCoreMapper;
    private final SecurityUserResolver securityUserResolver;
    private final ObjectMapper objectMapper;
    private final RestTemplate restTemplate;

    @Value("${hr.integration.salary.push-url:}")
    private String pushUrl;

    @Value("${hr.integration.salary.callback-token:}")
    private String callbackToken;

    @Autowired
    public HrSalaryIntegrationServiceImpl(HrSalaryFieldMappingMapper fieldMappingMapper,
            HrSalarySyncLogMapper syncLogMapper,
            HrSalaryRetryTaskMapper retryTaskMapper,
            HrEmployeeCoreMapper employeeCoreMapper,
            SecurityUserResolver securityUserResolver) {
        this(fieldMappingMapper, syncLogMapper, retryTaskMapper, employeeCoreMapper, securityUserResolver,
                new ObjectMapper(), new RestTemplate());
    }

    HrSalaryIntegrationServiceImpl(HrSalaryFieldMappingMapper fieldMappingMapper,
            HrSalarySyncLogMapper syncLogMapper,
            HrSalaryRetryTaskMapper retryTaskMapper,
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
    public List<HrSalaryFieldMapping> listMappings() {
        return fieldMappingMapper.selectList(new LambdaQueryWrapper<HrSalaryFieldMapping>()
                .eq(StringUtils.hasText(currentTenantId()), HrSalaryFieldMapping::getTenantId, currentTenantId())
                .orderByAsc(HrSalaryFieldMapping::getDirection)
                .orderByAsc(HrSalaryFieldMapping::getSortNo)
                .orderByAsc(HrSalaryFieldMapping::getMappingId));
    }

    /**
     * 保存映射配置。
     *
     * @param mappings 映射配置
     * @return 最新配置
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public List<HrSalaryFieldMapping> saveMappings(List<HrSalaryFieldMapping> mappings) {
        fieldMappingMapper.delete(new LambdaQueryWrapper<HrSalaryFieldMapping>()
                .eq(StringUtils.hasText(currentTenantId()), HrSalaryFieldMapping::getTenantId, currentTenantId()));
        Date now = new Date();
        if (mappings != null) {
            int sortNo = 1;
            for (HrSalaryFieldMapping mapping : mappings) {
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
     * 发起薪酬推送。
     *
     * @param body 推送参数
     * @return 生成的同步日志
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public List<HrSalarySyncLog> pushSalary(HrSalaryPushBody body) {
        List<HrEmployeeCore> employees = loadTargetEmployees(body);
        List<HrSalarySyncLog> logs = new ArrayList<>();
        for (HrEmployeeCore employee : employees) {
            HrSalarySyncLog log = createPendingLog(employee, body);
            executePush(log, employee, body);
            logs.add(syncLogMapper.selectById(log.getLogId()));
        }
        return logs;
    }

    /**
     * 处理薪酬回传。
     *
     * @param body 回传参数
     * @return 最新日志
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public HrSalarySyncLog callback(HrSalaryCallbackBody body) {
        HrSalarySyncLog log = loadLog(body);
        HrSalarySyncLog updateEntity = new HrSalarySyncLog();
        updateEntity.setLogId(log.getLogId());
        updateEntity.setSyncStatus(HrEmployeeSupport.defaultIfBlank(
                HrEmployeeSupport.normalizeStatus(body == null ? null : body.getSyncStatus()),
                HrEmployeeSupport.SALARY_SYNC_STATUS_SUCCESS));
        updateEntity.setResponseJson(HrEmployeeSupport.trimToNull(body == null ? null : body.getPayloadJson()));
        updateEntity.setLastError(HrEmployeeSupport.trimToNull(body == null ? null : body.getResultMessage()));
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
    public Page<HrSalarySyncLog> selectLogPage(String periodCode, String syncStatus, Long employeeId, Long pageNum, Long pageSize) {
        Page<HrSalarySyncLog> page = new Page<>(
                HrEmployeeSupport.normalizePageNum(pageNum),
                HrEmployeeSupport.normalizePageSize(pageSize));
        return syncLogMapper.selectPage(page, new LambdaQueryWrapper<HrSalarySyncLog>()
                .eq(StringUtils.hasText(currentTenantId()), HrSalarySyncLog::getTenantId, currentTenantId())
                .eq(StringUtils.hasText(periodCode), HrSalarySyncLog::getPeriodCode, HrEmployeeSupport.trimToNull(periodCode))
                .eq(StringUtils.hasText(syncStatus), HrSalarySyncLog::getSyncStatus, HrEmployeeSupport.normalizeStatus(syncStatus))
                .eq(employeeId != null, HrSalarySyncLog::getEmployeeId, employeeId)
                .orderByDesc(HrSalarySyncLog::getUpdateTime)
                .orderByDesc(HrSalarySyncLog::getCreateTime));
    }

    /**
     * 重试指定日志。
     *
     * @param logId 日志ID
     * @return 最新日志
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public HrSalarySyncLog retry(Long logId) {
        HrSalarySyncLog log = syncLogMapper.selectOne(new LambdaQueryWrapper<HrSalarySyncLog>()
                .eq(HrSalarySyncLog::getLogId, logId)
                .eq(StringUtils.hasText(currentTenantId()), HrSalarySyncLog::getTenantId, currentTenantId()));
        if (log == null) {
            throw new ServiceException("薪酬同步日志不存在", (int) ResultCode.NOT_FOUND.getCode());
        }
        HrSalaryRetryTask retryTask = new HrSalaryRetryTask();
        retryTask.setTenantId(log.getTenantId());
        retryTask.setLogId(logId);
        retryTask.setTaskStatus(HrEmployeeSupport.SALARY_SYNC_STATUS_RETRYING);
        retryTask.setRetryCount(log.getRetryCount() == null ? 1 : log.getRetryCount() + 1);
        retryTask.setNextRetryTime(new Date());
        retryTaskMapper.insert(retryTask);
        HrSalarySyncLog updateEntity = new HrSalarySyncLog();
        updateEntity.setLogId(logId);
        updateEntity.setSyncStatus(HrEmployeeSupport.SALARY_SYNC_STATUS_RETRYING);
        updateEntity.setRetryCount(retryTask.getRetryCount());
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
    private List<HrEmployeeCore> loadTargetEmployees(HrSalaryPushBody body) {
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
    private HrSalarySyncLog createPendingLog(HrEmployeeCore employee, HrSalaryPushBody body) {
        Date now = new Date();
        HrSalarySyncLog log = new HrSalarySyncLog();
        log.setTenantId(employee.getTenantId());
        log.setEmployeeId(employee.getEmployeeId());
        log.setDirection("ERP_TO_SALARY");
        log.setPeriodCode(body == null ? null : HrEmployeeSupport.trimToNull(body.getPeriodCode()));
        log.setSyncStatus(HrEmployeeSupport.SALARY_SYNC_STATUS_PENDING);
        log.setRequestNo("SALARY-" + employee.getEmployeeId() + "-" + System.currentTimeMillis());
        log.setPayloadJson(writePayload(buildPayload(employee, body)));
        log.setRetryCount(0);
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
    private void executePush(HrSalarySyncLog log, HrEmployeeCore employee, HrSalaryPushBody body) {
        if (log == null) {
            return;
        }
        if (!StringUtils.hasText(pushUrl)) {
            markLogFailed(log.getLogId(), "未配置薪酬推送地址");
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
            HrSalarySyncLog updateEntity = new HrSalarySyncLog();
            updateEntity.setLogId(log.getLogId());
            updateEntity.setSyncStatus(HrEmployeeSupport.SALARY_SYNC_STATUS_SUCCESS);
            updateEntity.setResponseJson(HrEmployeeSupport.trimToNull(response));
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
    private Map<String, Object> buildPayload(HrEmployeeCore employee, HrSalaryPushBody body) {
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
        HrSalarySyncLog updateEntity = new HrSalarySyncLog();
        updateEntity.setLogId(logId);
        updateEntity.setSyncStatus(HrEmployeeSupport.SALARY_SYNC_STATUS_FAILED);
        updateEntity.setLastError(HrEmployeeSupport.trimToNull(message));
        syncLogMapper.updateById(updateEntity);
    }

    /**
     * 加载目标日志。
     *
     * @param body 回传参数
     * @return 同步日志
     */
    private HrSalarySyncLog loadLog(HrSalaryCallbackBody body) {
        LambdaQueryWrapper<HrSalarySyncLog> queryWrapper = new LambdaQueryWrapper<HrSalarySyncLog>()
                .eq(StringUtils.hasText(currentTenantId()), HrSalarySyncLog::getTenantId, currentTenantId());
        if (body != null && body.getLogId() != null) {
            queryWrapper.eq(HrSalarySyncLog::getLogId, body.getLogId());
        } else if (body != null && StringUtils.hasText(body.getExternalBizNo())) {
            queryWrapper.eq(HrSalarySyncLog::getRequestNo, body.getExternalBizNo().trim());
        } else {
            throw new IllegalArgumentException("薪酬回传必须提供日志ID或外部业务号");
        }
        HrSalarySyncLog log = syncLogMapper.selectOne(queryWrapper.last("limit 1"));
        if (log == null) {
            throw new ServiceException("薪酬同步日志不存在", (int) ResultCode.NOT_FOUND.getCode());
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
            throw new IllegalStateException("薪酬同步载荷序列化失败", ex);
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

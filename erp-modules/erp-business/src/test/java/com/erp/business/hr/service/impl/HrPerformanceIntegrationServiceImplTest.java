package com.erp.business.hr.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.erp.business.hr.domain.HrPerformanceFieldMapping;
import com.erp.business.hr.domain.HrPerformanceRetryTask;
import com.erp.business.hr.domain.HrPerformanceSyncLog;
import com.erp.business.hr.domain.HrEmployeeCore;
import com.erp.business.hr.domain.vo.HrPerformanceCallbackBody;
import com.erp.business.hr.domain.vo.HrPerformancePushBody;
import com.erp.business.hr.mapper.HrPerformanceFieldMappingMapper;
import com.erp.business.hr.mapper.HrPerformanceRetryTaskMapper;
import com.erp.business.hr.mapper.HrPerformanceSyncLogMapper;
import com.erp.business.hr.mapper.HrEmployeeCoreMapper;
import com.erp.business.security.service.SecurityUserResolver;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpEntity;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestTemplate;

import java.util.Collections;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 绩效管理同步服务单元测试。
 */
@ExtendWith(MockitoExtension.class)
class HrPerformanceIntegrationServiceImplTest {

    @Mock
    private HrPerformanceFieldMappingMapper fieldMappingMapper;

    @Mock
    private HrPerformanceSyncLogMapper syncLogMapper;

    @Mock
    private HrPerformanceRetryTaskMapper retryTaskMapper;

    @Mock
    private HrEmployeeCoreMapper employeeCoreMapper;

    @Mock
    private SecurityUserResolver securityUserResolver;

    @Mock
    private RestTemplate restTemplate;

    private HrPerformanceIntegrationServiceImpl performanceService;

    /**
     * 初始化被测对象。
     */
    @BeforeEach
    void setUp() {
        performanceService = new HrPerformanceIntegrationServiceImpl(fieldMappingMapper, syncLogMapper,
                retryTaskMapper, employeeCoreMapper, securityUserResolver, new ObjectMapper(), restTemplate);
        when(securityUserResolver.getCurrentTenantId()).thenReturn("000000");
    }

    /**
     * 验证保存映射时会补齐租户、状态与排序。
     */
    @Test
    void shouldSavePerformanceMappings() {
        HrPerformanceFieldMapping mapping = new HrPerformanceFieldMapping();
        mapping.setDirection("ERP_TO_PERFORMANCE");
        mapping.setFieldCode("workDays");
        mapping.setTargetField("days");
        when(fieldMappingMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(Collections.singletonList(mapping));

        List<HrPerformanceFieldMapping> result = performanceService.saveMappings(Collections.singletonList(mapping));

        Assertions.assertEquals(1, result.size());
        ArgumentCaptor<HrPerformanceFieldMapping> captor = ArgumentCaptor.forClass(HrPerformanceFieldMapping.class);
        verify(fieldMappingMapper).insert(captor.capture());
        Assertions.assertEquals("000000", captor.getValue().getTenantId());
        Assertions.assertEquals("ACTIVE", captor.getValue().getStatus());
        Assertions.assertEquals(1, captor.getValue().getSortNo());
    }

    /**
     * 验证配置推送地址后会写入成功日志。
     */
    @Test
    void shouldPushPerformanceSuccessfully() {
        ReflectionTestUtils.setField(performanceService, "pushUrl", "https://example.com/performance");
        HrPerformancePushBody body = new HrPerformancePushBody();
        body.setPeriodCode("202603");
        body.setEmployeeIds(Collections.singletonList(10L));
        when(employeeCoreMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(Collections.singletonList(buildEmployee()));
        when(fieldMappingMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(Collections.emptyList());
        doAnswer(invocation -> {
            HrPerformanceSyncLog log = invocation.getArgument(0);
            log.setLogId(101L);
            return 1;
        }).when(syncLogMapper).insert(any(HrPerformanceSyncLog.class));
        when(restTemplate.postForObject(anyString(), any(HttpEntity.class), eq(String.class))).thenReturn("{\"status\":\"OK\"}");
        when(syncLogMapper.selectById(101L)).thenReturn(buildLog(101L, "SUCCESS", null));

        List<HrPerformanceSyncLog> result = performanceService.pushPerformance(body);

        Assertions.assertEquals(1, result.size());
        ArgumentCaptor<HrPerformanceSyncLog> updateCaptor = ArgumentCaptor.forClass(HrPerformanceSyncLog.class);
        verify(syncLogMapper).updateById(updateCaptor.capture());
        Assertions.assertEquals("SUCCESS", updateCaptor.getValue().getSyncStatus());
    }

    /**
     * 验证未配置推送地址时会写入失败日志。
     */
    @Test
    void shouldMarkPerformanceLogFailedWhenPushUrlMissing() {
        HrPerformancePushBody body = new HrPerformancePushBody();
        body.setEmployeeIds(Collections.singletonList(10L));
        when(employeeCoreMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(Collections.singletonList(buildEmployee()));
        when(fieldMappingMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(Collections.emptyList());
        doAnswer(invocation -> {
            HrPerformanceSyncLog log = invocation.getArgument(0);
            log.setLogId(102L);
            return 1;
        }).when(syncLogMapper).insert(any(HrPerformanceSyncLog.class));
        when(syncLogMapper.selectById(102L)).thenReturn(buildLog(102L, "FAILED", "未配置绩效推送地址"));

        List<HrPerformanceSyncLog> result = performanceService.pushPerformance(body);

        Assertions.assertEquals("FAILED", result.get(0).getSyncStatus());
        ArgumentCaptor<HrPerformanceSyncLog> updateCaptor = ArgumentCaptor.forClass(HrPerformanceSyncLog.class);
        verify(syncLogMapper).updateById(updateCaptor.capture());
        Assertions.assertEquals("FAILED", updateCaptor.getValue().getSyncStatus());
        Assertions.assertTrue(updateCaptor.getValue().getLastError().contains("未配置绩效推送地址"));
    }

    /**
     * 验证回传时会按日志号更新同步状态。
     */
    @Test
    void shouldHandlePerformanceCallback() {
        HrPerformanceCallbackBody body = new HrPerformanceCallbackBody();
        body.setLogId(201L);
        body.setSyncStatus("SUCCESS");
        body.setPayloadJson("{\"done\":true}");
        body.setResultMessage("ok");
        when(syncLogMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(buildLog(201L, "PENDING", null));
        when(syncLogMapper.selectById(201L)).thenReturn(buildLog(201L, "SUCCESS", null));

        HrPerformanceSyncLog result = performanceService.callback(body);

        Assertions.assertEquals("SUCCESS", result.getSyncStatus());
        ArgumentCaptor<HrPerformanceSyncLog> captor = ArgumentCaptor.forClass(HrPerformanceSyncLog.class);
        verify(syncLogMapper).updateById(captor.capture());
        Assertions.assertEquals("SUCCESS", captor.getValue().getSyncStatus());
    }

    /**
     * 验证重试时会写入重试任务并重新推送。
     */
    @Test
    void shouldRetryPerformanceLog() {
        ReflectionTestUtils.setField(performanceService, "pushUrl", "https://example.com/performance");
        HrPerformanceSyncLog existingLog = buildLog(301L, "FAILED", "timeout");
        existingLog.setTenantId("000000");
        existingLog.setPayloadJson("{}");
        existingLog.setRetryCount(1);
        when(syncLogMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(existingLog);
        when(restTemplate.postForObject(anyString(), any(HttpEntity.class), eq(String.class))).thenReturn("{\"status\":\"OK\"}");
        when(syncLogMapper.selectById(301L))
                .thenReturn(existingLog)
                .thenReturn(buildLog(301L, "SUCCESS", null));

        HrPerformanceSyncLog result = performanceService.retry(301L);

        Assertions.assertEquals("SUCCESS", result.getSyncStatus());
        ArgumentCaptor<HrPerformanceRetryTask> retryCaptor = ArgumentCaptor.forClass(HrPerformanceRetryTask.class);
        verify(retryTaskMapper).insert(retryCaptor.capture());
        Assertions.assertEquals(2, retryCaptor.getValue().getRetryCount());
    }

    /**
     * 构造员工核心主档。
     *
     * @return 员工核心主档
     */
    private HrEmployeeCore buildEmployee() {
        HrEmployeeCore employee = new HrEmployeeCore();
        employee.setEmployeeId(10L);
        employee.setTenantId("000000");
        employee.setEmpCode("E0001");
        employee.setEmpName("张三");
        employee.setOrgId(1L);
        employee.setDeptId(2L);
        employee.setPosition("专员");
        employee.setDelFlag("0");
        employee.setStatus("ACTIVE");
        return employee;
    }

    /**
     * 构造同步日志。
     *
     * @param logId 日志ID
     * @param status 状态
     * @param lastError 最后错误
     * @return 同步日志
     */
    private HrPerformanceSyncLog buildLog(Long logId, String status, String lastError) {
        HrPerformanceSyncLog log = new HrPerformanceSyncLog();
        log.setLogId(logId);
        log.setTenantId("000000");
        log.setEmployeeId(10L);
        log.setSyncStatus(status);
        log.setRequestNo("PERFORMANCE-10-1");
        log.setLastError(lastError);
        return log;
    }
}

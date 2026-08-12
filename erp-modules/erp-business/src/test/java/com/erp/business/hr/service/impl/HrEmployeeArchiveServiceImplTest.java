package com.erp.business.hr.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.erp.business.hr.domain.HrEmployeeArchive;
import com.erp.business.hr.domain.HrEmployeeCore;
import com.erp.business.hr.domain.vo.HrEmployeeArchiveBody;
import com.erp.business.hr.mapper.HrEmployeeArchiveMapper;
import com.erp.business.hr.mapper.HrEmployeeCoreMapper;
import com.erp.business.security.service.SecurityUserResolver;
import com.erp.common.client.internal.InternalSystemClient;
import com.erp.common.core.exception.ServiceException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 员工扩展档案服务单元测试。
 */
@ExtendWith(MockitoExtension.class)
class HrEmployeeArchiveServiceImplTest {

    @Mock
    private HrEmployeeArchiveMapper archiveMapper;

    @Mock
    private HrEmployeeCoreMapper employeeCoreMapper;

    @Mock
    private InternalSystemClient internalSystemClient;

    @Mock
    private SecurityUserResolver securityUserResolver;

    private HrEmployeeArchiveServiceImpl archiveService;

    /**
     * 初始化被测对象。
     */
    @BeforeEach
    void setUp() {
        archiveService = new HrEmployeeArchiveServiceImpl(archiveMapper, employeeCoreMapper, internalSystemClient,
                securityUserResolver);
        lenient().when(securityUserResolver.getCurrentUsername()).thenReturn("tester");
    }

    /**
     * 验证新增档案时会落库并补齐操作人。
     */
    @Test
    void shouldCreateArchiveWhenEmployeeExists() {
        HrEmployeeArchiveBody archiveBody = buildArchiveBody();
        archiveBody.setEmployeeId(10L);
        when(employeeCoreMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(buildEmployeeCore(10L));
        when(archiveMapper.selectById(10L)).thenReturn(null, buildArchive(10L));
        when(internalSystemClient.getConfigValue("hr.employee.cert_unique_enabled")).thenReturn("true");
        when(archiveMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(0L);
        when(archiveMapper.insert(any(HrEmployeeArchive.class))).thenReturn(1);

        HrEmployeeArchive archive = archiveService.createArchive(archiveBody);

        Assertions.assertNotNull(archive);
        ArgumentCaptor<HrEmployeeArchive> captor = ArgumentCaptor.forClass(HrEmployeeArchive.class);
        verify(archiveMapper).insert(captor.capture());
        // create_by 等留痕字段已改由 AuditMetaObjectHandlerSupport 自动填充，不再由本服务赋值
        Assertions.assertEquals("ID_CARD", captor.getValue().getCertType());
    }

    /**
     * 验证关闭唯一校验时允许重复证件号。
     */
    @Test
    void shouldSkipCertUniqueCheckWhenConfigDisabled() {
        HrEmployeeArchiveBody archiveBody = buildArchiveBody();
        archiveBody.setEmployeeId(11L);
        when(employeeCoreMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(buildEmployeeCore(11L));
        when(archiveMapper.selectById(11L)).thenReturn(null, buildArchive(11L));
        when(internalSystemClient.getConfigValue("hr.employee.cert_unique_enabled")).thenReturn("false");
        when(archiveMapper.insert(any(HrEmployeeArchive.class))).thenReturn(1);

        archiveService.createArchive(archiveBody);

        verify(archiveMapper, never()).selectCount(any(LambdaQueryWrapper.class));
    }

    /**
     * 验证证件号重复时会抛出冲突异常。
     */
    @Test
    void shouldRejectDuplicateCertNoWhenConfigEnabled() {
        HrEmployeeArchiveBody archiveBody = buildArchiveBody();
        archiveBody.setEmployeeId(12L);
        when(employeeCoreMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(buildEmployeeCore(12L));
        when(archiveMapper.selectById(12L)).thenReturn(null);
        when(internalSystemClient.getConfigValue("hr.employee.cert_unique_enabled")).thenReturn("true");
        when(archiveMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(1L);

        IllegalStateException exception = Assertions.assertThrows(IllegalStateException.class,
                () -> archiveService.createArchive(archiveBody));

        Assertions.assertTrue(exception.getMessage().contains("证件号已存在"));
    }

    /**
     * 验证更新缺失档案时会自动补建。
     */
    @Test
    void shouldCreateArchiveWhenUpdatingMissingArchive() {
        HrEmployeeArchiveBody archiveBody = buildArchiveBody();
        when(employeeCoreMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(buildEmployeeCore(13L));
        when(archiveMapper.selectById(13L)).thenReturn(null, null, buildArchive(13L));
        when(internalSystemClient.getConfigValue("hr.employee.cert_unique_enabled")).thenReturn("false");
        when(archiveMapper.insert(any(HrEmployeeArchive.class))).thenReturn(1);

        HrEmployeeArchive archive = archiveService.updateArchive(13L, archiveBody);

        Assertions.assertNotNull(archive);
        verify(archiveMapper).insert(any(HrEmployeeArchive.class));
    }

    /**
     * 验证更新已有档案时只执行更新。
     */
    @Test
    void shouldUpdateArchiveWhenArchiveExists() {
        HrEmployeeArchiveBody archiveBody = buildArchiveBody();
        archiveBody.setEmploymentType("FULL_TIME");
        when(employeeCoreMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(buildEmployeeCore(14L));
        when(archiveMapper.selectById(14L)).thenReturn(buildArchive(14L), buildArchive(14L));
        when(internalSystemClient.getConfigValue("hr.employee.cert_unique_enabled")).thenReturn("true");
        when(archiveMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(0L);
        when(archiveMapper.updateById(any(HrEmployeeArchive.class))).thenReturn(1);

        HrEmployeeArchive archive = archiveService.updateArchive(14L, archiveBody);

        Assertions.assertNotNull(archive);
        verify(archiveMapper).updateById(any(HrEmployeeArchive.class));
    }

    /**
     * 验证查询员工档案支持空值与批量空集合。
     */
    @Test
    void shouldReturnNullOrEmptyWhenArchiveQueryInputIsEmpty() {
        Assertions.assertNull(archiveService.getArchiveByEmployeeId(null));
        Assertions.assertEquals(Collections.emptyList(), archiveService.listArchivesByEmployeeIds(Collections.emptyList()));
    }

    /**
     * 验证员工不存在时返回未找到异常。
     */
    @Test
    void shouldThrowNotFoundWhenEmployeeDoesNotExist() {
        when(employeeCoreMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(null);

        Assertions.assertThrows(ServiceException.class,
                () -> archiveService.updateArchive(15L, buildArchiveBody()));
    }

    /**
     * 构造档案请求参数。
     *
     * @return 档案请求参数
     */
    private HrEmployeeArchiveBody buildArchiveBody() {
        HrEmployeeArchiveBody archiveBody = new HrEmployeeArchiveBody();
        archiveBody.setCertType("ID_CARD");
        archiveBody.setCertNo("310101199001018888");
        archiveBody.setEmploymentType("FULL_TIME");
        archiveBody.setEmergencyContact("李雷");
        return archiveBody;
    }

    /**
     * 构造员工核心主档。
     *
     * @param employeeId 员工ID
     * @return 员工核心主档
     */
    private HrEmployeeCore buildEmployeeCore(Long employeeId) {
        HrEmployeeCore employeeCore = new HrEmployeeCore();
        employeeCore.setEmployeeId(employeeId);
        employeeCore.setTenantId("000000");
        employeeCore.setDelFlag("0");
        return employeeCore;
    }

    /**
     * 构造员工扩展档案。
     *
     * @param employeeId 员工ID
     * @return 员工扩展档案
     */
    private HrEmployeeArchive buildArchive(Long employeeId) {
        HrEmployeeArchive archive = new HrEmployeeArchive();
        archive.setEmployeeId(employeeId);
        archive.setTenantId("000000");
        archive.setCertType("ID_CARD");
        archive.setCertNo("310101199001018888");
        return archive;
    }
}

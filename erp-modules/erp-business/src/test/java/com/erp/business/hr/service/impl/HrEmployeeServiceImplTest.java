package com.erp.business.hr.service.impl;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.erp.business.hr.domain.HrEmployeeArchive;
import com.erp.business.hr.domain.HrEmployeeCore;
import com.erp.business.hr.domain.vo.HrEmployeeAggregateQuery;
import com.erp.business.hr.domain.vo.HrEmployeeDetailVO;
import com.erp.business.hr.domain.vo.HrEmployeeListVO;
import com.erp.business.hr.mapper.HrEmployeeCoreMapper;
import com.erp.business.hr.service.IHrEmployeeArchiveService;
import com.erp.business.hr.service.IHrEmployeeChangeService;
import com.erp.business.hr.service.IHrEmployeePositionService;
import com.erp.common.core.exception.ServiceException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * HR 员工聚合服务单元测试。
 */
@ExtendWith(MockitoExtension.class)
class HrEmployeeServiceImplTest {

    @Mock
    private HrEmployeeCoreMapper employeeCoreMapper;

    @Mock
    private IHrEmployeeArchiveService employeeArchiveService;

    @Mock
    private IHrEmployeePositionService employeePositionService;

    @Mock
    private IHrEmployeeChangeService employeeChangeService;

    private HrEmployeeServiceImpl hrEmployeeService;

    /**
     * 初始化被测对象。
     */
    @BeforeEach
    void setUp() {
        hrEmployeeService = new HrEmployeeServiceImpl(employeeCoreMapper, employeeArchiveService,
                employeePositionService, employeeChangeService);
    }

    /**
     * 验证分页查询会聚合扩展档案并返回脱敏列表。
     */
    @Test
    void shouldAssembleEmployeePageWithArchiveInfo() {
        HrEmployeeCore core = buildEmployeeCore();
        Page<HrEmployeeCore> corePage = new Page<>(1, 20, 1);
        corePage.setRecords(Collections.singletonList(core));
        when(employeeCoreMapper.selectPage(any(Page.class), any())).thenReturn(corePage);
        when(employeeArchiveService.listArchivesByEmployeeIds(Collections.singletonList(10L)))
                .thenReturn(Collections.singletonList(buildArchive()));

        Page<HrEmployeeListVO> resultPage = hrEmployeeService.selectEmployeePage(new HrEmployeeAggregateQuery());

        Assertions.assertEquals(1, resultPage.getTotal());
        Assertions.assertEquals(1, resultPage.getRecords().size());
        HrEmployeeListVO listVO = resultPage.getRecords().get(0);
        Assertions.assertEquals("E0001", listVO.getEmpCode());
        Assertions.assertEquals(3, listVO.getVersionNo());
        Assertions.assertEquals("****8888", listVO.getCertNoMasked());
        Assertions.assertTrue(listVO.getMobile().contains("****"));
    }

    /**
     * 验证空分页记录时不会继续查询扩展档案。
     */
    @Test
    void shouldReturnEmptyPageWhenCorePageIsEmpty() {
        Page<HrEmployeeCore> corePage = new Page<>(1, 20, 0);
        corePage.setRecords(Collections.emptyList());
        when(employeeCoreMapper.selectPage(any(Page.class), any())).thenReturn(corePage);

        Page<HrEmployeeListVO> resultPage = hrEmployeeService.selectEmployeePage(new HrEmployeeAggregateQuery());

        Assertions.assertEquals(0, resultPage.getRecords().size());
    }

    /**
     * 验证查询详情时会返回核心与扩展档案。
     */
    @Test
    void shouldReturnEmployeeDetail() {
        HrEmployeeCore core = buildEmployeeCore();
        HrEmployeeArchive archive = buildArchive();
        when(employeeCoreMapper.selectOne(any())).thenReturn(core);
        when(employeeArchiveService.getArchiveByEmployeeId(10L)).thenReturn(archive);
        when(employeePositionService.listByEmployeeId(10L)).thenReturn(Collections.emptyList());
        when(employeeChangeService.listByEmployeeId(10L)).thenReturn(Collections.emptyList());

        HrEmployeeDetailVO detailVO = hrEmployeeService.getEmployeeDetail(10L);

        Assertions.assertNotNull(detailVO.getCore());
        Assertions.assertNotNull(detailVO.getArchive());
        Assertions.assertNotNull(detailVO.getPositions());
        Assertions.assertNotNull(detailVO.getChanges());
        Assertions.assertEquals("张三", detailVO.getCore().getEmpName());
    }

    /**
     * 验证员工不存在时抛出未找到异常。
     */
    @Test
    void shouldThrowNotFoundWhenEmployeeDetailIsMissing() {
        when(employeeCoreMapper.selectOne(any())).thenReturn(null);

        Assertions.assertThrows(ServiceException.class, () -> hrEmployeeService.getEmployeeDetail(10L));
    }

    /**
     * 验证空员工ID会触发参数异常。
     */
    @Test
    void shouldRejectEmptyEmployeeIdWhenQueryingDetail() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> hrEmployeeService.getEmployeeDetail(null));
    }

    /**
     * 构造员工核心主档。
     *
     * @return 员工核心主档
     */
    private HrEmployeeCore buildEmployeeCore() {
        HrEmployeeCore core = new HrEmployeeCore();
        core.setEmployeeId(10L);
        core.setEmpCode("E0001");
        core.setEmpName("张三");
        core.setMobile("13800138000");
        core.setEmail("zhangsan@example.com");
        core.setOrgId(1L);
        core.setDeptId(2L);
        core.setPosition("研发工程师");
        core.setStatus("ACTIVE");
        core.setVersionNo(3);
        core.setDelFlag("0");
        return core;
    }

    /**
     * 构造员工扩展档案。
     *
     * @return 员工扩展档案
     */
    private HrEmployeeArchive buildArchive() {
        HrEmployeeArchive archive = new HrEmployeeArchive();
        archive.setEmployeeId(10L);
        archive.setCertNo("310101199001018888");
        archive.setEmploymentType("FULL_TIME");
        return archive;
    }
}

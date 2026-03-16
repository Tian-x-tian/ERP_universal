package com.erp.business.hr.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.erp.business.hr.domain.HrEmployeeArchive;
import com.erp.business.hr.domain.HrEmployeeCore;
import com.erp.business.hr.domain.vo.HrEmployeeAggregateQuery;
import com.erp.business.hr.domain.vo.HrEmployeeDetailVO;
import com.erp.business.hr.domain.vo.HrEmployeeListVO;
import com.erp.business.hr.mapper.HrEmployeeCoreMapper;
import com.erp.business.hr.service.IHrEmployeeArchiveService;
import com.erp.business.hr.service.IHrEmployeeChangeService;
import com.erp.business.hr.service.IHrEmployeeService;
import com.erp.business.hr.service.IHrEmployeePositionService;
import com.erp.business.hr.support.HrEmployeeSupport;
import com.erp.common.core.domain.ResultCode;
import com.erp.common.core.exception.ServiceException;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * HR 员工聚合服务实现。
 */
@Service
public class HrEmployeeServiceImpl implements IHrEmployeeService {
    private final HrEmployeeCoreMapper employeeCoreMapper;
    private final IHrEmployeeArchiveService employeeArchiveService;
    private final IHrEmployeePositionService employeePositionService;
    private final IHrEmployeeChangeService employeeChangeService;

    public HrEmployeeServiceImpl(HrEmployeeCoreMapper employeeCoreMapper,
            IHrEmployeeArchiveService employeeArchiveService,
            IHrEmployeePositionService employeePositionService,
            IHrEmployeeChangeService employeeChangeService) {
        this.employeeCoreMapper = employeeCoreMapper;
        this.employeeArchiveService = employeeArchiveService;
        this.employeePositionService = employeePositionService;
        this.employeeChangeService = employeeChangeService;
    }

    /**
     * 分页查询 HR 员工台账。
     *
     * @param query 查询参数
     * @return 聚合分页结果
     */
    @Override
    public Page<HrEmployeeListVO> selectEmployeePage(HrEmployeeAggregateQuery query) {
        HrEmployeeAggregateQuery safeQuery = query == null ? new HrEmployeeAggregateQuery() : query;
        Page<HrEmployeeCore> page = new Page<>(
                HrEmployeeSupport.normalizePageNum(safeQuery.getPageNum()),
                HrEmployeeSupport.normalizePageSize(safeQuery.getPageSize()));
        LambdaQueryWrapper<HrEmployeeCore> queryWrapper = new LambdaQueryWrapper<HrEmployeeCore>()
                .eq(HrEmployeeCore::getDelFlag, HrEmployeeSupport.EXIST_DEL_FLAG)
                .like(StringUtils.hasText(safeQuery.getEmpCode()), HrEmployeeCore::getEmpCode,
                        HrEmployeeSupport.trimToNull(safeQuery.getEmpCode()))
                .like(StringUtils.hasText(safeQuery.getEmpName()), HrEmployeeCore::getEmpName,
                        HrEmployeeSupport.trimToNull(safeQuery.getEmpName()))
                .eq(safeQuery.getOrgId() != null, HrEmployeeCore::getOrgId, safeQuery.getOrgId())
                .eq(safeQuery.getDeptId() != null, HrEmployeeCore::getDeptId, safeQuery.getDeptId())
                .like(StringUtils.hasText(safeQuery.getPosition()), HrEmployeeCore::getPosition,
                        HrEmployeeSupport.trimToNull(safeQuery.getPosition()))
                .eq(StringUtils.hasText(safeQuery.getStatus()), HrEmployeeCore::getStatus,
                        HrEmployeeSupport.trimToNull(safeQuery.getStatus()))
                .orderByDesc(HrEmployeeCore::getUpdateTime)
                .orderByDesc(HrEmployeeCore::getCreateTime);
        Page<HrEmployeeCore> corePage = employeeCoreMapper.selectPage(page, queryWrapper);
        List<HrEmployeeCore> coreRecords = corePage.getRecords();
        Map<Long, HrEmployeeArchive> archiveMap = loadArchiveMap(coreRecords);
        Page<HrEmployeeListVO> resultPage = new Page<>(corePage.getCurrent(), corePage.getSize(), corePage.getTotal());
        resultPage.setRecords(coreRecords.stream()
                .map(core -> buildListVO(core, archiveMap.get(core.getEmployeeId())))
                .collect(Collectors.toList()));
        return resultPage;
    }

    /**
     * 查询 HR 员工详情。
     *
     * @param employeeId 员工ID
     * @return 聚合详情
     */
    @Override
    public HrEmployeeDetailVO getEmployeeDetail(Long employeeId) {
        if (employeeId == null) {
            throw new IllegalArgumentException("员工ID不能为空");
        }
        HrEmployeeCore core = employeeCoreMapper.selectOne(new LambdaQueryWrapper<HrEmployeeCore>()
                .eq(HrEmployeeCore::getEmployeeId, employeeId)
                .eq(HrEmployeeCore::getDelFlag, HrEmployeeSupport.EXIST_DEL_FLAG));
        if (core == null) {
            throw new ServiceException("员工不存在", (int) ResultCode.NOT_FOUND.getCode());
        }
        HrEmployeeDetailVO detailVO = new HrEmployeeDetailVO();
        detailVO.setCore(core);
        detailVO.setArchive(employeeArchiveService.getArchiveByEmployeeId(employeeId));
        detailVO.setPositions(employeePositionService.listByEmployeeId(employeeId));
        detailVO.setChanges(employeeChangeService.listByEmployeeId(employeeId));
        return detailVO;
    }

    /**
     * 批量加载扩展档案索引。
     *
     * @param coreRecords 员工主档集合
     * @return 员工ID到档案对象的映射
     */
    private Map<Long, HrEmployeeArchive> loadArchiveMap(List<HrEmployeeCore> coreRecords) {
        if (coreRecords == null || coreRecords.isEmpty()) {
            return Collections.emptyMap();
        }
        List<Long> employeeIds = coreRecords.stream()
                .map(HrEmployeeCore::getEmployeeId)
                .filter(id -> id != null)
                .collect(Collectors.toList());
        if (employeeIds.isEmpty()) {
            return Collections.emptyMap();
        }
        List<HrEmployeeArchive> archives = employeeArchiveService.listArchivesByEmployeeIds(employeeIds);
        Map<Long, HrEmployeeArchive> archiveMap = new HashMap<>();
        for (HrEmployeeArchive archive : archives) {
            if (archive == null || archive.getEmployeeId() == null) {
                continue;
            }
            archiveMap.put(archive.getEmployeeId(), archive);
        }
        return archiveMap;
    }

    /**
     * 组装列表视图对象。
     *
     * @param core    核心主档
     * @param archive 扩展档案
     * @return 列表视图
     */
    private HrEmployeeListVO buildListVO(HrEmployeeCore core, HrEmployeeArchive archive) {
        HrEmployeeListVO listVO = new HrEmployeeListVO();
        listVO.setEmployeeId(core.getEmployeeId());
        listVO.setVersionNo(core.getVersionNo());
        listVO.setEmpCode(core.getEmpCode());
        listVO.setEmpName(core.getEmpName());
        listVO.setMobile(HrEmployeeSupport.maskMobile(core.getMobile()));
        listVO.setEmail(HrEmployeeSupport.maskEmail(core.getEmail()));
        listVO.setOrgId(core.getOrgId());
        listVO.setDeptId(core.getDeptId());
        listVO.setPosition(core.getPosition());
        listVO.setStatus(core.getStatus());
        if (archive != null) {
            listVO.setEmploymentType(archive.getEmploymentType());
            listVO.setHireDate(archive.getHireDate());
            listVO.setProbationEndDate(archive.getProbationEndDate());
            listVO.setCertNoMasked(HrEmployeeSupport.maskCertificateNo(archive.getCertNo()));
        }
        return listVO;
    }
}

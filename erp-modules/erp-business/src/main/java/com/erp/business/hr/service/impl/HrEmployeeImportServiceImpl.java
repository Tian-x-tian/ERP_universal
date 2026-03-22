package com.erp.business.hr.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.erp.business.hr.domain.HrEmployeeArchive;
import com.erp.business.hr.domain.HrEmployeeCore;
import com.erp.business.hr.domain.vo.HrEmployeeArchiveBody;
import com.erp.business.hr.mapper.HrEmployeeArchiveMapper;
import com.erp.business.hr.mapper.HrEmployeeCoreMapper;
import com.erp.business.hr.service.IHrEmployeeArchiveService;
import com.erp.business.hr.service.IHrEmployeeImportService;
import com.erp.business.hr.support.HrEmployeeSupport;
import com.erp.business.security.service.SecurityUserResolver;
import com.erp.business.system.domain.SysImexJob;
import com.erp.common.client.internal.InternalSystemClient;
import com.erp.platform.contract.model.PlatformImexJob;
import com.erp.platform.contract.model.PlatformImexJobCreateRequest;
import com.erp.platform.contract.model.PlatformImexJobUpdateRequest;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * 员工批量导入服务实现。
 */
@Service
public class HrEmployeeImportServiceImpl implements IHrEmployeeImportService {
    private static final String MODULE_CODE = "HR_EMPLOYEE";
    private static final String JOB_TYPE = "IMPORT";

    private final InternalSystemClient internalSystemClient;
    private final HrEmployeeCoreMapper employeeCoreMapper;
    private final HrEmployeeArchiveMapper employeeArchiveMapper;
    private final IHrEmployeeArchiveService archiveService;
    private final SecurityUserResolver securityUserResolver;

    public HrEmployeeImportServiceImpl(InternalSystemClient internalSystemClient,
            HrEmployeeCoreMapper employeeCoreMapper,
            HrEmployeeArchiveMapper employeeArchiveMapper,
            IHrEmployeeArchiveService archiveService,
            SecurityUserResolver securityUserResolver) {
        this.internalSystemClient = internalSystemClient;
        this.employeeCoreMapper = employeeCoreMapper;
        this.employeeArchiveMapper = employeeArchiveMapper;
        this.archiveService = archiveService;
        this.securityUserResolver = securityUserResolver;
    }

    /**
     * 获取导入模板。
     *
     * @return 模板资源
     */
    @Override
    public Resource loadTemplate() {
        String template = "empCode,empName,mobile,email,orgId,deptId,position,employmentType,hireDate,certType,certNo,remark\n";
        return new ByteArrayResource(template.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * 执行员工导入。
     *
     * @param file 导入文件
     * @return 导入任务
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public SysImexJob importEmployees(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("导入文件不能为空");
        }
        SysImexJob job = createJob(file.getOriginalFilename());
        List<String> summaryLines = new ArrayList<>();
        summaryLines.add("rowNo,status,message,employeeId,empCode");
        try {
            List<String> lines = new ArrayList<>(Files.readAllLines(writeTempFile(file), StandardCharsets.UTF_8));
            int successCount = 0;
            for (int index = 1; index < lines.size(); index++) {
                String line = lines.get(index);
                if (!StringUtils.hasText(line)) {
                    continue;
                }
                String[] values = line.split(",", -1);
                try {
                    HrEmployeeCore employee = createEmployee(values);
                    successCount++;
                    summaryLines.add((index + 1) + ",SUCCESS,导入成功," + employee.getEmployeeId() + "," + employee.getEmpCode());
                } catch (Exception ex) {
                    summaryLines.add((index + 1) + ",FAILED," + sanitizeCsv(ex.getMessage()) + ",,");
                }
            }
            Path summaryPath = writeSummaryFile(summaryLines);
            updateJob(job, successCount > 0 ? "SUCCESS" : "FAILED", 100, "导入执行完成", summaryPath);
            return internalSystemToLocal(internalSystemClient.getImexJob(job.getJobId()));
        } catch (Exception ex) {
            updateJob(job, "FAILED", 100, ex.getMessage(), null);
            throw new IllegalStateException("员工导入失败", ex);
        }
    }

    /**
     * 创建导入任务。
     *
     * @param fileName 原始文件名
     * @return 任务对象
     */
    private SysImexJob createJob(String fileName) {
        Date now = new Date();
        PlatformImexJobCreateRequest request = new PlatformImexJobCreateRequest();
        request.setTenantId(currentTenantId());
        request.setJobNo("IMP-" + System.currentTimeMillis());
        request.setJobType(JOB_TYPE);
        request.setModuleCode(MODULE_CODE);
        request.setFileName(fileName);
        request.setStatus("RUNNING");
        request.setProgress(0);
        request.setTriggerType("MANUAL");
        request.setMessage("导入处理中");
        request.setCreateBy(resolveOperator());
        request.setCreateTime(now);
        request.setUpdateBy(resolveOperator());
        request.setUpdateTime(now);
        return internalSystemToLocal(internalSystemClient.createImexJob(request));
    }

    /**
     * 创建员工及档案。
     *
     * @param values CSV 列
     * @return 员工主档
     */
    private HrEmployeeCore createEmployee(String[] values) {
        if (values.length < 2) {
            throw new IllegalArgumentException("导入列数不足");
        }
        String empCode = trim(values, 0);
        String empName = trim(values, 1);
        if (!StringUtils.hasText(empCode) || !StringUtils.hasText(empName)) {
            throw new IllegalArgumentException("员工编码和员工姓名不能为空");
        }
        HrEmployeeCore existed = employeeCoreMapper.selectOne(new LambdaQueryWrapper<HrEmployeeCore>()
                .eq(HrEmployeeCore::getTenantId, currentTenantId())
                .eq(HrEmployeeCore::getEmpCode, empCode)
                .eq(HrEmployeeCore::getDelFlag, HrEmployeeSupport.EXIST_DEL_FLAG)
                .last("limit 1"));
        if (existed != null) {
            throw new IllegalStateException("员工编码已存在");
        }
        Date now = new Date();
        HrEmployeeCore employee = new HrEmployeeCore();
        employee.setTenantId(currentTenantId());
        employee.setEmpCode(empCode);
        employee.setEmpName(empName);
        employee.setMobile(trim(values, 2));
        employee.setEmail(trim(values, 3));
        employee.setOrgId(parseLong(trim(values, 4)));
        employee.setDeptId(parseLong(trim(values, 5)));
        employee.setPosition(trim(values, 6));
        employee.setStatus(HrEmployeeSupport.STATUS_DRAFT);
        employee.setVersionNo(1);
        employee.setDelFlag(HrEmployeeSupport.EXIST_DEL_FLAG);
        employee.setRemark(trim(values, 11));
        employee.setCreateBy(resolveOperator());
        employee.setCreateTime(now);
        employee.setUpdateBy(resolveOperator());
        employee.setUpdateTime(now);
        employeeCoreMapper.insert(employee);
        createArchiveIfNeeded(employee, values);
        return employee;
    }

    /**
     * 创建扩展档案。
     *
     * @param employee 员工主档
     * @param values CSV 列
     */
    private void createArchiveIfNeeded(HrEmployeeCore employee, String[] values) {
        String employmentType = trim(values, 7);
        String hireDate = trim(values, 8);
        String certType = trim(values, 9);
        String certNo = trim(values, 10);
        if (!StringUtils.hasText(employmentType)
                && !StringUtils.hasText(hireDate)
                && !StringUtils.hasText(certType)
                && !StringUtils.hasText(certNo)) {
            return;
        }
        HrEmployeeArchive archive = employeeArchiveMapper.selectById(employee.getEmployeeId());
        if (archive != null) {
            return;
        }
        HrEmployeeArchiveBody archiveBody = new HrEmployeeArchiveBody();
        archiveBody.setEmployeeId(employee.getEmployeeId());
        archiveBody.setEmploymentType(employmentType);
        archiveBody.setHireDate(parseDate(hireDate));
        archiveBody.setCertType(certType);
        archiveBody.setCertNo(certNo);
        archiveService.createArchive(archiveBody);
    }

    /**
     * 更新任务状态。
     *
     * @param job 任务
     * @param status 状态
     * @param progress 进度
     * @param message 消息
     * @param summaryPath 结果文件
     */
    private void updateJob(SysImexJob job, String status, int progress, String message, Path summaryPath) {
        if (job == null || job.getJobId() == null) {
            return;
        }
        PlatformImexJobUpdateRequest request = new PlatformImexJobUpdateRequest();
        request.setStatus(status);
        request.setProgress(progress);
        request.setMessage(message);
        request.setFilePath(summaryPath == null ? job.getFilePath() : summaryPath.toString());
        request.setUpdateBy(resolveOperator());
        request.setUpdateTime(new Date());
        internalSystemClient.updateImexJob(job.getJobId(), request);
    }

    /**
     * 写入临时文件。
     *
     * @param file 上传文件
     * @return 临时文件路径
     * @throws Exception 文件异常
     */
    private Path writeTempFile(MultipartFile file) throws Exception {
        Path tempDir = Paths.get("upload", "imex");
        Files.createDirectories(tempDir);
        Path tempFile = tempDir.resolve("import_" + System.currentTimeMillis() + ".csv");
        Files.write(tempFile, file.getBytes());
        return tempFile;
    }

    /**
     * 写入结果汇总文件。
     *
     * @param lines 结果行
     * @return 汇总文件路径
     * @throws Exception 文件异常
     */
    private Path writeSummaryFile(List<String> lines) throws Exception {
        Path tempDir = Paths.get("upload", "imex");
        Files.createDirectories(tempDir);
        Path summaryPath = tempDir.resolve("summary_" + System.currentTimeMillis() + ".csv");
        Files.write(summaryPath, lines, StandardCharsets.UTF_8);
        return summaryPath;
    }

    /**
     * 安全获取列值。
     *
     * @param values 列集合
     * @param index 下标
     * @return 规范化后的值
     */
    private String trim(String[] values, int index) {
        if (values == null || index < 0 || index >= values.length) {
            return null;
        }
        return HrEmployeeSupport.trimToNull(values[index]);
    }

    /**
     * 解析长整型。
     *
     * @param value 原始值
     * @return 解析结果
     */
    private Long parseLong(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        return Long.valueOf(value.trim());
    }

    /**
     * 解析日期。
     *
     * @param value 原始值
     * @return 日期
     */
    private Date parseDate(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        return java.sql.Date.valueOf(value.trim());
    }

    /**
     * 处理 CSV 消息中的逗号。
     *
     * @param value 原始文本
     * @return 转义文本
     */
    private String sanitizeCsv(String value) {
        return value == null ? "" : value.replace(',', '，');
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

    /**
     * 将平台任务投影映射为业务任务 DTO。
     *
     * @param source 平台任务投影
     * @return 业务任务 DTO
     */
    private SysImexJob internalSystemToLocal(PlatformImexJob source) {
        if (source == null) {
            return null;
        }
        SysImexJob target = new SysImexJob();
        target.setJobId(source.getJobId());
        target.setTenantId(source.getTenantId());
        target.setJobNo(source.getJobNo());
        target.setJobType(source.getJobType());
        target.setModuleCode(source.getModuleCode());
        target.setFileName(source.getFileName());
        target.setFilePath(source.getFilePath());
        target.setStatus(source.getStatus());
        target.setProgress(source.getProgress());
        target.setTriggerType(source.getTriggerType());
        target.setMessage(source.getMessage());
        target.setCreateBy(source.getCreateBy());
        target.setCreateTime(source.getCreateTime());
        target.setUpdateBy(source.getUpdateBy());
        target.setUpdateTime(source.getUpdateTime());
        return target;
    }
}


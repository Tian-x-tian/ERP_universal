package com.erp.business.hr.controller;

import com.erp.business.hr.service.IHrEmployeeImportService;
import com.erp.business.system.domain.SysImexJob;
import com.erp.common.core.domain.R;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/**
 * 员工导入控制层。
 */
@RestController
@RequestMapping("/business/hr/employee/import")
public class HrEmployeeImportController {
    private final IHrEmployeeImportService employeeImportService;

    public HrEmployeeImportController(IHrEmployeeImportService employeeImportService) {
        this.employeeImportService = employeeImportService;
    }

    /**
     * 下载导入模板。
     *
     * @return 模板文件
     */
    @GetMapping("/template")
    @PreAuthorize("@ss.hasPermi('business:hr:employee:import')")
    public ResponseEntity<Resource> template() {
        Resource resource = employeeImportService.loadTemplate();
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType("text/csv"))
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=hr-employee-import-template.csv")
                .body(resource);
    }

    /**
     * 提交员工导入任务。
     *
     * @param file 导入文件
     * @return 导入任务
     */
    @PostMapping
    @PreAuthorize("@ss.hasPermi('business:hr:employee:import')")
    public R<SysImexJob> importEmployees(@RequestPart("file") MultipartFile file) {
        return R.success(employeeImportService.importEmployees(file));
    }
}

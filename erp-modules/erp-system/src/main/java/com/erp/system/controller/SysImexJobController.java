package com.erp.system.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.erp.common.core.domain.PageData;
import com.erp.common.core.domain.R;
import com.erp.system.domain.SysImexJob;
import com.erp.system.service.ISysImexJobService;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * 导入导出任务控制器。
 */
@RestController
@RequestMapping("/system/imex/job")
public class SysImexJobController {

    private final ISysImexJobService imexJobService;

    public SysImexJobController(ISysImexJobService imexJobService) {
        this.imexJobService = imexJobService;
    }

    /**
     * 查询导入导出任务分页。
     *
     * @param jobType 任务类型
     * @param status 状态
     * @param moduleCode 模块编码
     * @param pageNum 页码
     * @param pageSize 页长
     * @return 分页结果
     */
    @GetMapping("/list")
    @PreAuthorize("@ss.hasPermi('system:imex:list')")
    public R<PageData<SysImexJob>> list(@RequestParam(value = "jobType", required = false) String jobType,
            @RequestParam(value = "status", required = false) String status,
            @RequestParam(value = "moduleCode", required = false) String moduleCode,
            @RequestParam(value = "pageNum", required = false) Long pageNum,
            @RequestParam(value = "pageSize", required = false) Long pageSize) {
        Page<SysImexJob> page = imexJobService.selectPage(jobType, status, moduleCode, pageNum, pageSize);
        return R.page(page.getRecords(), page.getCurrent(), page.getSize(), page.getTotal());
    }

    /**
     * 查询任务详情。
     *
     * @param jobId 任务ID
     * @return 任务详情
     */
    @GetMapping("/query/{jobId}")
    @PreAuthorize("@ss.hasPermi('system:imex:query')")
    public R<SysImexJob> query(@PathVariable("jobId") Long jobId) {
        return R.success(imexJobService.getDetail(jobId));
    }

    /**
     * 下载导出文件。
     *
     * @param jobId 任务ID
     * @return 文件响应
     * @throws Exception IO 异常
     */
    @GetMapping("/download/{jobId}")
    @PreAuthorize("@ss.hasPermi('system:imex:download')")
    public ResponseEntity<Resource> download(@PathVariable("jobId") Long jobId) throws Exception {
        SysImexJob job = imexJobService.getDetail(jobId);
        Path filePath = Paths.get(job.getFilePath());
        if (!Files.exists(filePath)) {
            throw new IllegalStateException("导出文件不存在");
        }
        Resource resource = new FileSystemResource(filePath);
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + job.getFileName())
                .body(resource);
    }
}

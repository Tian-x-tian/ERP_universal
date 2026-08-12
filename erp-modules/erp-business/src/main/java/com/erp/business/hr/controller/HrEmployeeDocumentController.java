package com.erp.business.hr.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.erp.business.hr.domain.HrEmployeeDocument;
import com.erp.business.hr.domain.vo.HrEmployeeDocumentBody;
import com.erp.business.hr.domain.vo.HrEmployeeDocumentQuery;
import com.erp.business.hr.service.IHrEmployeeDocumentService;
import com.erp.common.core.domain.PageData;
import com.erp.common.core.domain.R;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/**
 * 员工电子档案控制层。
 */
@RestController
@RequestMapping("/business/hr/employee/document")
public class HrEmployeeDocumentController {
    private final IHrEmployeeDocumentService employeeDocumentService;

    public HrEmployeeDocumentController(IHrEmployeeDocumentService employeeDocumentService) {
        this.employeeDocumentService = employeeDocumentService;
    }

    /**
     * 分页查询电子档案。
     *
     * @param query 查询参数
     * @return 分页结果
     */
    @GetMapping("/list")
    @PreAuthorize("@ss.hasPermi('business:hr:document:list')")
    public R<PageData<HrEmployeeDocument>> list(HrEmployeeDocumentQuery query) {
        Page<HrEmployeeDocument> page = employeeDocumentService.selectPage(query);
        return R.page(page.getRecords(), page.getCurrent(), page.getSize(), page.getTotal());
    }

    /**
     * 上传电子档案。
     *
     * @param body 元数据
     * @param file 上传文件
     * @return 档案详情
     */
    @PostMapping("/upload")
    @PreAuthorize("@ss.hasPermi('business:hr:document:add')")
    public R<HrEmployeeDocument> upload(@ModelAttribute HrEmployeeDocumentBody body,
            @RequestPart("file") MultipartFile file) {
        return R.success(employeeDocumentService.uploadDocument(body, file));
    }

    /**
     * 更新电子档案元数据。
     *
     * @param documentId 档案ID
     * @param body 元数据
     * @return 档案详情
     */
    @PutMapping("/{documentId}")
    @PreAuthorize("@ss.hasPermi('business:hr:document:edit')")
    public R<HrEmployeeDocument> update(@PathVariable("documentId") Long documentId,
            @ModelAttribute HrEmployeeDocumentBody body) {
        return R.success(employeeDocumentService.updateDocument(documentId, body));
    }

    /**
     * 生成预览链接。
     *
     * @param documentId 档案ID
     * @return 预览链接
     */
    @GetMapping("/preview/{documentId}")
    @PreAuthorize("@ss.hasPermi('business:hr:document:query')")
    public R<String> preview(@PathVariable("documentId") Long documentId) {
        return R.success(employeeDocumentService.buildPreviewUrl(documentId));
    }

    /**
     * 生成下载链接。
     *
     * @param documentId 档案ID
     * @return 下载链接
     */
    @GetMapping("/download/{documentId}")
    @PreAuthorize("@ss.hasPermi('business:hr:document:query')")
    public R<String> download(@PathVariable("documentId") Long documentId) {
        return R.success(employeeDocumentService.buildDownloadUrl(documentId));
    }

    /**
     * 删除电子档案。
     *
     * @param documentId 档案ID
     * @return 删除结果
     */
    @DeleteMapping("/{documentId}")
    @PreAuthorize("@ss.hasPermi('business:hr:document:remove')")
    public R<Boolean> delete(@PathVariable("documentId") Long documentId) {
        return R.success(employeeDocumentService.deleteDocument(documentId));
    }
}

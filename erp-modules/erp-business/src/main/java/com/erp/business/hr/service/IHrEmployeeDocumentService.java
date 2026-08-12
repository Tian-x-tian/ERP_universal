package com.erp.business.hr.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.erp.business.hr.domain.HrEmployeeDocument;
import com.erp.business.hr.domain.vo.HrEmployeeDocumentBody;
import com.erp.business.hr.domain.vo.HrEmployeeDocumentQuery;
import org.springframework.web.multipart.MultipartFile;

/**
 * 员工电子档案服务接口。
 */
public interface IHrEmployeeDocumentService {

    /**
     * 分页查询电子档案。
     *
     * @param query 查询参数
     * @return 分页结果
     */
    Page<HrEmployeeDocument> selectPage(HrEmployeeDocumentQuery query);

    /**
     * 查询电子档案详情。
     *
     * @param documentId 档案ID
     * @return 档案详情
     */
    HrEmployeeDocument getById(Long documentId);

    /**
     * 上传电子档案并保存元数据。
     *
     * @param body 元数据
     * @param file 上传文件
     * @return 档案详情
     */
    HrEmployeeDocument uploadDocument(HrEmployeeDocumentBody body, MultipartFile file);

    /**
     * 更新电子档案元数据。
     *
     * @param documentId 档案ID
     * @param body 元数据
     * @return 档案详情
     */
    HrEmployeeDocument updateDocument(Long documentId, HrEmployeeDocumentBody body);

    /**
     * 删除电子档案。
     *
     * @param documentId 档案ID
     * @return true 表示成功
     */
    boolean deleteDocument(Long documentId);

    /**
     * 生成预览链接。
     *
     * @param documentId 档案ID
     * @return 预览链接
     */
    String buildPreviewUrl(Long documentId);

    /**
     * 生成下载链接。
     *
     * @param documentId 档案ID
     * @return 下载链接
     */
    String buildDownloadUrl(Long documentId);
}

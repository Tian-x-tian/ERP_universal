package com.erp.business.hr.service;

import org.springframework.web.multipart.MultipartFile;

/**
 * HR 对象存储服务接口。
 */
public interface IHrObjectStorageService {

    /**
     * 上传文件到对象存储。
     *
     * @param objectKey 对象键
     * @param file 上传文件
     * @return 对象键
     */
    String upload(String objectKey, MultipartFile file);

    /**
     * 生成预览链接。
     *
     * @param objectKey 对象键
     * @return 预览链接
     */
    String generatePreviewUrl(String objectKey);

    /**
     * 生成下载链接。
     *
     * @param objectKey 对象键
     * @return 下载链接
     */
    String generateDownloadUrl(String objectKey);
}

package com.erp.business.hr.service.impl;

import com.erp.business.hr.config.HrObjectStorageProperties;
import com.erp.business.hr.service.IHrObjectStorageService;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3Configuration;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;

import java.io.IOException;
import java.net.URI;
import java.time.Duration;

/**
 * S3 兼容对象存储实现。
 */
@Service
public class HrObjectStorageServiceImpl implements IHrObjectStorageService {
    private final HrObjectStorageProperties properties;

    public HrObjectStorageServiceImpl(HrObjectStorageProperties properties) {
        this.properties = properties;
    }

    /**
     * 上传文件到对象存储。
     *
     * @param objectKey 对象键
     * @param file 上传文件
     * @return 对象键
     */
    @Override
    public String upload(String objectKey, MultipartFile file) {
        validateStorageReady();
        if (!StringUtils.hasText(objectKey) || file == null || file.isEmpty()) {
            throw new IllegalArgumentException("上传对象键和文件不能为空");
        }
        try (S3Client s3Client = buildS3Client()) {
            PutObjectRequest request = PutObjectRequest.builder()
                    .bucket(properties.getBucket())
                    .key(objectKey)
                    .contentType(file.getContentType())
                    .build();
            s3Client.putObject(request, RequestBody.fromBytes(file.getBytes()));
            return objectKey;
        } catch (IOException ex) {
            throw new IllegalStateException("上传电子档案失败", ex);
        }
    }

    /**
     * 生成预览链接。
     *
     * @param objectKey 对象键
     * @return 预览链接
     */
    @Override
    public String generatePreviewUrl(String objectKey) {
        return generateSignedUrl(objectKey, properties.getPreviewExpireSeconds());
    }

    /**
     * 生成下载链接。
     *
     * @param objectKey 对象键
     * @return 下载链接
     */
    @Override
    public String generateDownloadUrl(String objectKey) {
        return generateSignedUrl(objectKey, properties.getDownloadExpireSeconds());
    }

    /**
     * 生成签名链接。
     *
     * @param objectKey 对象键
     * @param expireSeconds 失效秒数
     * @return 签名链接
     */
    private String generateSignedUrl(String objectKey, Integer expireSeconds) {
        validateStorageReady();
        if (!StringUtils.hasText(objectKey)) {
            throw new IllegalArgumentException("对象键不能为空");
        }
        try (S3Presigner presigner = buildPresigner()) {
            GetObjectRequest objectRequest = GetObjectRequest.builder()
                    .bucket(properties.getBucket())
                    .key(objectKey)
                    .build();
            GetObjectPresignRequest request = GetObjectPresignRequest.builder()
                    .signatureDuration(Duration.ofSeconds(expireSeconds == null || expireSeconds < 60 ? 900 : expireSeconds))
                    .getObjectRequest(objectRequest)
                    .build();
            return presigner.presignGetObject(request).url().toString();
        }
    }

    /**
     * 创建 S3 客户端。
     *
     * @return S3 客户端
     */
    private S3Client buildS3Client() {
        return S3Client.builder()
                .region(Region.of(properties.getRegion()))
                .endpointOverride(URI.create(properties.getEndpoint()))
                .forcePathStyle(true)
                .credentialsProvider(StaticCredentialsProvider.create(
                        AwsBasicCredentials.create(properties.getAccessKey(), properties.getSecretKey())))
                .serviceConfiguration(S3Configuration.builder().pathStyleAccessEnabled(true).build())
                .build();
    }

    /**
     * 创建 S3 签名客户端。
     *
     * @return S3 签名客户端
     */
    private S3Presigner buildPresigner() {
        return S3Presigner.builder()
                .region(Region.of(properties.getRegion()))
                .endpointOverride(URI.create(properties.getEndpoint()))
                .credentialsProvider(StaticCredentialsProvider.create(
                        AwsBasicCredentials.create(properties.getAccessKey(), properties.getSecretKey())))
                .serviceConfiguration(S3Configuration.builder().pathStyleAccessEnabled(true).build())
                .build();
    }

    /**
     * 校验对象存储配置。
     */
    private void validateStorageReady() {
        if (!properties.isEnabled()
                || !StringUtils.hasText(properties.getEndpoint())
                || !StringUtils.hasText(properties.getBucket())
                || !StringUtils.hasText(properties.getAccessKey())
                || !StringUtils.hasText(properties.getSecretKey())) {
            throw new IllegalStateException("HR 对象存储未配置完成");
        }
    }
}

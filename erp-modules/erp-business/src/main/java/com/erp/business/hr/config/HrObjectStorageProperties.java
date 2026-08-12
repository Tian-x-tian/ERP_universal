package com.erp.business.hr.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * HR 对象存储配置。
 */
@ConfigurationProperties(prefix = "hr.storage.s3")
public class HrObjectStorageProperties {
    private boolean enabled = true;
    private String endpoint;
    private String region = "us-east-1";
    private String accessKey;
    private String secretKey;
    private String bucket;
    private Integer previewExpireSeconds = 900;
    private Integer downloadExpireSeconds = 900;

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getEndpoint() {
        return endpoint;
    }

    public void setEndpoint(String endpoint) {
        this.endpoint = endpoint;
    }

    public String getRegion() {
        return region;
    }

    public void setRegion(String region) {
        this.region = region;
    }

    public String getAccessKey() {
        return accessKey;
    }

    public void setAccessKey(String accessKey) {
        this.accessKey = accessKey;
    }

    public String getSecretKey() {
        return secretKey;
    }

    public void setSecretKey(String secretKey) {
        this.secretKey = secretKey;
    }

    public String getBucket() {
        return bucket;
    }

    public void setBucket(String bucket) {
        this.bucket = bucket;
    }

    public Integer getPreviewExpireSeconds() {
        return previewExpireSeconds;
    }

    public void setPreviewExpireSeconds(Integer previewExpireSeconds) {
        this.previewExpireSeconds = previewExpireSeconds;
    }

    public Integer getDownloadExpireSeconds() {
        return downloadExpireSeconds;
    }

    public void setDownloadExpireSeconds(Integer downloadExpireSeconds) {
        this.downloadExpireSeconds = downloadExpireSeconds;
    }
}

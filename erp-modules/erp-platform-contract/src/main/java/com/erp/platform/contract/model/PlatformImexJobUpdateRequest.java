package com.erp.platform.contract.model;

import java.io.Serializable;
import java.util.Date;

/**
 * 平台导入导出任务更新参数。
 */
public class PlatformImexJobUpdateRequest implements Serializable {
    private static final long serialVersionUID = 1L;

    private String filePath;
    private String status;
    private Integer progress;
    private String message;
    private String updateBy;
    private Date updateTime;

    public String getFilePath() {
        return filePath;
    }

    public void setFilePath(String filePath) {
        this.filePath = filePath;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Integer getProgress() {
        return progress;
    }

    public void setProgress(Integer progress) {
        this.progress = progress;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getUpdateBy() {
        return updateBy;
    }

    public void setUpdateBy(String updateBy) {
        this.updateBy = updateBy;
    }

    public Date getUpdateTime() {
        return updateTime;
    }

    public void setUpdateTime(Date updateTime) {
        this.updateTime = updateTime;
    }
}


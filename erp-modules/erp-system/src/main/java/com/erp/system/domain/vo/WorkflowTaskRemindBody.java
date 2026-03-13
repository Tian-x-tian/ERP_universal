package com.erp.system.domain.vo;


import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * 流程任务催办请求对象。
 */
public class WorkflowTaskRemindBody implements Serializable {
    private static final long serialVersionUID = 1L;

    /** 催办渠道（IN_APP/SMS/WECOM） */
    private List<String> channels = new ArrayList<>();

    /** 催办消息 */
    private String message;


    public List<String> getChannels() {
        return channels;
    }

    public void setChannels(List<String> channels) {
        this.channels = channels;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}

package com.erp.workflow.service;

import com.erp.workflow.domain.platform.SysNotice;

/**
 * 工作流通知桥接服务接口。
 */
public interface ISysNoticeService {

    /**
     * 创建平台通知。
     *
     * @param notice 通知对象
     */
    void createNotice(SysNotice notice);
}

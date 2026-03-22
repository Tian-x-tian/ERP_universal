package com.erp.workflow.service.impl;

import com.erp.common.client.internal.InternalPlatformClient;
import com.erp.platform.contract.model.PlatformNoticeCreateRequest;
import com.erp.workflow.domain.platform.SysNotice;
import com.erp.workflow.service.ISysNoticeService;
import org.springframework.stereotype.Service;

/**
 * 工作流通知桥接服务实现。
 */
@Service
public class SysNoticeServiceImpl implements ISysNoticeService {
    private final InternalPlatformClient internalPlatformClient;

    public SysNoticeServiceImpl(InternalPlatformClient internalPlatformClient) {
        this.internalPlatformClient = internalPlatformClient;
    }

    /**
     * 创建平台通知。
     *
     * @param notice 通知对象
     */
    @Override
    public void createNotice(SysNotice notice) {
        if (notice == null) {
            return;
        }
        PlatformNoticeCreateRequest request = new PlatformNoticeCreateRequest();
        request.setTenantId(notice.getTenantId());
        request.setTitle(notice.getTitle());
        request.setNoticeType(notice.getNoticeType());
        request.setSource(notice.getSource());
        request.setBusinessNo(notice.getBusinessNo());
        request.setContent(notice.getContent());
        request.setReceiverUserId(notice.getReceiverUserId());
        request.setDeliveryChannel(notice.getDeliveryChannel());
        request.setDeliveryStatus(notice.getDeliveryStatus());
        request.setDeliveryTime(notice.getDeliveryTime());
        request.setStatus(notice.getStatus());
        request.setCreateTime(notice.getCreateTime());
        internalPlatformClient.createNotice(request);
    }
}

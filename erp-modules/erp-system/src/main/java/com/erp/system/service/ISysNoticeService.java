package com.erp.system.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.erp.system.domain.SysNotice;

import java.util.List;

/**
 * 系统消息通知服务接口
 */
public interface ISysNoticeService extends IService<SysNotice> {

    /**
     * 查询当前用户消息列表。
     *
     * @param userId     当前用户ID
     * @param noticeType 消息类型
     * @return 消息列表
     */
    List<SysNotice> selectByCurrentUser(Long userId, String noticeType);

    /**
     * 标记单条消息已读。
     *
     * @param noticeId 消息ID
     * @param userId   当前用户ID
     * @return 是否更新成功
     */
    boolean markRead(Long noticeId, Long userId);

    /**
     * 批量标记当前用户消息已读。
     *
     * @param userId 当前用户ID
     * @return 更新记录条数
     */
    int markAllRead(Long userId);
}

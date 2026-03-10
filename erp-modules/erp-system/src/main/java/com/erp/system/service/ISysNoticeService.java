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
    List<SysNotice> selectByCurrentUser(Long userId, String noticeType, String status, String deliveryChannel, String deliveryStatus);

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

    /**
     * 按租户查询消息中心管理列表。
     *
     * @param tenantId       租户编号
     * @param title          标题关键字
     * @param noticeType     消息类型
     * @param receiverUserId 接收人用户ID
     * @param status         状态（0未读 1已读）
     * @return 消息列表
     */
    List<SysNotice> selectForManage(String tenantId,
                                    String title,
                                    String noticeType,
                                    Long receiverUserId,
                                    String status,
                                    String deliveryChannel,
                                    String deliveryStatus,
                                    String source,
                                    String businessNo);

    /**
     * 新增消息通知。
     *
     * @param notice 消息对象
     * @return 新增结果
     */
    boolean createNotice(SysNotice notice);

    /**
     * 修改消息通知。
     *
     * @param notice 消息对象
     * @return 修改结果
     */
    boolean updateNotice(SysNotice notice);
}

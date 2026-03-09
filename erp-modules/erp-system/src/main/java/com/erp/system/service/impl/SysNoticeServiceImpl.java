package com.erp.system.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.erp.system.domain.SysNotice;
import com.erp.system.mapper.SysNoticeMapper;
import com.erp.system.service.ISysNoticeService;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.Date;
import java.util.List;

/**
 * 系统消息通知服务实现
 */
@Service
public class SysNoticeServiceImpl extends ServiceImpl<SysNoticeMapper, SysNotice> implements ISysNoticeService {

    /**
     * 查询当前用户消息列表。
     *
     * @param userId     当前用户ID
     * @param noticeType 消息类型
     * @return 消息列表
     */
    @Override
    public List<SysNotice> selectByCurrentUser(Long userId, String noticeType) {
        LambdaQueryWrapper<SysNotice> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(SysNotice::getReceiverUserId, userId);
        if (StringUtils.hasText(noticeType)) {
            queryWrapper.eq(SysNotice::getNoticeType, noticeType.trim());
        }
        queryWrapper.orderByDesc(SysNotice::getCreateTime);
        return list(queryWrapper);
    }

    /**
     * 标记单条消息已读。
     *
     * @param noticeId 消息ID
     * @param userId   当前用户ID
     * @return 是否更新成功
     */
    @Override
    public boolean markRead(Long noticeId, Long userId) {
        if (noticeId == null || userId == null) {
            return false;
        }
        LambdaUpdateWrapper<SysNotice> updateWrapper = new LambdaUpdateWrapper<>();
        updateWrapper.eq(SysNotice::getNoticeId, noticeId)
                .eq(SysNotice::getReceiverUserId, userId)
                .set(SysNotice::getStatus, "1")
                .set(SysNotice::getReadTime, new Date());
        return update(updateWrapper);
    }

    /**
     * 批量标记当前用户消息已读。
     *
     * @param userId 当前用户ID
     * @return 更新记录条数
     */
    @Override
    public int markAllRead(Long userId) {
        if (userId == null) {
            return 0;
        }
        LambdaUpdateWrapper<SysNotice> updateWrapper = new LambdaUpdateWrapper<>();
        updateWrapper.eq(SysNotice::getReceiverUserId, userId)
                .eq(SysNotice::getStatus, "0")
                .set(SysNotice::getStatus, "1")
                .set(SysNotice::getReadTime, new Date());
        return baseMapper.update(null, updateWrapper);
    }

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
    @Override
    public List<SysNotice> selectForManage(String tenantId, String title, String noticeType, Long receiverUserId, String status) {
        LambdaQueryWrapper<SysNotice> queryWrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(tenantId)) {
            queryWrapper.eq(SysNotice::getTenantId, tenantId.trim());
        }
        if (StringUtils.hasText(title)) {
            queryWrapper.like(SysNotice::getTitle, title.trim());
        }
        if (StringUtils.hasText(noticeType)) {
            queryWrapper.eq(SysNotice::getNoticeType, noticeType.trim());
        }
        if (receiverUserId != null) {
            queryWrapper.eq(SysNotice::getReceiverUserId, receiverUserId);
        }
        if (StringUtils.hasText(status)) {
            queryWrapper.eq(SysNotice::getStatus, status.trim());
        }
        queryWrapper.orderByDesc(SysNotice::getCreateTime);
        return list(queryWrapper);
    }

    /**
     * 新增消息通知。
     *
     * @param notice 消息对象
     * @return 新增结果
     */
    @Override
    public boolean createNotice(SysNotice notice) {
        if (notice == null || !StringUtils.hasText(notice.getTitle()) || notice.getReceiverUserId() == null) {
            return false;
        }
        notice.setTitle(notice.getTitle().trim());
        notice.setNoticeType(StringUtils.hasText(notice.getNoticeType()) ? notice.getNoticeType().trim() : "系统公告");
        notice.setStatus(StringUtils.hasText(notice.getStatus()) ? notice.getStatus() : "0");
        if ("1".equals(notice.getStatus())) {
            notice.setReadTime(new Date());
        } else {
            notice.setReadTime(null);
        }
        notice.setCreateTime(new Date());
        return save(notice);
    }

    /**
     * 修改消息通知。
     *
     * @param notice 消息对象
     * @return 修改结果
     */
    @Override
    public boolean updateNotice(SysNotice notice) {
        if (notice == null || notice.getNoticeId() == null) {
            return false;
        }
        SysNotice existedNotice = getById(notice.getNoticeId());
        if (existedNotice == null) {
            return false;
        }
        notice.setTenantId(existedNotice.getTenantId());
        notice.setCreateTime(existedNotice.getCreateTime());
        if (StringUtils.hasText(notice.getTitle())) {
            notice.setTitle(notice.getTitle().trim());
        }
        if (StringUtils.hasText(notice.getNoticeType())) {
            notice.setNoticeType(notice.getNoticeType().trim());
        }
        if (!StringUtils.hasText(notice.getStatus())) {
            notice.setStatus(existedNotice.getStatus());
            notice.setReadTime(existedNotice.getReadTime());
        } else if ("1".equals(notice.getStatus())) {
            if (notice.getReadTime() == null) {
                notice.setReadTime(new Date());
            }
        } else {
            notice.setReadTime(null);
        }
        return updateById(notice);
    }
}

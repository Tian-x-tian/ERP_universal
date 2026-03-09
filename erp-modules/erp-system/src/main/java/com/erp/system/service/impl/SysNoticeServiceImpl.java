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
}

package com.erp.system.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.erp.system.domain.SysPost;
import com.erp.system.mapper.SysPostMapper;
import com.erp.system.service.ISysPostService;
import com.erp.system.support.StatusFieldSupport;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.Date;

/**
 * 岗位服务实现
 */
@Service
public class SysPostServiceImpl extends ServiceImpl<SysPostMapper, SysPost> implements ISysPostService {

    /**
     * 新增岗位时规范状态字段和基础审计字段。
     *
     * @param entity 岗位实体
     * @return 新增结果
     */
    @Override
    public boolean save(SysPost entity) {
        normalizePost(entity, true, null);
        return super.save(entity);
    }

    /**
     * 修改岗位时规范状态字段和更新时间。
     * 若未显式传入状态字段，则保持原状态值。
     *
     * @param entity 岗位实体
     * @return 修改结果
     */
    @Override
    public boolean updateById(SysPost entity) {
        String currentStatus = null;
        if (entity != null && entity.getPostId() != null) {
            SysPost existedPost = getById(entity.getPostId());
            currentStatus = existedPost == null ? null : existedPost.getStatus();
        }
        normalizePost(entity, false, currentStatus);
        return super.updateById(entity);
    }

    /**
     * 规范岗位核心字段，避免状态为空导致前端无法判断启停。
     *
     * @param post          岗位对象
     * @param isCreate      是否为新增操作
     * @param currentStatus 当前已落库状态值（更新场景使用）
     */
    private void normalizePost(SysPost post, boolean isCreate, String currentStatus) {
        if (post == null) {
            return;
        }
        if (isCreate) {
            post.setStatus(StatusFieldSupport.normalizeBinaryStatus(post.getStatus()));
        } else {
            post.setStatus(StatusFieldSupport.normalizeBinaryStatusForUpdate(post.getStatus(), currentStatus));
        }
        if (StringUtils.hasText(post.getPostCode())) {
            post.setPostCode(post.getPostCode().trim());
        }
        if (StringUtils.hasText(post.getPostName())) {
            post.setPostName(post.getPostName().trim());
        }
        if (isCreate) {
            post.setCreateTime(new Date());
        } else {
            post.setUpdateTime(new Date());
        }
    }
}

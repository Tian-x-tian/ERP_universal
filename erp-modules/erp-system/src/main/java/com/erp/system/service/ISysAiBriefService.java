package com.erp.system.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.erp.platform.contract.model.PlatformAiBriefSaveRequest;
import com.erp.platform.contract.model.PlatformAiBriefView;
import com.erp.system.domain.SysAiBrief;

/**
 * AI 每日简报服务。
 */
public interface ISysAiBriefService extends IService<SysAiBrief> {

    /**
     * 查询当日简报。
     *
     * @param tenantId  租户编号
     * @param userId    用户ID
     * @param briefType 简报类型
     * @return 简报视图；不存在时返回 null
     */
    PlatformAiBriefView getToday(String tenantId, Long userId, String briefType);

    /**
     * 抢占当日简报的生成权。
     *
     * @param tenantId     租户编号
     * @param userId       用户ID
     * @param briefType    简报类型
     * @param staleMinutes 生成中状态的陈旧判定分钟数
     * @return true 表示抢占成功，调用方应负责生成
     */
    boolean claimToday(String tenantId, Long userId, String briefType, int staleMinutes);

    /**
     * 回写生成结果。
     *
     * @param tenantId 租户编号
     * @param userId   用户ID
     * @param request  回写请求
     * @return 回写后的简报视图
     */
    PlatformAiBriefView saveResult(String tenantId, Long userId, PlatformAiBriefSaveRequest request);
}
